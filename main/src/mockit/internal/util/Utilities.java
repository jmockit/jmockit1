/*
 * Copyright (c) 2006 JMockit developers
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.util;

import javax.annotation.*;
import java.io.*;
import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.net.*;
import java.security.*;
import java.util.*;

/**
 * Miscellaneous utility constants and methods.
 */
public final class Utilities
{
   @Nonnull public static final Object[] NO_ARGS = {};
   public static final boolean JAVA8;
   public static final boolean HOTSPOT_VM;

   static {
      float javaVersion = Float.parseFloat(System.getProperty("java.specification.version"));
      JAVA8 = javaVersion >= 1.8F;
      String vmName = System.getProperty("java.vm.name");
      HOTSPOT_VM = vmName.contains("HotSpot") || vmName.contains("OpenJDK");
   }

   private Utilities() {}

   public static void ensureThatMemberIsAccessible(@Nonnull AccessibleObject classMember) {
      //noinspection deprecation
      if (!classMember.isAccessible()) {
         classMember.setAccessible(true);
      }
   }

   public static Method getAnnotatedMethod(Class<?> cls, Class<? extends Annotation> annotation) {
      for (Method method : cls.getMethods()) if (method.getAnnotation(annotation) != null) return method;
      return null;
   }

   public static Method getAnnotatedDeclaredMethod(Class<?> cls, Class<? extends Annotation> annotation) {
      for (Method method : cls.getDeclaredMethods()) if (method.getAnnotation(annotation) != null) return method;
      return null;
   }

   @Nonnull
   public static Class<?> getClassType(@Nonnull Type declaredType) {
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

   public static boolean containsReference(@Nonnull List<?> references, @Nullable Object toBeFound) {
      for (Object reference : references) {
         if (reference == toBeFound) {
            return true;
         }
      }

      return false;
   }

   @Nonnull
   public static String getClassFileLocationPath(@Nonnull Class<?> aClass) {
      CodeSource codeSource = aClass.getProtectionDomain().getCodeSource();
      return getClassFileLocationPath(codeSource);
   }

   @Nonnull
   public static String getClassFileLocationPath(@Nonnull CodeSource codeSource) {
      String locationPath = codeSource.getLocation().getPath();
      try { locationPath = URLDecoder.decode(locationPath, "UTF-8"); } catch (UnsupportedEncodingException ignore) {}
      return locationPath;
   }
}