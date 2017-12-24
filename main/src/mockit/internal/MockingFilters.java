/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal;

import java.util.*;
import javax.annotation.*;

public final class MockingFilters
{
   private static final Map<String, String> FILTERS = new HashMap<String, String>();
   static {
      FILTERS.put("java/lang/Object", "<init> clone getClass hashCode wait notify notifyAll ");
      FILTERS.put("java/lang/System", "arraycopy getProperties getSecurityManager identityHashCode mapLibraryName ");
      FILTERS.put("java/lang/Thread", "currentThread getName getThreadGroup interrupted isInterrupted ");
      FILTERS.put("java/io/File", "<init> compareTo equals getName getPath hashCode toString ");
      FILTERS.put("java/util/logging/Logger", "<init> getName ");
      FILTERS.put("java/util/jar/JarEntry", "<init> ");
   }

   private MockingFilters() {}

   @Nullable
   public static String filtersForClass(@Nonnull String classDesc) { return FILTERS.get(classDesc); }

   public static boolean isUnmockable(@Nonnull String classDesc)
   {
      return
         ("java/lang/String java/lang/AbstractStringBuilder java/util/AbstractCollection java/util/AbstractMap " +
          "java/util/Hashtable java/lang/Throwable " +
          "java/lang/ClassLoader java/lang/Math java/lang/StrictMath java/time/Duration").contains(classDesc) ||
         "java/lang/ThreadLocal".equals(classDesc) || "java/nio/file/Paths".equals(classDesc);
   }

   public static boolean isFullMockingDisallowed(@Nonnull String classDesc)
   {
      return classDesc.startsWith("java/io/") && (
         "java/io/FileOutputStream".equals(classDesc) || "java/io/FileInputStream".equals(classDesc) ||
         "java/io/FileWriter".equals(classDesc) ||
         "java/io/PrintWriter java/io/Writer java/io/DataInputStream".contains(classDesc)
      );
   }

   public static boolean isUnmockableInvocation(@Nullable String mockingFilters, @Nonnull String name)
   {
      if (mockingFilters == null) {
         return false;
      }

      if (mockingFilters.isEmpty()) {
         return true;
      }

      int i = mockingFilters.indexOf(name);
      return i > -1 && mockingFilters.charAt(i + name.length()) == ' ';
   }

   public static boolean isSubclassOfUnmockable(@Nonnull Class<?> aClass)
   {
      return
         AbstractCollection.class.isAssignableFrom(aClass) ||
         AbstractMap.class.isAssignableFrom(aClass) ||
         Hashtable.class.isAssignableFrom(aClass) ||
         Throwable.class.isAssignableFrom(aClass) ||
         ClassLoader.class.isAssignableFrom(aClass) ||
         ThreadLocal.class.isAssignableFrom(aClass);
   }
}
