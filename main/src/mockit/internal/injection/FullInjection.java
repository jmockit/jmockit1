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
   @Nullable private final ServletDependencies servletDependencies;
   @Nullable private final JPADependencies jpaDependencies;
   private Class<?> dependencyClass;

   FullInjection(@Nonnull InjectionState injectionState)
   {
      this.injectionState = injectionState;
      servletDependencies = SERVLET_CLASS == null ? null : new ServletDependencies(injectionState);
      jpaDependencies = PERSISTENCE_UNIT_CLASS == null ? null : new JPADependencies(injectionState);
   }

   @Nullable
   Object newInstance(
      @Nonnull TestedClass testedClass, @Nonnull Injector injector, @Nonnull InjectionPointProvider injectionProvider,
      @Nullable String qualifiedName)
   {
      InjectionPoint injectionPoint = getInjectionPoint(testedClass.reflection, injectionProvider, qualifiedName);
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

      if (INJECT_CLASS != null && typeToInject == Provider.class) {
         dependency = createProviderInstance(injectionProvider, injectionPoint);
      }
      else if (servletDependencies != null && ServletDependencies.isApplicable(typeToInject)) {
         dependency = servletDependencies.createAndRegisterDependency(typeToInject);
      }
      else if (CONVERSATION_CLASS != null && typeToInject == Conversation.class) {
         dependency = createAndRegisterConversationInstance();
      }
      else {
         dependency = createAndRegisterNewInstance(testedClass, injector, injectionProvider, injectionPoint);
      }

      return dependency;
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
      }

      return true;
   }

   @Nonnull
   private InjectionPoint getInjectionPoint(
      @Nonnull GenericTypeReflection reflection, @Nonnull InjectionPointProvider injectionProvider,
      @Nullable String qualifiedName)
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

   @Nonnull
   private Object createProviderInstance(
      @Nonnull InjectionPointProvider injectionProvider, @Nonnull final InjectionPoint dependencyKey)
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
                  dependency = createNewInstance(providedClass, dependencyKey);
               }

               return dependency;
            }
         };
      }

      return new Provider<Object>() {
         @Override
         public Object get()
         {
            Object dependency = createNewInstance(providedClass, dependencyKey);
            return dependency;
         }
      };
   }

   @Nullable
   private Object createNewInstance(@Nonnull Class<?> dependencyClass, @Nonnull InjectionPoint dependencyKey)
   {
      if (dependencyClass.isInterface()) {
         if (jpaDependencies != null) {
            Object newInstance = jpaDependencies.newInstanceIfApplicable(dependencyClass, dependencyKey);

            if (newInstance != null) {
               return newInstance;
            }
         }

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
      Conversation conversation = new Conversation() {
         private boolean currentlyTransient = true;
         private int counter;
         private String currentId;
         private long currentTimeout;

         @Override
         public void begin()
         {
            counter++;
            currentId = String.valueOf(counter);
            currentlyTransient = false;
         }

         @Override
         public void begin(String id)
         {
            counter++;
            currentId = id;
            currentlyTransient = false;
         }

         @Override
         public void end()
         {
            currentlyTransient = true;
            currentId = null;
         }

         @Override public String getId() { return currentId; }
         @Override public long getTimeout() { return currentTimeout; }
         @Override public void setTimeout(long milliseconds) { currentTimeout = milliseconds; }
         @Override public boolean isTransient() { return currentlyTransient; }
      };

      InjectionPoint injectionPoint = new InjectionPoint(Conversation.class);
      injectionState.saveInstantiatedDependency(injectionPoint, conversation);
      return conversation;
   }

   @Nullable
   private Object createAndRegisterNewInstance(
      @Nonnull TestedClass testedClass, @Nonnull Injector injector, @Nonnull InjectionPointProvider injectionProvider,
      @Nonnull InjectionPoint dependencyKey)
   {
      Object dependency = createNewInstance(dependencyClass, dependencyKey);

      if (dependency != null) {
         if (dependencyKey.name == null) {
            dependencyKey = new InjectionPoint(dependencyKey.type, injectionProvider.getName());
         }

         registerNewInstance(testedClass, injector, dependencyKey, dependency);
      }

      return dependency;
   }

   private void registerNewInstance(
      @Nonnull TestedClass testedClass, @Nonnull Injector injector,
      @Nonnull InjectionPoint dependencyKey, @Nonnull Object dependency)
   {
      Class<?> instantiatedClass = dependency.getClass();

      if (testedClass.isClassFromSameModuleOrSystemAsTestedClass(instantiatedClass)) {
         injector.fillOutDependenciesRecursively(dependency);
         injectionState.lifecycleMethods.findLifecycleMethods(instantiatedClass);
         injectionState.lifecycleMethods.executeInitializationMethodsIfAny(instantiatedClass, dependency);
      }

      injectionState.saveInstantiatedDependency(dependencyKey, dependency);
   }
}
