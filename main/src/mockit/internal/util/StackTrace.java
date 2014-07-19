/*
 * Copyright (c) 2006-2014 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.util;

import java.lang.reflect.*;

import org.jetbrains.annotations.*;

/**
 * Provides optimized utility methods to extract stack trace information.
 */
public final class StackTrace
{
   private static final Method getStackTraceDepth = getThrowableMethod("getStackTraceDepth");
   private static final Method getStackTraceElement = getThrowableMethod("getStackTraceElement", int.class);

   @Nullable private static Method getThrowableMethod(@NotNull String name, @NotNull Class<?>... parameterTypes)
   {
      Method method;
      try { method = Throwable.class.getDeclaredMethod(name, parameterTypes); }
      catch (NoSuchMethodException ignore) { return null; }
      method.setAccessible(true);
      return method;
   }

   @NotNull private final Throwable throwable;
   @Nullable private final StackTraceElement[] elements;

   public StackTrace() { this(new Throwable()); }

   public StackTrace(@NotNull Throwable throwable)
   {
      this.throwable = throwable;
      elements = getStackTraceDepth == null ? throwable.getStackTrace() : null;
   }

   public int getDepth()
   {
      if (elements != null) {
         return elements.length;
      }

      int depth = 0;

      try {
         depth = (Integer) getStackTraceDepth.invoke(throwable);
      }
      catch (IllegalAccessException ignore) {}
      catch (InvocationTargetException ignored) {}

      return depth;
   }

   @NotNull
   public StackTraceElement getElement(int index)
   {
      return elements == null ? getElement(throwable, index) : elements[index];
   }

   @NotNull
   public static StackTraceElement getElement(@NotNull Throwable throwable, int index)
   {
      try { return (StackTraceElement) getStackTraceElement.invoke(throwable, index); }
      catch (IllegalAccessException e) { throw new RuntimeException(e); }
      catch (InvocationTargetException e) { throw new RuntimeException(e); }
   }

   public static void filterStackTrace(@NotNull Throwable t)
   {
      new StackTrace(t).filter();
   }

   public void filter()
   {
      int n = getDepth();
      StackTraceElement[] filteredST = new StackTraceElement[n];
      int j = 0;

      for (int i = 0; i < n; i++) {
         StackTraceElement ste = getElement(i);

         if (ste.getFileName() != null) {
            String where = ste.getClassName();

            if (!isSunMethod(ste) && !isTestFrameworkMethod(where) && !isJMockitMethod(where)) {
               filteredST[j] = ste;
               j++;
            }
         }
      }

      StackTraceElement[] newStackTrace = new StackTraceElement[j];
      System.arraycopy(filteredST, 0, newStackTrace, 0, j);
      throwable.setStackTrace(newStackTrace);

      Throwable cause = throwable.getCause();

      if (cause != null) {
         new StackTrace(cause).filter();
      }
   }

   private static boolean isSunMethod(@NotNull StackTraceElement ste)
   {
      return ste.getClassName().startsWith("sun.") && !ste.isNativeMethod();
   }

   private static boolean isTestFrameworkMethod(@NotNull String where)
   {
      return where.startsWith("org.junit.") || where.startsWith("org.testng.");
   }

   private static boolean isJMockitMethod(@NotNull String where)
   {
      if (!where.startsWith("mockit.")) {
         return false;
      }

      int p = where.lastIndexOf("Test") + 4;

      if (p < 4) {
         return true;
      }

      return p < where.length() && where.charAt(p) != '$';
   }
}
