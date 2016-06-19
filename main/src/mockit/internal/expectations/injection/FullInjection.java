/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.expectations.injection;

import java.lang.annotation.*;
import java.lang.reflect.*;
import java.util.logging.*;
import javax.annotation.*;
import javax.enterprise.context.*;
import javax.inject.*;
import static java.lang.reflect.Modifier.*;

import mockit.internal.startup.*;
import mockit.internal.util.*;
import static mockit.external.asm.Opcodes.*;
import static mockit.internal.expectations.injection.InjectionPoint.*;
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
      InjectionPoint dependencyKey = getDependencyKey(injectionProvider, qualifiedName);
      Object dependency = injectionState.getInstantiatedDependency(testedClass, injectionProvider, dependencyKey);

      if (dependency != null) {
         return dependency;
      }

      Class<?> typeToInject = injectionProvider.getClassOfDeclaredType();

      if (typeToInject == Logger.class) {
         return Logger.getLogger(testedClass.nameOfTestedClass);
      }

      if (!isInstantiableType(typeToInject)) {
         return null;
      }

      if (INJECT_CLASS != null && typeToInject == Provider.class) {
         dependency = createProviderInstance(injectionProvider, dependencyKey);
      }
      else if (servletDependencies != null && ServletDependencies.isApplicable(typeToInject)) {
         dependency = servletDependencies.createAndRegisterDependency(typeToInject);
      }
      else if (CONVERSATION_CLASS != null && typeToInject == Conversation.class) {
         dependency = createAndRegisterConversationInstance();
      }
      else {
         dependency = createAndRegisterNewInstance(testedClass, injector, injectionProvider, dependencyKey);
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
   private InjectionPoint getDependencyKey(@Nonnull InjectionPointProvider provider, @Nullable String qualifiedName)
   {
      Class<?> dependencyClass = provider.getClassOfDeclaredType();

      if (qualifiedName != null && !qualifiedName.isEmpty()) {
         return new InjectionPoint(dependencyClass, qualifiedName);
      }

      if (jpaDependencies != null && JPADependencies.isApplicable(dependencyClass)) {
         for (Annotation annotation : provider.getAnnotations()) {
            String id = jpaDependencies.getDependencyIdIfAvailable(annotation);

            if (id != null) {
               return new InjectionPoint(dependencyClass, id);
            }
         }
      }

      return new InjectionPoint(injectionState.typeOfInjectionPoint);
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
      Class<?> implementationClass;

      if (dependencyClass.isInterface()) {
         if (jpaDependencies != null) {
            Object newInstance = jpaDependencies.newInstanceIfApplicable(dependencyClass, dependencyKey);

            if (newInstance != null) {
               return newInstance;
            }
         }

         implementationClass = findImplementationClassInClasspathIfUnique(dependencyClass);
      }
      else {
         implementationClass = dependencyClass;
      }

      if (implementationClass != null) {
         if (implementationClass.getClassLoader() == null) {
            return newInstanceUsingDefaultConstructorIfAvailable(implementationClass);
         }

         return new TestedObjectCreation(injectionState, this, implementationClass).create();
      }

      return null;
   }

   @Nullable
   private static Class<?> findImplementationClassInClasspathIfUnique(@Nonnull Class<?> dependencyClass)
   {
      ClassLoader dependencyLoader = dependencyClass.getClassLoader();
      Class<?> implementationClass = null;

      if (dependencyLoader != null) {
         Class<?>[] loadedClasses = Startup.instrumentation().getInitiatedClasses(dependencyLoader);

         for (Class<?> loadedClass : loadedClasses) {
            if (loadedClass != dependencyClass && dependencyClass.isAssignableFrom(loadedClass)) {
               if (implementationClass != null) {
                  return null;
               }

               implementationClass = loadedClass;
            }
         }
      }

      return implementationClass;
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
      Class<?> classToInstantiate = getClassToInstantiate(testedClass.targetClass, injectionProvider);
      Object dependency = createNewInstance(classToInstantiate, dependencyKey);

      if (dependency != null) {
         registerNewInstance(testedClass, injector, dependencyKey, dependency);
      }

      return dependency;
   }

   @Nonnull
   private Class<?> getClassToInstantiate(
      @Nonnull Class<?> targetClass, @Nonnull InjectionPointProvider injectionProvider)
   {
      Type declaredType = injectionProvider.getDeclaredType();

      if (declaredType instanceof TypeVariable<?>) {
         GenericTypeReflection typeReflection = new GenericTypeReflection(targetClass);
         Type resolvedType = typeReflection.resolveReturnType((TypeVariable<?>) declaredType);
         return getClassType(resolvedType);
      }

      return injectionProvider.getClassOfDeclaredType();
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
