/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.reflection;

import java.lang.reflect.*;
import javax.annotation.*;

import mockit.internal.util.*;
import static mockit.internal.reflection.ParameterReflection.*;
import static mockit.internal.util.Utilities.ensureThatMemberIsAccessible;

public final class ConstructorReflection
{
   private static final Constructor<?> OBJECT_CONSTRUCTOR;
   static
   {
      try { OBJECT_CONSTRUCTOR = Object.class.getConstructor(); }
      catch (NoSuchMethodException e) { throw new RuntimeException(e); }
   }

   private ConstructorReflection() {}

   @Nonnull
   static <T> Constructor<T> findSpecifiedConstructor(@Nonnull Class<?> theClass, @Nonnull Class<?>[] paramTypes)
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

   @Nonnull
   public static <T> T invoke(@Nonnull Constructor<T> constructor, @Nonnull Object... initArgs)
   {
      ensureThatMemberIsAccessible(constructor);

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
            throw new IllegalStateException("Should never get here", cause);
         }
      }
   }

   @Nonnull
   public static <T> T newInstance(@Nonnull Class<? extends T> aClass, @Nullable Object... nonNullArgs)
   {
      if (nonNullArgs == null) {
         throw invalidArguments();
      }

      Class<?>[] argTypes = getArgumentTypesFromArgumentValues(nonNullArgs);
      Constructor<T> constructor = findCompatibleConstructor(aClass, argTypes);
      return invoke(constructor, nonNullArgs);
   }

   @Nonnull
   private static <T> Constructor<T> findCompatibleConstructor(@Nonnull Class<?> theClass, @Nonnull Class<?>[] argTypes)
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

      if (paramTypes.length > argTypes.length && paramTypes[0] == declaringClass) {
         throw new IllegalArgumentException("Invalid instantiation of inner class");
      }

      String argTypesDesc = getParameterTypesDescription(argTypes);
      throw new IllegalArgumentException("No compatible constructor found: " + theClass.getSimpleName() + argTypesDesc);
   }

   @Nonnull
   public static <T> T newInstanceUsingDefaultConstructor(@Nonnull Class<T> aClass)
   {
      try {
         //noinspection ClassNewInstance
         return aClass.newInstance();
      }
      catch (InstantiationException ie) {
         throw new RuntimeException(ie);
      }
      catch (IllegalAccessException ignore) {
         return newInstance(aClass, (Object[]) NO_PARAMETERS);
      }
   }

   @Nullable
   public static <T> T newInstanceUsingDefaultConstructorIfAvailable(@Nonnull Class<T> aClass)
   {
      try {
         //noinspection ClassNewInstance
         return aClass.newInstance();
      }
      catch (InstantiationException ignore) { return null; }
      catch (IllegalAccessException ignore) { return null; }
   }

   @Nonnull
   public static <T> T newInstanceUsingPublicDefaultConstructor(@Nonnull Class<T> aClass)
   {
      Constructor<T> publicConstructor;
      try { publicConstructor = aClass.getConstructor(); }
      catch (NoSuchMethodException e) { throw new RuntimeException(e); }

      return invoke(publicConstructor);
   }

   @Nullable
   public static <T> T newInstanceUsingPublicConstructorIfAvailable(
      @Nonnull Class<T> aClass, @Nonnull Class<?>[] parameterTypes, @Nonnull Object... initArgs)
   {
      Constructor<T> publicConstructor;
      try { publicConstructor = aClass.getConstructor(parameterTypes); }
      catch (NoSuchMethodException ignore) { return null; }

      return invoke(publicConstructor, initArgs);
   }

   @SuppressWarnings({"UnnecessaryFullyQualifiedName", "UseOfSunClasses"})
   private static final sun.reflect.ReflectionFactory REFLECTION_FACTORY =
      sun.reflect.ReflectionFactory.getReflectionFactory();

   @Nonnull
   public static <T> T newUninitializedInstance(@Nonnull Class<T> aClass)
   {
      try {
         Constructor<?> fakeConstructor = REFLECTION_FACTORY.newConstructorForSerialization(aClass, OBJECT_CONSTRUCTOR);

         if (fakeConstructor == null) { // can happen on Java 9
            //noinspection ConstantConditions
            return null;
         }

         @SuppressWarnings("unchecked") T newInstance = (T) fakeConstructor.newInstance();
         return newInstance;
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
