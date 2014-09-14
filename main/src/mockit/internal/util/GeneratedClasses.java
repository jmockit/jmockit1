/*
 * Copyright (c) 2006-2014 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.util;

import java.lang.reflect.*;

import org.jetbrains.annotations.*;

public final class GeneratedClasses
{
   public static final String SUBCLASS_PREFIX = "$Subclass_";
   public static final String IMPLCLASS_PREFIX = "$Impl_";

   private GeneratedClasses() {}

   @NotNull
   public static String getNameForGeneratedClass(@NotNull Class<?> aClass, @Nullable String suffix)
   {
      String prefix = aClass.isInterface() ? IMPLCLASS_PREFIX : SUBCLASS_PREFIX;
      StringBuilder name = new StringBuilder(60).append(prefix).append(aClass.getSimpleName());

      if (suffix != null) {
         name.append('_').append(suffix);
      }

      if (aClass.getClassLoader() != null) {
         Package targetPackage = aClass.getPackage();

         if (targetPackage != null && !targetPackage.isSealed()) {
            name.insert(0, '.').insert(0, targetPackage.getName());
         }
      }

      return name.toString();
   }

   public static boolean isGeneratedImplementationClass(@NotNull Class<?> mockedType)
   {
      return isGeneratedImplementationClass(mockedType.getName());
   }

   private static boolean isGeneratedSubclass(@NotNull String className)
   {
      return className.contains(SUBCLASS_PREFIX);
   }

   public static boolean isGeneratedImplementationClass(@NotNull String className)
   {
      return className.contains(IMPLCLASS_PREFIX);
   }

   public static boolean isGeneratedClass(@NotNull String className)
   {
      return isGeneratedSubclass(className) || isGeneratedImplementationClass(className);
   }

   @NotNull public static Class<?> getMockedClassOrInterfaceType(@NotNull Class<?> aClass)
   {
      if (Proxy.isProxyClass(aClass) || isGeneratedImplementationClass(aClass)) {
         return getImplementedInterface(aClass);
      }

      if (isGeneratedSubclass(aClass.getName())) {
         return aClass.getSuperclass();
      }

      return aClass;
   }

   @NotNull public static Class<?> getImplementedInterface(@NotNull Class<?> implementationClass)
   {
      // Assumes that a proxy class implements a single interface.
      return implementationClass.getInterfaces()[0];
   }

   @NotNull public static Class<?> getMockedClass(@NotNull Object mock)
   {
      return getMockedClassOrInterfaceType(mock.getClass());
   }

   @NotNull public static Object newInstance(Class<?> generatedClass)
   {
      Constructor<?> publicConstructor;
      try { publicConstructor = generatedClass.getConstructor(); }
      catch (NoSuchMethodException e) { throw new RuntimeException(e); }

      return ConstructorReflection.invoke(publicConstructor);
   }
}
