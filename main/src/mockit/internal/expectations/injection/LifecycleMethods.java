/*
 * Copyright (c) 2006-2015 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.expectations.injection;

import java.lang.reflect.*;
import java.util.*;
import javax.annotation.*;
import javax.servlet.*;

import mockit.internal.util.*;
import static mockit.internal.expectations.injection.InjectionPoint.*;
import static mockit.internal.util.Utilities.NO_ARGS;

final class LifecycleMethods
{
   private final Map<Class<?>, Method> initializationMethods;
   private final Map<Class<?>, Method> terminationMethods;
   private final List<Object> objectsWithPreDestroyMethodsToExecute;
   Object servletConfig;

   LifecycleMethods()
   {
      initializationMethods = new IdentityHashMap<Class<?>, Method>();
      terminationMethods = new IdentityHashMap<Class<?>, Method>();
      objectsWithPreDestroyMethodsToExecute = new ArrayList<Object>();
   }

   private void findLifecycleMethods(@Nonnull Class<?> testedClass)
   {
      boolean isServlet = isServlet(testedClass);
      Method initializationMethod = null;
      Method terminationMethod = null;
      int methodsFound = 0;
      Class<?> classWithMethods = testedClass;

      do {
         for (Method method : classWithMethods.getDeclaredMethods()) {
            if (method.isSynthetic()) {
               continue;
            }
            else if (initializationMethod == null && isInitializationMethod(method, isServlet)) {
               initializationMethod = method;
               methodsFound++;
            }
            else if (terminationMethod == null && isTerminationMethod(method, isServlet)) {
               terminationMethod = method;
               methodsFound++;
            }

            if (methodsFound == 2) {
               break;
            }
         }

         classWithMethods = classWithMethods.getSuperclass();
      }
      while (methodsFound < 2 && classWithMethods != Object.class);

      initializationMethods.put(testedClass, initializationMethod);
      terminationMethods.put(testedClass, terminationMethod);
   }

   private static boolean isInitializationMethod(@Nonnull Method method, boolean isServlet)
   {
      if (method.isAnnotationPresent(PostConstruct.class)) {
         return true;
      }

      if (isServlet && "init".equals(method.getName())) {
         Class<?>[] parameterTypes = method.getParameterTypes();
         return parameterTypes.length == 0 || parameterTypes.length == 1 && parameterTypes[0] == ServletConfig.class;
      }

      return false;
   }

   private static boolean isTerminationMethod(@Nonnull Method method, boolean isServlet)
   {
      if (method.isAnnotationPresent(PreDestroy.class)) {
         return true;
      }

      return isServlet && "destroy".equals(method.getName()) && method.getParameterTypes().length == 0;
   }

   void executePostConstructMethodIfAny(@Nonnull Class<?> testedClass, @Nonnull Object testedObject)
   {
      Method postConstructMethod = getLifecycleMethod(testedClass);

      if (postConstructMethod != null) {
         Object[] args = NO_ARGS;

         if ("init".equals(postConstructMethod.getName()) && postConstructMethod.getParameterTypes().length == 1) {
            args = new Object[] {servletConfig};
         }

         MethodReflection.invoke(testedObject, postConstructMethod, args);
      }

      Method preDestroyMethod = terminationMethods.get(testedClass);

      if (preDestroyMethod != null) {
         objectsWithPreDestroyMethodsToExecute.add(testedObject);
      }
   }

   @Nullable
   private Method getLifecycleMethod(@Nonnull Class<?> testedClass)
   {
      if (!initializationMethods.containsKey(testedClass)) {
         findLifecycleMethods(testedClass);
      }

      return initializationMethods.get(testedClass);
   }

   void executePreDestroyMethodsIfAny()
   {
      try {
         for (Object testedObject : objectsWithPreDestroyMethodsToExecute) {
            executePreDestroyMethod(testedObject);
         }
      }
      finally {
         objectsWithPreDestroyMethodsToExecute.clear();
      }
   }

   private void executePreDestroyMethod(@Nonnull Object testedObject)
   {
      Class<?> testedClass = testedObject.getClass();
      Method preDestroyMethod = terminationMethods.get(testedClass);

      try { MethodReflection.invoke(testedObject, preDestroyMethod); }
      catch (RuntimeException ignore) {}
      catch (AssertionError ignore) {}
   }
}
