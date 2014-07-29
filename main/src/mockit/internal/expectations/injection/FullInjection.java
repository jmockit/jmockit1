package mockit.internal.expectations.injection;

import java.lang.annotation.*;
import java.lang.reflect.*;
import java.util.*;
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

   @NotNull private final Map<Object, Object> instantiatedDependencies;
   @Nullable private final JPADependencies jpaDependencies;

   FullInjection()
   {
      instantiatedDependencies = new HashMap<Object, Object>();
      jpaDependencies = JPADependencies.createIfAvailableInClasspath();
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
      Object dependency = instantiatedDependencies.get(dependencyKey);

      if (dependency == null) {
         Class<?> dependencyClass = fieldToBeInjected.getType();

         if (!fieldType.isInterface()) {
            dependency = newInstanceUsingDefaultConstructorIfAvailable(dependencyClass);
         }
         else if (jpaDependencies != null) {
            dependency =
               jpaDependencies.newInstanceIfApplicable(dependencyClass, dependencyKey, instantiatedDependencies);
         }

         if (dependency != null) {
            Class<?> instantiatedClass = dependency.getClass();

            if (fieldInjection.isClassFromSameModuleOrSystemAsTestedClass(instantiatedClass)) {
               fieldInjection.fillOutDependenciesRecursively(dependency);
               executePostConstructMethodIfAny(instantiatedClass, dependency);
            }

            instantiatedDependencies.put(dependencyKey, dependency);
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
