/*
 * Copyright (c) 2006-2014 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.util;

import java.lang.reflect.*;
import java.util.*;

import mockit.internal.state.*;

import org.jetbrains.annotations.*;

/**
 * Miscellaneous utility constants and methods.
 */
public final class Utilities
{
   @NotNull public static final Object[] NO_ARGS = {};

   public static final boolean JAVA6;
   public static final boolean JAVA7;
   public static final boolean JAVA8;
   public static final boolean HOTSPOT_VM;
   static {
      HOTSPOT_VM = System.getProperty("java.vm.name").contains("HotSpot");
      String javaVersion = System.getProperty("java.specification.version");
      JAVA8 = "1.8".equals(javaVersion);
      JAVA7 = "1.7".equals(javaVersion);
      JAVA6 = "1.6".equals(javaVersion);
   }

   private Utilities() {}

   public static void ensureThatMemberIsAccessible(@NotNull AccessibleObject classMember)
   {
      if (!classMember.isAccessible()) {
         classMember.setAccessible(true);
      }
   }

   public static void ensureThatClassIsInitialized(@NotNull Class<?> aClass)
   {
      ExecutingTest executingTest = TestRun.getExecutingTest();
      boolean previousFlag = executingTest.setShouldIgnoreMockingCallbacks(true);

      try {
         Class.forName(aClass.getName(), true, aClass.getClassLoader());
      }
      catch (ClassNotFoundException ignore) {}
      catch (LinkageError e) {
         StackTrace.filterStackTrace(e);
         e.printStackTrace();
      }
      finally {
         executingTest.setShouldIgnoreMockingCallbacks(previousFlag);
      }
   }

   @NotNull
   public static Class<?> getClassType(@NotNull Type declaredType)
   {
      while (true) {
         if (declaredType instanceof Class<?>) {
            return (Class<?>) declaredType;
         }

         if (declaredType instanceof ParameterizedType) {
            return (Class<?>) ((ParameterizedType) declaredType).getRawType();
         }

         if (declaredType instanceof TypeVariable) {
            declaredType = ((TypeVariable<?>) declaredType).getBounds()[0];
            continue;
         }

         throw new IllegalArgumentException("Type of unexpected kind: " + declaredType);
      }
   }

   @NotNull
   public static Class<?> getClassType(@NotNull GenericArrayType arrayType)
   {
      Type componentType = arrayType.getGenericComponentType();
      int dimensions = 1;

      while (componentType instanceof GenericArrayType) {
         componentType = ((GenericArrayType) componentType).getGenericComponentType();
         dimensions++;
      }

      Class<?> componentClass = getClassType(componentType);
      Object emptyArray = Array.newInstance(componentClass, new int[dimensions]);
      return emptyArray.getClass();
   }

   public static boolean containsReference(@NotNull List<?> references, @Nullable Object toBeFound)
   {
      return indexOfReference(references, toBeFound) >= 0;
   }

   public static int indexOfReference(@NotNull List<?> references, @Nullable Object toBeFound)
   {
      for (int i = 0, n = references.size(); i < n; i++) {
         if (references.get(i) == toBeFound) {
            return i;
         }
      }

      return -1;
   }

   public static int indexOfReference(@NotNull Object[] references, @Nullable Object toBeFound)
   {
      for (int i = 0, n = references.length; i < n; i++) {
         if (references[i] == toBeFound) {
            return i;
         }
      }

      return -1;
   }

   public static boolean isClassAssignableTo(@NotNull List<Class<?>> fromClasses, @NotNull Class<?> toClass)
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
   public static Class<?> findClassAssignableFrom(@NotNull List<Class<?>> toClasses, @NotNull Class<?> fromClass)
   {
      int n = toClasses.size();

      for (int i = 0; i < n; i++) {
         Class<?> toClass = toClasses.get(i);

         if (toClass == fromClass || toClass.isAssignableFrom(fromClass)) {
            return toClass;
         }
      }

      return null;
   }

   public static boolean isClassAssignableFrom(@NotNull List<Class<?>> toClasses, @NotNull Class<?> fromClass)
   {
      return findClassAssignableFrom(toClasses, fromClass) != null;
   }

   @Nullable
   public static Object convertFromString(@NotNull Class<?> targetType, @Nullable String value)
   {
      if (value != null) {
         if (targetType == String.class) {
            return value;
         }
         else if (targetType == char.class) {
            return value.charAt(0);
         }
         else if (targetType.isPrimitive()) {
            Class<?> wrapperClass = AutoBoxing.getWrapperType(targetType);
            assert wrapperClass != null;
            Class<?>[] constructorParameters = {String.class};
            return ConstructorReflection.newInstance(wrapperClass, constructorParameters, value);
         }
         else if (targetType.isEnum()) {
            @SuppressWarnings({"rawtypes", "unchecked"})
            Class<? extends Enum> enumType = (Class<? extends Enum>) targetType;
            return Enum.valueOf(enumType, value);
         }
      }

      return null;
   }
}
