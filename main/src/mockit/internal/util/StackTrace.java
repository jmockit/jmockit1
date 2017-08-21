/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.util;

import java.io.*;
import javax.annotation.*;

/**
 * Provides utility methods to extract and filter stack trace information.
 */
public final class StackTrace
{
   @Nonnull private final Throwable throwable;
   @Nonnull private StackTraceElement[] elements;

   public StackTrace(@Nonnull Throwable throwable)
   {
      this.throwable = throwable;
      elements = throwable.getStackTrace();
   }

   public int getDepth() { return elements.length; }

   @Nonnull
   public StackTraceElement getElement(int index) { return elements[index]; }

   public static void filterStackTrace(@Nonnull Throwable t)
   {
      new StackTrace(t).filter();
   }

   public void filter()
   {
      StackTraceElement[] filteredST = new StackTraceElement[elements.length];
      int i = 0;

      for (StackTraceElement ste : elements) {
         if (ste.getFileName() != null) {
            String where = ste.getClassName();

            if (!isJDKInternalMethod(ste) && !isTestFrameworkMethod(where) && !isJMockitMethod(where)) {
               filteredST[i] = ste;
               i++;
            }
         }
      }

      StackTraceElement[] newStackTrace = new StackTraceElement[i];
      System.arraycopy(filteredST, 0, newStackTrace, 0, i);
      throwable.setStackTrace(newStackTrace);
      elements = newStackTrace;

      Throwable cause = throwable.getCause();

      if (cause != null) {
         new StackTrace(cause).filter();
      }
   }

   private static boolean isJDKInternalMethod(@Nonnull StackTraceElement ste)
   {
      String className = ste.getClassName();

      return
         className.startsWith("sun.") && !ste.isNativeMethod() ||
         className.startsWith("jdk.") || className.startsWith("java.util.") ||
         className.contains(".reflect.") ||
         className.contains(".surefire.") ||
         className.contains(".intellij.") ||
         className.contains(".jdt.");
   }

   private static boolean isTestFrameworkMethod(@Nonnull String where)
   {
      return where.startsWith("org.junit.") || where.startsWith("org.testng.");
   }

   private static boolean isJMockitMethod(@Nonnull String where)
   {
      if (!where.startsWith("mockit.")) {
         return false;
      }

      int p = where.indexOf('$');

      if (p < 0) {
         int q = where.lastIndexOf("Test");
         return q < 0 || q + 4 < where.length();
      }

      int q = where.lastIndexOf("Test", p - 4);

      if (q < 0) {
         return true;
      }

      q += 4;
      return q < where.length() && where.charAt(q) != '$';
   }

   public void print(@Nonnull Appendable output)
   {
      String previousFileName = null;
      int previousLineNumber = 0;
      String sep = "";

      for (int i = 0, d = getDepth(); i < d; i++) {
         StackTraceElement ste = elements[i];

         if (ste.getLineNumber() != previousLineNumber || !ste.getFileName().equals(previousFileName)) {
            try { output.append(sep).append("\tat ").append(ste.toString()); } catch (IOException ignore) {}
            sep = "\n";
            previousFileName = ste.getFileName();
            previousLineNumber = ste.getLineNumber();
         }
      }
   }
}
