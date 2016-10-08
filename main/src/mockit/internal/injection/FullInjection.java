/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.injection;

import java.lang.annotation.*;
import java.lang.reflect.*;
import java.util.logging.*;
import javax.annotation.*;
import javax.enterprise.context.*;
import javax.inject.*;
import javax.sql.*;
import static java.lang.reflect.Modifier.*;

import mockit.internal.util.*;
import static mockit.external.asm.Opcodes.*;
import static mockit.internal.injection.InjectionPoint.*;
import static mockit.internal.util.ConstructorReflection.*;
import static mockit.internal.util.Utilities.*;

/**
 * Responsible for recursive injection of dependencies as requested by a {@code @Tested(fullyInitialized = true)} field.
 */
final class FullInjection
{
   private static final int INVALID_TYPES = ACC_ABSTRACT + ACC_ANNOTATION + ACC_ENUM;

   @Nonnull private final InjectionState injectionState;
   @Nonnull private final Field testedField;
   @Nullable private final ServletDependencies servletDependencies;
   @Nullable private final JPADependencies jpaDependencies;
   @Nonnull private Class<?> dependencyClass;
   @Nonnull private InjectionPointProvider injectionProvider;

   FullInjection(@Nonnull InjectionState injectionState, @Nonnull Field testedField)
   {
      this.injectionState = injectionState;
      this.testedField = testedField;
      servletDependencies = SERVLET_CLASS == null ? null : new ServletDependencies(injectionState);
      jpaDependencies = PERSISTENCE_UNIT_CLASS == null ? null : new JPADependencies(injectionState);
   }

   @Nullable
   Object reuseInstance(
      @Nonnull TestedClass testedClass, @Nonnull InjectionPointProvider injectionProvider,
      @Nullable String qualifiedName)
   {
      this.injectionProvider = injectionProvider;
      InjectionPoint injectionPoint = getInjectionPoint(testedClass.reflection, qualifiedName);
      Object dependency = injectionState.getInstantiatedDependency(testedClass, injectionProvider, injectionPoint);
      return dependency;
   }

   @Nonnull
   private InjectionPoint getInjectionPoint(@Nonnull GenericTypeReflection reflection, @Nullable String qualifiedName)
   {
      Type dependencyType = injectionProvider.getDeclaredType();

      if (dependencyType instanceof TypeVariable<?>) {
         dependencyType = reflection.resolveTypeVariable((TypeVariable<?>) dependencyType);
         dependencyClass = getClassType(dependencyType);
      }
      else {
         dependencyClass = injectionProvider.getClassOfDeclaredType();
      }

      if (qualifiedName != null && !qualifiedName.isEmpty()) {
         return new InjectionPoint(dependencyClass, qualifiedName);
      }

      if (jpaDependencies != null && JPADependencies.isApplicable(dependencyClass)) {
         for (Annotation annotation : injectionProvider.getAnnotations()) {
            String id = jpaDependencies.getDependencyIdIfAvailable(annotation);

            if (id != null) {
               return new InjectionPoint(dependencyClass, id);
            }
         }
      }

      return new InjectionPoint(dependencyType);
   }

   @Nullable
   Object createOrReuseInstance(
      @Nonnull Injector injector, @Nonnull InjectionPointProvider injectionProvider, @Nullable String qualifiedName)
   {
      TestedClass testedClass = injector.testedClass;
      setInjectionProvider(injectionProvider);
      InjectionPoint injectionPoint = getInjectionPoint(testedClass.reflection, qualifiedName);
      Object dependency = injectionState.getInstantiatedDependency(testedClass, injectionProvider, injectionPoint);

      if (dependency != null) {
         return dependency;
      }

      Class<?> typeToInject = dependencyClass;

      if (typeToInject == Logger.class) {
         return Logger.getLogger(testedClass.nameOfTestedClass);
      }

      if (!isInstantiableType(typeToInject)) {
         return null;
      }

      dependency = typeToInject.isInterface() ?
         createInstanceOfSupportedInterfaceIfApplicable(testedClass, injectionPoint) :
         createAndRegisterNewInstance(injector, injectionPoint);

      return dependency;
   }

   private void setInjectionProvider(@Nonnull InjectionPointProvider injectionProvider)
   {
      injectionProvider.parent = this.injectionProvider;
      this.injectionProvider = injectionProvider;
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
   private Object createInstanceOfSupportedInterfaceIfApplicable(
      @Nonnull TestedClass testedClass, @Nonnull InjectionPoint injectionPoint)
   {
      Class<?> typeToInject = dependencyClass;
      Object dependency = null;

      if (CommonDataSource.class.isAssignableFrom(typeToInject)) {
         dependency = createAndRegisterDataSource(testedClass, injectionPoint);
      }
      else if (INJECT_CLASS != null && typeToInject == Provider.class) {
         dependency = createProviderInstance();
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
   private Object createProviderInstance()
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
   private Object createAndRegisterNewInstance(@Nonnull Injector injector, @Nonnull InjectionPoint injectionPoint)
   {
      Object dependency = createNewInstance(dependencyClass);

      if (dependency != null) {
         if (injectionPoint.name == null) {
            injectionPoint = new InjectionPoint(injectionPoint.type, injectionProvider.getName());
         }

         registerNewInstance(injector, injectionPoint, dependency);
      }

      return dependency;
   }

   private void registerNewInstance(
      @Nonnull Injector injector, @Nonnull InjectionPoint injectionPoint, @Nonnull Object dependency)
   {
      Class<?> instantiatedClass = dependency.getClass();

      if (injector.testedClass.isClassFromSameModuleOrSystemAsTestedClass(instantiatedClass)) {
         injector.fillOutDependenciesRecursively(dependency);
         injectionState.lifecycleMethods.findLifecycleMethods(instantiatedClass);
         injectionState.lifecycleMethods.executeInitializationMethodsIfAny(instantiatedClass, dependency);
      }

      injectionState.saveInstantiatedDependency(injectionPoint, dependency);
   }

   @Override
   public String toString()
   {
      String description =
         "@Tested field \"" + testedField.getType().getSimpleName() + ' ' + testedField.getName() + '"';
      InjectionPointProvider parentInjectionProvider = injectionProvider.parent;

      if (parentInjectionProvider != null) {
         description = parentInjectionProvider + "\r\n  of " + description;
      }

      return description;
   }
}
