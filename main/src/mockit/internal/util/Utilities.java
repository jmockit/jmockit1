/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.util;

import mockit.internal.state.ExecutingTest;
import mockit.internal.state.TestRun;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.reflect.*;
import java.util.List;

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
         classMember.setAccessible(true) ;
      }
   }

   static void ensureThatMemberIsAccessible(@Nonnull Field field)
   {
      if (field.isAccessible()) return;
      try {
         field.setAccessible(true);
         Field modifiers = Field.class.getDeclaredField("modifiers");
         modifiers.setAccessible(true);
         modifiers.setInt(field, field.getModifiers() & ~Modifier.FINAL);
      } catch (NoSuchFieldException e) {
         throw new RuntimeException(e);
      } catch (IllegalAccessException e) {
         throw new RuntimeException(e);
      }
   }

   public static void ensureThatClassIsInitialized(@Nonnull Class<?> aClass)
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

         if (declaredType instanceof TypeVariable) {
            declaredType = ((TypeVariable<?>) declaredType).getBounds()[0];
            continue;
         }

         throw new IllegalArgumentException("Type of unexpected kind: " + declaredType);
      }
   }

   @Nonnull
   public static Class<?> getClassType(@Nonnull GenericArrayType arrayType)
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
      int n = toClasses.size();

      for (int i = 0; i < n; i++) {
         Class<?> toClass = toClasses.get(i);

         if (toClass == fromClass || toClass.isAssignableFrom(fromClass)) {
            return toClass;
         }
      }

      return null;
   }

   public static boolean isClassAssignableFrom(@Nonnull List<Class<?>> toClasses, @Nonnull Class<?> fromClass)
   {
      return findClassAssignableFrom(toClasses, fromClass) != null;
   }

   @Nullable
   public static Object convertFromString(@Nonnull Class<?> targetType, @Nullable String value)
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

   public static boolean calledFromSpecialThread()
   {
      Thread currentThread = Thread.currentThread();
      return
         "java.awt.EventDispatchThread".equals(currentThread.getClass().getName()) ||
         "system".equals(currentThread.getThreadGroup().getName());
   }
}
