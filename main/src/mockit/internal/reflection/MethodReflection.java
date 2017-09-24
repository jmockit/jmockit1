/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.reflection;

import java.lang.reflect.*;
import javax.annotation.*;
import static java.lang.reflect.Modifier.*;

import mockit.*;
import mockit.internal.util.*;
import static mockit.internal.reflection.ParameterReflection.*;

public final class MethodReflection
{
   private MethodReflection() {}

   @Nullable
   public static <T> T invokeWithCheckedThrows(
      @Nonnull Class<?> theClass, @Nullable Object targetInstance, @Nonnull String methodName,
      @Nonnull Class<?>[] paramTypes, @Nonnull Object... methodArgs)
      throws Throwable
   {
      Method method = findSpecifiedMethod(theClass, methodName, paramTypes);
      T result = invokeWithCheckedThrows(targetInstance, method, methodArgs);
      return result;
   }

   @Nonnull
   private static Method findSpecifiedMethod(
      @Nonnull Class<?> theClass, @Nonnull String methodName, @Nonnull Class<?>[] paramTypes)
   {
      while (true) {
         Method declaredMethod = findSpecifiedMethodInGivenClass(theClass, methodName, paramTypes);

         if (declaredMethod != null) {
            return declaredMethod;
         }

         Class<?> superClass = theClass.getSuperclass();

         if (superClass == null || superClass == Object.class) {
            String paramTypesDesc = getParameterTypesDescription(paramTypes);
            throw new IllegalArgumentException("Specified method not found: " + methodName + paramTypesDesc);
         }

         //noinspection AssignmentToMethodParameter
         theClass = superClass;
      }
   }

   @Nullable
   private static Method findSpecifiedMethodInGivenClass(
      @Nonnull Class<?> theClass, @Nonnull String methodName, @Nonnull Class<?>[] paramTypes)
   {
      for (Method declaredMethod : theClass.getDeclaredMethods()) {
         if (declaredMethod.getName().equals(methodName)) {
            Class<?>[] declaredParameterTypes = declaredMethod.getParameterTypes();
            int firstRealParameter = indexOfFirstRealParameter(declaredParameterTypes, paramTypes);

            if (
               firstRealParameter >= 0 &&
               matchesParameterTypes(declaredMethod.getParameterTypes(), paramTypes, firstRealParameter)
            ) {
               return declaredMethod;
            }
         }
      }

      return null;
   }

   @Nullable
   public static <T> T invokePublicIfAvailable(
      @Nonnull Class<?> aClass, @Nullable Object targetInstance, @Nonnull String methodName,
      @Nonnull Class<?>[] parameterTypes, @Nonnull Object... methodArgs)
   {
      Method publicMethod;
      try { publicMethod = aClass.getMethod(methodName, parameterTypes); }
      catch (NoSuchMethodException ignore) { return null; }

      T result = invoke(targetInstance, publicMethod, methodArgs);
      return result;
   }

   @Nullable
   public static <T> T invoke(@Nullable Object targetInstance, @Nonnull Method method, @Nonnull Object... methodArgs)
   {
      Utilities.ensureThatMemberIsAccessible(method);

      try {
         //noinspection unchecked
         return (T) method.invoke(targetInstance, methodArgs);
      }
      catch (IllegalAccessException e) {
         throw new RuntimeException(e);
      }
      catch (IllegalArgumentException e) {
         StackTrace.filterStackTrace(e);
         throw new IllegalArgumentException("Failure to invoke method: " + method, e);
      }
      catch (InvocationTargetException e) {
         Throwable cause = e.getCause();

         if (cause instanceof Error) {
            throw (Error) cause;
         }
         else if (cause instanceof RuntimeException) {
            throw (RuntimeException) cause;
         }
         else {
            ThrowOfCheckedException.doThrow((Exception) cause);
            return null;
         }
      }
   }

   @Nullable
   public static <T> T invokeWithCheckedThrows(
      @Nullable Object targetInstance, @Nonnull Method method, @Nonnull Object... methodArgs)
      throws Throwable
   {
      Utilities.ensureThatMemberIsAccessible(method);

      try {
         //noinspection unchecked
         return (T) method.invoke(targetInstance, methodArgs);
      }
      catch (IllegalArgumentException e) {
         StackTrace.filterStackTrace(e);
         throw new IllegalArgumentException("Failure to invoke method: " + method, e);
      }
      catch (InvocationTargetException e) {
         throw e.getCause();
      }
   }

   @Nonnull
   public static String readAnnotationAttribute(@Nonnull Object annotationInstance, @Nonnull String attributeName)
   {
      try {
         Method publicMethod = annotationInstance.getClass().getMethod(attributeName, NO_PARAMETERS);
         String result = (String) publicMethod.invoke(annotationInstance);
         return result;
      }
      catch (NoSuchMethodException e) { throw new RuntimeException(e); }
      catch (IllegalAccessException e) { throw new RuntimeException(e); }
      catch (InvocationTargetException e) { throw new RuntimeException(e.getCause()); }
   }

   @Nonnull
   public static Method findCompatibleMethod(
      @Nonnull Class<?> theClass, @Nonnull String methodName, @Nonnull Class<?>[] argTypes)
   {
      Method methodFound = findCompatibleMethodIfAvailable(theClass, methodName, argTypes);

      if (methodFound != null) {
         return methodFound;
      }

      String argTypesDesc = getParameterTypesDescription(argTypes);
      throw new IllegalArgumentException("No compatible method found: " + methodName + argTypesDesc);
   }

   @Nullable
   private static Method findCompatibleMethodIfAvailable(
      @Nonnull Class<?> theClass, @Nonnull String methodName, @Nonnull Class<?>[] argTypes)
   {
      Method methodFound = null;

      while (true) {
         Method compatibleMethod = findCompatibleMethodInClass(theClass, methodName, argTypes);

         if (
            compatibleMethod != null &&
            (methodFound == null ||
             hasMoreSpecificTypes(compatibleMethod.getParameterTypes(), methodFound.getParameterTypes()))
         ) {
            methodFound = compatibleMethod;
         }

         Class<?> superClass = theClass.getSuperclass();

         if (superClass == null || superClass == Object.class) {
            break;
         }

         //noinspection AssignmentToMethodParameter
         theClass = superClass;
      }

      return methodFound;
   }

   @Nullable
   private static Method findCompatibleMethodInClass(
      @Nonnull Class<?> theClass, @Nonnull String methodName, @Nonnull Class<?>[] argTypes)
   {
      Method found = null;
      Class<?>[] foundParamTypes = null;

      for (Method declaredMethod : theClass.getDeclaredMethods()) {
         if (declaredMethod.getName().equals(methodName)) {
            Class<?>[] declaredParamTypes = declaredMethod.getParameterTypes();
            int firstRealParameter = indexOfFirstRealParameter(declaredParamTypes, argTypes);

            if (
               firstRealParameter >= 0 &&
               (matchesParameterTypes(declaredParamTypes, argTypes, firstRealParameter) ||
                acceptsArgumentTypes(declaredParamTypes, argTypes, firstRealParameter)) &&
               (foundParamTypes == null || hasMoreSpecificTypes(declaredParamTypes, foundParamTypes))
            ) {
               found = declaredMethod;
               foundParamTypes = declaredParamTypes;
            }
         }
      }

      return found;
   }

   @Nonnull
   public static Method findNonPrivateHandlerMethod(@Nonnull Object handler)
   {
      Class<?> handlerClass = handler.getClass();
      Method nonPrivateMethod;

      do {
         nonPrivateMethod = findNonPrivateHandlerMethod(handlerClass);

         if (nonPrivateMethod != null) {
            break;
         }

         handlerClass = handlerClass.getSuperclass();
      }
      while (handlerClass != null && handlerClass != Object.class);

      if (nonPrivateMethod == null) {
         throw new IllegalArgumentException("No non-private instance method found");
      }

      return nonPrivateMethod;
   }

   @Nullable
   private static Method findNonPrivateHandlerMethod(@Nonnull Class<?> handlerClass)
   {
      Method[] declaredMethods = handlerClass.getDeclaredMethods();
      Method found = null;

      for (Method declaredMethod : declaredMethods) {
         int methodModifiers = declaredMethod.getModifiers();

         if (!isPrivate(methodModifiers) && !isStatic(methodModifiers)) {
            if (found != null) {
               String methodType = Delegate.class.isAssignableFrom(handlerClass) ? "delegate" : "invocation handler";
               throw new IllegalArgumentException(
                  "More than one candidate " + methodType + " method found: " +
                  methodSignature(found) + ", " + methodSignature(declaredMethod));
            }

            found = declaredMethod;
         }
      }

      return found;
   }

   @Nonnull
   private static String methodSignature(@Nonnull Method method)
   {
      String signature = method.toGenericString().replace("java.lang.", "");

      int p = signature.lastIndexOf('(');
      int q = signature.lastIndexOf('.', p);

      return signature.substring(q + 1);
   }
}
