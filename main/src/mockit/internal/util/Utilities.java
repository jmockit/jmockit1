/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.util;

import java.io.*;
import java.lang.reflect.*;
import java.math.*;
import java.net.*;
import java.security.*;
import java.util.*;
import java.util.concurrent.atomic.*;
import javax.annotation.*;

import static java.lang.reflect.Modifier.isPublic;

import static mockit.internal.util.AutoBoxing.*;

/**
 * Miscellaneous utility constants and methods.
 */
public final class Utilities
{
   @Nonnull public static final Object[] NO_ARGS = {};
   public static final float JAVA_VERSION;
   public static final boolean JAVA8;
   public static final boolean JAVA9;
   public static final boolean HOTSPOT_VM;

   static
   {
      JAVA_VERSION = Float.parseFloat(System.getProperty("java.specification.version"));
      JAVA8 = JAVA_VERSION >= 1.8F;
      JAVA9 = JAVA_VERSION >= 9.0F;
      String vmName = System.getProperty("java.vm.name");
      HOTSPOT_VM = vmName.contains("HotSpot") || vmName.contains("OpenJDK");
   }

   private Utilities() {}

   public static void ensureThatMemberIsAccessible(@Nonnull AccessibleObject classMember)
   {
      if (!classMember.isAccessible()) {
         classMember.setAccessible(true);
      }
   }

   @Nonnull
   public static Class<?> getClassType(@Nonnull Type declaredType)
   {
      while (true) {
         if (declaredType instanceof Class<?>) {
            return (Class<?>) declaredType;
         }

         if (declaredType instanceof ParameterizedType) {
            return (Class<?>) ((ParameterizedType) declaredType).getRawType();
         }

         if (declaredType instanceof GenericArrayType) {
            declaredType = ((GenericArrayType) declaredType).getGenericComponentType();
            continue;
         }

         if (declaredType instanceof TypeVariable) {
            declaredType = ((TypeVariable<?>) declaredType).getBounds()[0];
            continue;
         }

         if (declaredType instanceof WildcardType) {
            declaredType = ((WildcardType) declaredType).getUpperBounds()[0];
            continue;
         }

         throw new IllegalArgumentException("Type of unexpected kind: " + declaredType);
      }
   }

   public static boolean containsReference(@Nonnull List<?> references, @Nullable Object toBeFound)
   {
      return indexOfReference(references, toBeFound) >= 0;
   }

   public static int indexOfReference(@Nonnull List<?> references, @Nullable Object toBeFound)
   {
      for (int i = 0, n = references.size(); i < n; i++) {
         if (references.get(i) == toBeFound) {
            return i;
         }
      }

      return -1;
   }

   public static int indexOfReference(@Nonnull Object[] references, @Nullable Object toBeFound)
   {
      for (int i = 0, n = references.length; i < n; i++) {
         if (references[i] == toBeFound) {
            return i;
         }
      }

      return -1;
   }

   public static boolean isClassAssignableTo(@Nonnull List<Class<?>> fromClasses, @Nonnull Class<?> toClass)
   {
      int n = fromClasses.size();

      for (int i = 0; i < n; i++) {
         Class<?> fromClass = fromClasses.get(i);

         if (toClass == fromClass || toClass.isAssignableFrom(fromClass)) {
            return true;
         }
      }

      return false;
   }

   @Nullable
   public static Class<?> findClassAssignableFrom(@Nonnull List<Class<?>> toClasses, @Nonnull Class<?> fromClass)
   {
      for (int i = 0; i < toClasses.size(); i++) {
         Class<?> toClass = toClasses.get(i);

         if (toClass == fromClass || toClass.isAssignableFrom(fromClass)) {
            return toClass;
         }
      }

      return null;
   }

   @SuppressWarnings({"OverlyComplexMethod", "rawtypes", "unchecked"})
   @Nullable
   public static Object convertFromString(@Nonnull Class<?> targetType, @Nullable String value)
   {
      if (value != null) {
         if (targetType == String.class) {
            return value;
         }
         else if (targetType == char.class || targetType == Character.class) {
            return value.charAt(0);
         }
         else if (targetType.isPrimitive()) {
            Class<?> wrapperClass = getWrapperType(targetType);
            assert wrapperClass != null;
            return newWrapperInstance(wrapperClass, value);
         }
         else if (isWrapperOfPrimitiveType(targetType)) {
            return newWrapperInstance(targetType, value);
         }
         else if (targetType == BigDecimal.class) {
            return new BigDecimal(value.trim());
         }
         else if (targetType == BigInteger.class) {
            return new BigInteger(value.trim());
         }
         else if (targetType == AtomicInteger.class) {
            return new AtomicInteger(Integer.parseInt(value.trim()));
         }
         else if (targetType == AtomicLong.class) {
            return new AtomicLong(Long.parseLong(value.trim()));
         }
         else if (targetType.isEnum()) {
            Class<Enum> enumType = (Class<Enum>) targetType;
            Object enumValue = Enum.valueOf(enumType, value);
            return enumValue;
         }
      }

      return null;
   }

   @Nonnull
   private static Object newWrapperInstance(@Nonnull Class<?> wrapperClass, @Nonnull String value)
   {
      for (Constructor<?> constructor : wrapperClass.getDeclaredConstructors()) {
         if (isPublic(constructor.getModifiers())) {
            Class<?>[] parameterTypes = constructor.getParameterTypes();

            if (parameterTypes.length == 1 && parameterTypes[0] == String.class) {
               try { return constructor.newInstance(value.trim()); } catch (Exception ignore) {}
            }
         }
      }

      throw new RuntimeException("Unable to instantiate " + wrapperClass + " with value \"" + value + "\"");
   }

   public static boolean calledFromSpecialThread()
   {
      Thread currentThread = Thread.currentThread();
      return
         "java.awt.EventDispatchThread".equals(currentThread.getClass().getName()) ||
         "system".equals(currentThread.getThreadGroup().getName());
   }

   @Nonnull
   public static String getClassFileLocationPath(@Nonnull Class<?> aClass)
   {
      CodeSource codeSource = aClass.getProtectionDomain().getCodeSource();
      return getClassFileLocationPath(codeSource);
   }

   @Nonnull
   public static String getClassFileLocationPath(@Nonnull CodeSource codeSource)
   {
      String locationPath = codeSource.getLocation().getPath();
      try { locationPath = URLDecoder.decode(locationPath, "UTF-8"); } catch (UnsupportedEncodingException ignore) {}
      return locationPath;
   }
}
