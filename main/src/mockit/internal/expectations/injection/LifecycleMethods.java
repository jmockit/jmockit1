/*
 * Copyright (c) 2006-2015 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.expectations.injection;

import java.lang.reflect.*;
import java.util.*;
import java.util.Map.*;
import javax.annotation.*;
import javax.servlet.*;

import mockit.internal.util.*;
import static mockit.internal.expectations.injection.InjectionPoint.*;
import static mockit.internal.util.Utilities.NO_ARGS;

final class LifecycleMethods
{
   @Nonnull private final List<Class<?>> classesSearched;
   @Nonnull private final Map<Class<?>, Method> initializationMethods;
   @Nonnull private final Map<Class<?>, Method> terminationMethods;
   @Nonnull private final Map<Class<?>, Object> objectsWithTerminationMethodsToExecute;
   @Nullable Object servletConfig;

   LifecycleMethods()
   {
      classesSearched = new ArrayList<Class<?>>();
      initializationMethods = new IdentityHashMap<Class<?>, Method>();
      terminationMethods = new IdentityHashMap<Class<?>, Method>();
      objectsWithTerminationMethodsToExecute = new IdentityHashMap<Class<?>, Object>();
   }

   void findLifecycleMethods(@Nonnull Class<?> testedClass)
   {
      if (classesSearched.contains(testedClass)) {
         return;
      }

      boolean isServlet = isServlet(testedClass);
      Class<?> classWithLifecycleMethods = testedClass;

      do {
         Method initializationMethod = null;
         Method terminationMethod = null;
         int methodsFoundInSameClass = 0;

         for (Method method : classWithLifecycleMethods.getDeclaredMethods()) {
            if (method.isSynthetic()) {
               continue;
            }
            else if (initializationMethod == null && isInitializationMethod(method, isServlet)) {
               initializationMethods.put(classWithLifecycleMethods, method);
               initializationMethod = method;
               methodsFoundInSameClass++;
            }
            else if (terminationMethod == null && isTerminationMethod(method, isServlet)) {
               terminationMethods.put(classWithLifecycleMethods, method);
               terminationMethod = method;
               methodsFoundInSameClass++;
            }

            if (methodsFoundInSameClass == 2) {
               break;
            }
         }

         classWithLifecycleMethods = classWithLifecycleMethods.getSuperclass();
      }
      while (classWithLifecycleMethods != Object.class);

      classesSearched.add(testedClass);
   }

   private static boolean isInitializationMethod(@Nonnull Method method, boolean isServlet)
   {
      if (method.isAnnotationPresent(PostConstruct.class)) {
         return true;
      }

      if (isServlet && "init".equals(method.getName())) {
         Class<?>[] parameterTypes = method.getParameterTypes();
         return parameterTypes.length == 1 && parameterTypes[0] == ServletConfig.class;
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

   void executeInitializationMethodsIfAny(@Nonnull Class<?> testedClass, @Nonnull Object testedObject)
   {
      Class<?> superclass = testedClass.getSuperclass();

      if (superclass != Object.class) {
         executeInitializationMethodsIfAny(superclass, testedObject);
      }

      Method postConstructMethod = initializationMethods.get(testedClass);

      if (postConstructMethod != null) {
         executeInitializationMethod(testedObject, postConstructMethod);
      }

      Method preDestroyMethod = terminationMethods.get(testedClass);

      if (preDestroyMethod != null) {
         objectsWithTerminationMethodsToExecute.put(testedClass, testedObject);
      }
   }

   private void executeInitializationMethod(@Nonnull Object testedObject, @Nonnull Method initializationMethod)
   {
      Object[] args = NO_ARGS;

      if ("init".equals(initializationMethod.getName()) && initializationMethod.getParameterTypes().length == 1) {
         args = new Object[] {servletConfig};
      }

      MethodReflection.invoke(testedObject, initializationMethod, args);
   }

   void executeTerminationMethodsIfAny()
   {
      try {
         for (Entry<Class<?>, Object> testedClassAndObject : objectsWithTerminationMethodsToExecute.entrySet()) {
            executeTerminationMethod(testedClassAndObject.getKey(), testedClassAndObject.getValue());
         }
      }
      finally {
         objectsWithTerminationMethodsToExecute.clear();
      }
   }

   private void executeTerminationMethod(@Nonnull Class<?> testedClass, @Nonnull Object testedObject)
   {
      Method terminationMethod = terminationMethods.get(testedClass);

      try { MethodReflection.invoke(testedObject, terminationMethod); }
      catch (RuntimeException ignore) {}
      catch (AssertionError ignore) {}
   }
}
