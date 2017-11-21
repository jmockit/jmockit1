/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.injection.full;

import java.lang.annotation.*;
import java.lang.reflect.*;
import java.util.logging.*;
import javax.annotation.*;
import javax.enterprise.context.*;
import javax.inject.*;
import javax.sql.*;
import static java.lang.reflect.Modifier.*;

import mockit.external.asm.*;
import mockit.internal.injection.*;
import static mockit.internal.injection.InjectionPoint.*;
import static mockit.internal.reflection.ConstructorReflection.*;
import static mockit.internal.util.Utilities.*;

/**
 * Responsible for recursive injection of dependencies into a <tt>@Tested(fullyInitialized = true)</tt> object.
 */
public final class FullInjection
{
   private static final int INVALID_TYPES = Access.ABSTRACT + Access.ANNOTATION + Access.ENUM;

   @Nonnull private final InjectionState injectionState;
   @Nonnull private final Class<?> testedClass;
   @Nonnull private final String testedName;
   @Nullable private final ServletDependencies servletDependencies;
   @Nullable private final JPADependencies jpaDependencies;
   @Nullable private Class<?> dependencyClass;
   @Nullable private InjectionProvider injectionProvider;

   public FullInjection(
      @Nonnull InjectionState injectionState, @Nonnull Class<?> testedClass, @Nonnull String testedName)
   {
      this.injectionState = injectionState;
      this.testedClass = testedClass;
      this.testedName = testedName;
      servletDependencies = SERVLET_CLASS == null ? null : new ServletDependencies(injectionState);
      jpaDependencies = PERSISTENCE_UNIT_CLASS == null ? null : new JPADependencies(injectionState);
   }

   @Nullable
   public Object createOrReuseInstance(
      @Nonnull TestedClass testedClass, @Nonnull Injector injector, @Nullable InjectionProvider injectionProvider,
      @Nullable String qualifiedName)
   {
      setInjectionProvider(injectionProvider);

      InjectionPoint injectionPoint = getInjectionPoint(testedClass, injectionProvider, qualifiedName);
      Object dependency = injectionState.getInstantiatedDependency(testedClass, injectionPoint);

      if (dependency != null) {
         return dependency;
      }

      Class<?> typeToInject = dependencyClass;

      if (typeToInject == Logger.class) {
         return createLogger(testedClass);
      }

      if (typeToInject == null || !isInstantiableType(typeToInject)) {
         return null;
      }

      Object testedInstance = createInstance(testedClass, injector, injectionProvider, injectionPoint);
      return testedInstance;
   }

   private void setInjectionProvider(@Nullable InjectionProvider injectionProvider)
   {
      if (injectionProvider != null) {
         injectionProvider.parent = this.injectionProvider;
      }

      this.injectionProvider = injectionProvider;
   }

   @Nonnull
   private InjectionPoint getInjectionPoint(
      @Nonnull TestedClass testedClass, @Nullable InjectionProvider injectionProvider, @Nullable String qualifiedName)
   {
      if (injectionProvider == null) {
         dependencyClass = testedClass.targetClass;
         return new InjectionPoint(dependencyClass, qualifiedName, true);
      }

      Type dependencyType = injectionProvider.getDeclaredType();

      if (dependencyType instanceof TypeVariable<?>) {
         dependencyType = testedClass.reflection.resolveTypeVariable((TypeVariable<?>) dependencyType);
         dependencyClass = getClassType(dependencyType);
      }
      else {
         dependencyClass = injectionProvider.getClassOfDeclaredType();
      }

      if (qualifiedName != null && !qualifiedName.isEmpty()) {
         return new InjectionPoint(dependencyClass, qualifiedName, true);
      }

      if (jpaDependencies != null && JPADependencies.isApplicable(dependencyClass)) {
         for (Annotation annotation : injectionProvider.getAnnotations()) {
            InjectionPoint injectionPoint = jpaDependencies.getInjectionPointIfAvailable(annotation);

            if (injectionPoint != null) {
               return injectionPoint;
            }
         }
      }

      return new InjectionPoint(dependencyType, injectionProvider.getName(), false);
   }

   @Nonnull
   private Object createLogger(@Nonnull TestedClass testedClass)
   {
      TestedClass testedClassWithLogger = testedClass.parent;
      assert testedClassWithLogger != null;
      return Logger.getLogger(testedClassWithLogger.nameOfTestedClass);
   }

   private static boolean isInstantiableType(@Nonnull Class<?> type)
   {
      if (type.isPrimitive() || type.isArray() || type.isAnnotation()) {
         return false;
      }

      if (!type.isInterface()) {
         int typeModifiers = type.getModifiers();

         if ((typeModifiers & INVALID_TYPES) != 0 || !isStatic(typeModifiers) && type.isMemberClass()) {
            return false;
         }

         if (type.getClassLoader() == null) {
            return false;
         }
      }

      return true;
   }

   @Nullable
   private Object createInstance(
      @Nonnull TestedClass testedClass, @Nonnull Injector injector, @Nullable InjectionProvider injectionProvider,
      @Nonnull InjectionPoint injectionPoint)
   {
      @SuppressWarnings("ConstantConditions") @Nonnull Class<?> typeToInject = dependencyClass;
      Object dependency = null;

      if (typeToInject.isInterface()) {
         dependency = createInstanceOfSupportedInterfaceIfApplicable(
            testedClass, typeToInject, injectionPoint, injectionProvider);

         if (dependency == null && typeToInject.getClassLoader() != null) {
            Class<?> resolvedType = injectionState.resolveInterface(typeToInject);

            if (resolvedType != null && !resolvedType.isInterface()) {
               testedClass = new TestedClass(resolvedType, resolvedType);
               typeToInject = resolvedType;
            }
         }
      }

      if (dependency == null) {
         dependency =
            createAndRegisterNewInstance(typeToInject, testedClass, injector, injectionPoint, injectionProvider);
      }

      return dependency;
   }

   @Nullable
   private Object createInstanceOfSupportedInterfaceIfApplicable(
      @Nonnull TestedClass testedClass, @Nonnull Class<?> typeToInject,
      @Nonnull InjectionPoint injectionPoint, @Nullable InjectionProvider injectionProvider)
   {
      Object dependency = null;

      if (CommonDataSource.class.isAssignableFrom(typeToInject)) {
         dependency = createAndRegisterDataSource(testedClass, injectionPoint);
      }
      else if (INJECT_CLASS != null && typeToInject == Provider.class) {
         assert injectionProvider != null;
         dependency = createProviderInstance(injectionProvider);
      }
      else if (CONVERSATION_CLASS != null && typeToInject == Conversation.class) {
         dependency = createAndRegisterConversationInstance();
      }
      else if (servletDependencies != null && ServletDependencies.isApplicable(typeToInject)) {
         dependency = servletDependencies.createAndRegisterDependency(typeToInject);
      }
      else if (jpaDependencies != null && JPADependencies.isApplicable(typeToInject)) {
         dependency = jpaDependencies.createAndRegisterDependency(typeToInject, injectionPoint);
      }

      return dependency;
   }

   @Nullable
   private Object createAndRegisterDataSource(@Nonnull TestedClass testedClass, @Nonnull InjectionPoint injectionPoint)
   {
      TestDataSource dsCreation = new TestDataSource(injectionPoint);
      CommonDataSource dataSource = dsCreation.createIfDataSourceDefinitionAvailable(testedClass);

      if (dataSource != null) {
         injectionState.saveInstantiatedDependency(injectionPoint, dataSource);
      }

      return dataSource;
   }

   @Nonnull
   private Object createProviderInstance(@Nonnull InjectionProvider injectionProvider)
   {
      ParameterizedType genericType = (ParameterizedType) injectionProvider.getDeclaredType();
      final Class<?> providedClass = (Class<?>) genericType.getActualTypeArguments()[0];

      if (providedClass.isAnnotationPresent(Singleton.class)) {
         return new Provider<Object>() {
            private Object dependency;

            @Override
            public synchronized Object get()
            {
               if (dependency == null) {
                  dependency = createNewInstance(providedClass);
               }

               return dependency;
            }
         };
      }

      return new Provider<Object>() {
         @Override
         public Object get()
         {
            Object dependency = createNewInstance(providedClass);
            return dependency;
         }
      };
   }

   @Nullable
   private Object createNewInstance(@Nonnull Class<?> dependencyClass)
   {
      if (dependencyClass.isInterface()) {
         return null;
      }

      if (dependencyClass.getClassLoader() == null) {
         return newInstanceUsingDefaultConstructorIfAvailable(dependencyClass);
      }

      return new TestedObjectCreation(injectionState, this, dependencyClass).create();
   }

   @Nonnull
   private Object createAndRegisterConversationInstance()
   {
      Conversation conversation = new TestConversation();

      InjectionPoint injectionPoint = new InjectionPoint(Conversation.class);
      injectionState.saveInstantiatedDependency(injectionPoint, conversation);
      return conversation;
   }

   @Nullable
   private Object createAndRegisterNewInstance(
      @Nonnull Class<?> typeToInstantiate, @Nonnull TestedClass testedClass, @Nonnull Injector injector,
      @Nonnull InjectionPoint injectionPoint, @Nullable InjectionProvider injectionProvider)
   {
      Object dependency = createNewInstance(typeToInstantiate);

      if (dependency != null) {
         if (injectionPoint.name == null) {
            assert injectionProvider != null;
            injectionPoint = new InjectionPoint(injectionPoint.type, injectionProvider.getName());
         }

         registerNewInstance(testedClass, injector, injectionPoint, dependency);
      }

      return dependency;
   }

   private void registerNewInstance(
      @Nonnull TestedClass testedClass, @Nonnull Injector injector, @Nonnull InjectionPoint injectionPoint,
      @Nonnull Object dependency)
   {
      injectionState.saveInstantiatedDependency(injectionPoint, dependency);

      Class<?> instantiatedClass = dependency.getClass();

      if (testedClass.isClassFromSameModuleOrSystemAsTestedClass(instantiatedClass)) {
         injector.fillOutDependenciesRecursively(dependency, testedClass);
         injectionState.lifecycleMethods.findLifecycleMethods(instantiatedClass);
         injectionState.lifecycleMethods.executeInitializationMethodsIfAny(instantiatedClass, dependency);
      }
   }

   @Override
   public String toString()
   {
      String description = "@Tested object \"" + testedClass.getSimpleName() + ' ' + testedName + '"';

      if (injectionProvider != null) {
         InjectionProvider parentInjectionProvider = injectionProvider.parent;

         if (parentInjectionProvider != null) {
            description = parentInjectionProvider + "\r\n  of " + description;
         }
      }

      return description;
   }
}
