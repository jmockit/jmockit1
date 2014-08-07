/*
 * Copyright (c) 2006-2014 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.expectations.injection;

import java.lang.annotation.*;
import java.lang.reflect.*;
import static java.lang.reflect.Modifier.*;

import static mockit.external.asm4.Opcodes.*;
import static mockit.internal.expectations.injection.InjectionPoint.*;
import static mockit.internal.util.ConstructorReflection.*;

import org.jetbrains.annotations.*;

/**
 * Responsible for recursive injection of dependencies as requested by a {@code @Tested(fullyInitialized = true)} field.
 */
final class FullInjection
{
   private static final int INVALID_TYPES = ACC_ABSTRACT + ACC_ANNOTATION + ACC_ENUM;

   @NotNull private final InjectionState injectionState;
   @Nullable private final JPADependencies jpaDependencies;

   FullInjection(@NotNull InjectionState injectionState)
   {
      this.injectionState = injectionState;
      jpaDependencies = JPADependencies.createIfAvailableInClasspath(injectionState);
   }

   @Nullable
   Object newInstanceCreatedWithNoArgsConstructorIfAvailable(
      @NotNull FieldInjection fieldInjection, @NotNull Field fieldToBeInjected)
   {
      Class<?> fieldType = fieldToBeInjected.getType();

      if (!isInstantiableType(fieldType)) {
         return null;
      }

      Object dependencyKey = getDependencyKey(fieldToBeInjected);
      Object dependency = injectionState.getInstantiatedDependency(dependencyKey);

      if (dependency == null) {
         Class<?> dependencyClass = fieldToBeInjected.getType();

         if (!fieldType.isInterface()) {
            dependency = newInstanceUsingDefaultConstructorIfAvailable(dependencyClass);
         }
         else if (jpaDependencies != null) {
            dependency = jpaDependencies.newInstanceIfApplicable(dependencyClass, dependencyKey);
         }

         if (dependency != null) {
            Class<?> instantiatedClass = dependency.getClass();

            if (fieldInjection.isClassFromSameModuleOrSystemAsTestedClass(instantiatedClass)) {
               fieldInjection.fillOutDependenciesRecursively(dependency);
               executePostConstructMethodIfAny(instantiatedClass, dependency);
            }

            injectionState.saveInstantiatedDependency(dependencyKey, dependency);
         }
      }

      return dependency;
   }

   private static boolean isInstantiableType(@NotNull Class<?> type)
   {
      if (type.isPrimitive() || type.isArray()) {
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

   @NotNull
   private Object getDependencyKey(@NotNull Field fieldToBeInjected)
   {
      Class<?> dependencyClass = fieldToBeInjected.getType();

      if (jpaDependencies != null) {
         for (Annotation annotation : fieldToBeInjected.getDeclaredAnnotations()) {
            String id = JPADependencies.getDependencyIdIfAvailable(annotation);

            if (id != null && !id.isEmpty()) {
               return dependencyClass.getName() + ':' + id;
            }
         }
      }

      return dependencyClass;
   }
}
