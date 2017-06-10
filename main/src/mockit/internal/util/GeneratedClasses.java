/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.util;

import java.lang.reflect.*;
import javax.annotation.*;

public final class GeneratedClasses
{
   private static final String SUBCLASS_PREFIX = "$Subclass_";
   private static final String IMPLCLASS_PREFIX = "$Impl_";

   private GeneratedClasses() {}

   @Nonnull
   public static String getNameForGeneratedClass(@Nonnull Class<?> aClass, @Nullable String suffix)
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

   public static boolean isGeneratedImplementationClass(@Nonnull Class<?> mockedType)
   {
      return isGeneratedImplementationClass(mockedType.getName());
   }

   public static boolean isGeneratedSubclass(@Nonnull String className) { return className.contains(SUBCLASS_PREFIX); }

   public static boolean isExternallyGeneratedSubclass(@Nonnull String className)
   {
      int p = className.indexOf('$') + 1;

      //noinspection SimplifiableIfStatement
      if (p < 2 || p == className.length() || className.charAt(p) != '$') {
         return false;
      }

      return className.contains("_$$_javassist_") || className.contains("_$$_jvst") || className.contains("CGLIB$$");
   }

   public static boolean isGeneratedImplementationClass(@Nonnull String className)
   {
      return className.contains(IMPLCLASS_PREFIX);
   }

   public static boolean isGeneratedClass(@Nonnull String className)
   {
      return isGeneratedSubclass(className) || isGeneratedImplementationClass(className);
   }

   @Nonnull
   public static Class<?> getMockedClassOrInterfaceType(@Nonnull Class<?> aClass)
   {
      if (Proxy.isProxyClass(aClass) || isGeneratedImplementationClass(aClass)) {
         return getImplementedInterface(aClass);
      }

      if (isGeneratedSubclass(aClass.getName())) {
         return aClass.getSuperclass();
      }

      return aClass;
   }

   @Nonnull
   public static Class<?> getImplementedInterface(@Nonnull Class<?> implementationClass)
   {
      // Assumes that a proxy class implements a single interface.
      return implementationClass.getInterfaces()[0];
   }

   @Nonnull
   public static Class<?> getMockedClass(@Nonnull Object mock)
   {
      return getMockedClassOrInterfaceType(mock.getClass());
   }
}
