/*
 * Copyright (c) 2006-2014 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.util;

import java.lang.reflect.*;

import org.jetbrains.annotations.*;
import sun.reflect.*;

import static mockit.internal.util.ParameterReflection.*;

public final class ConstructorReflection
{
   @SuppressWarnings("UseOfSunClasses")
   private static final ReflectionFactory REFLECTION_FACTORY = ReflectionFactory.getReflectionFactory();

   private static final Constructor<?> OBJECT_CONSTRUCTOR;
   static
   {
      try { OBJECT_CONSTRUCTOR = Object.class.getConstructor(); }
      catch (NoSuchMethodException e) { throw new RuntimeException(e); }
   }

   private ConstructorReflection() {}

   @NotNull
   public static <T> T newInstance(
      @NotNull Class<T> aClass, @NotNull Class<?>[] parameterTypes, @Nullable Object... initArgs)
   {
      if (initArgs == null) {
         throw invalidArguments();
      }

      Constructor<T> constructor = findSpecifiedConstructor(aClass, parameterTypes);
      return invoke(constructor, initArgs);
   }

   @NotNull
   public static <T> Constructor<T> findSpecifiedConstructor(@NotNull Class<?> theClass, @NotNull Class<?>[] paramTypes)
   {
      for (Constructor<?> declaredConstructor : theClass.getDeclaredConstructors()) {
         Class<?>[] declaredParameterTypes = declaredConstructor.getParameterTypes();
         int firstRealParameter = indexOfFirstRealParameter(declaredParameterTypes, paramTypes);

         if (
            firstRealParameter >= 0 &&
            matchesParameterTypes(declaredParameterTypes, paramTypes, firstRealParameter)
         ) {
            //noinspection unchecked
            return (Constructor<T>) declaredConstructor;
         }
      }

      String paramTypesDesc = getParameterTypesDescription(paramTypes);

      throw new IllegalArgumentException(
         "Specified constructor not found: " + theClass.getSimpleName() + paramTypesDesc);
   }

   @NotNull
   public static <T> T invoke(@NotNull Constructor<T> constructor, @NotNull Object... initArgs)
   {
      Utilities.ensureThatMemberIsAccessible(constructor);

      try {
         return constructor.newInstance(initArgs);
      }
      catch (InstantiationException e) {
         throw new RuntimeException(e);
      }
      catch (IllegalAccessException e) {
         throw new RuntimeException(e);
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
            throw new IllegalStateException("Should never get here");
         }
      }
   }

   @NotNull
   public static <T> T newInstance(
      @NotNull String className, @NotNull Class<?>[] parameterTypes, @Nullable Object... initArgs)
   {
      if (initArgs == null) {
         throw invalidArguments();
      }

      Class<T> theClass = ClassLoad.loadClass(className);
      return newInstance(theClass, parameterTypes, initArgs);
   }

   @NotNull
   public static <T> T newInstance(@NotNull String className, @Nullable Object... nonNullArgs)
   {
      if (nonNullArgs == null) {
         throw invalidArguments();
      }

      Class<?>[] argTypes = getArgumentTypesFromArgumentValues(nonNullArgs);
      Class<T> theClass = ClassLoad.loadClass(className);
      Constructor<T> constructor = findCompatibleConstructor(theClass, argTypes);
      return invoke(constructor, nonNullArgs);
   }

   @NotNull
   private static <T> Constructor<T> findCompatibleConstructor(@NotNull Class<?> theClass, @NotNull Class<?>[] argTypes)
   {
      Constructor<T> found = null;
      Class<?>[] foundParameters = null;
      Constructor<?>[] declaredConstructors = theClass.getDeclaredConstructors();

      for (Constructor<?> declaredConstructor : declaredConstructors) {
         Class<?>[] declaredParamTypes = declaredConstructor.getParameterTypes();
         int firstRealParameter = indexOfFirstRealParameter(declaredParamTypes, argTypes);

         if (
            firstRealParameter >= 0 &&
            (matchesParameterTypes(declaredParamTypes, argTypes, firstRealParameter) ||
             acceptsArgumentTypes(declaredParamTypes, argTypes, firstRealParameter)) &&
            (found == null || hasMoreSpecificTypes(declaredParamTypes, foundParameters))
         ) {
            //noinspection unchecked
            found = (Constructor<T>) declaredConstructor;
            foundParameters = declaredParamTypes;
         }
      }

      if (found != null) {
         return found;
      }

      Class<?> declaringClass = theClass.getDeclaringClass();
      Class<?>[] paramTypes = declaredConstructors[0].getParameterTypes();

      if (paramTypes[0] == declaringClass && paramTypes.length > argTypes.length) {
         throw new IllegalArgumentException("Invalid instantiation of inner class; use newInnerInstance instead");
      }

      String argTypesDesc = getParameterTypesDescription(argTypes);
      throw new IllegalArgumentException("No compatible constructor found: " + theClass.getSimpleName() + argTypesDesc);
   }

   @NotNull
   public static <T> T newInstance(@NotNull Class<? extends T> aClass, @Nullable Object... nonNullArgs)
   {
      if (nonNullArgs == null) {
         throw invalidArguments();
      }

      Class<?>[] argTypes = getArgumentTypesFromArgumentValues(nonNullArgs);
      Constructor<T> constructor = findCompatibleConstructor(aClass, argTypes);
      return invoke(constructor, nonNullArgs);
   }

   @NotNull
   public static <T> T newInstance(@NotNull Class<T> aClass)
   {
      return newInstance(aClass, NO_PARAMETERS);
   }

   @NotNull
   public static <T> T newInstanceUsingDefaultConstructor(@NotNull Class<T> aClass)
   {
      try {
         //noinspection ClassNewInstance
         return aClass.newInstance();
      }
      catch (InstantiationException ie) {
         throw new RuntimeException(ie);
      }
      catch (IllegalAccessException ignore) {
         return newInstance(aClass);
      }
   }

   @Nullable
   public static <T> T newInstanceUsingPublicConstructorIfAvailable(
      @NotNull Class<T> aClass, @NotNull Class<?>[] parameterTypes, @NotNull Object... initArgs)
   {
      Constructor<T> publicConstructor;
      try { publicConstructor = aClass.getConstructor(parameterTypes); }
      catch (NoSuchMethodException ignore) { return null; }

      return invoke(publicConstructor, initArgs);
   }

   @NotNull
   public static <T> T newInnerInstance(
      @NotNull Class<? extends T> innerClass, @NotNull Object outerInstance, @Nullable Object... nonNullArgs)
   {
      if (nonNullArgs == null) {
         throw invalidArguments();
      }

      Object[] initArgs = argumentsWithExtraFirstValue(nonNullArgs, outerInstance);
      return newInstance(innerClass, initArgs);
   }

   @NotNull
   public static <T> T newInnerInstance(
      @NotNull String innerClassName, @NotNull Object outerInstance, @Nullable Object... nonNullArgs)
   {
      if (nonNullArgs == null) {
         throw invalidArguments();
      }

      Class<?> outerClass = outerInstance.getClass();
      ClassLoader loader = outerClass.getClassLoader();
      String className = outerClass.getName() + '$' + innerClassName;
      Class<T> innerClass = ClassLoad.loadFromLoader(loader, className);

      return newInnerInstance(innerClass, outerInstance, nonNullArgs);
   }

   @NotNull
   public static <T> T newUninitializedInstance(@NotNull Class<T> aClass)
   {
      @SuppressWarnings("unchecked")
      Constructor<T> fakeConstructor = REFLECTION_FACTORY.newConstructorForSerialization(aClass, OBJECT_CONSTRUCTOR);

      try {
         return fakeConstructor.newInstance();
      }
      catch (NoClassDefFoundError e) {
         StackTrace.filterStackTrace(e);
         e.printStackTrace();
         throw e;
      }
      catch (ExceptionInInitializerError e) {
         StackTrace.filterStackTrace(e);
         e.printStackTrace();
         throw e;
      }
      catch (InstantiationException e) { throw new RuntimeException(e); }
      catch (IllegalAccessException e) { throw new RuntimeException(e); }
      catch (InvocationTargetException e) { throw new RuntimeException(e.getCause()); }
   }
}
