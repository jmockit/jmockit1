/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.expectations;

import java.util.*;
import javax.annotation.*;

public final class MockingFilters
{
   private static final Map<String, String> FILTERS = new HashMap<String, String>();
   static {
      FILTERS.put("java/lang/Object", "<init> clone getClass hashCode ");
      FILTERS.put("java/lang/AbstractStringBuilder", "");
      FILTERS.put("java/lang/String", "");
      FILTERS.put("java/lang/StringBuffer", "");
      FILTERS.put("java/lang/StringBuilder", "");
      FILTERS.put("java/lang/System", "arraycopy getProperties getSecurityManager identityHashCode mapLibraryName ");
      FILTERS.put("java/lang/Exception", "<init> ");
      FILTERS.put("java/lang/Throwable", "<init> fillInStackTrace ");
      FILTERS.put("java/lang/Thread", "currentThread getName getThreadGroup interrupted isInterrupted ");
      FILTERS.put("java/lang/ThreadLocal", "");
      FILTERS.put("java/io/File", "<init> compareTo equals getAbsolutePath getName getPath hashCode toString ");
      FILTERS.put("java/util/AbstractCollection", "<init> ");
      FILTERS.put("java/util/AbstractSet", "<init> ");
      FILTERS.put("java/util/ArrayList", "");
      FILTERS.put("java/util/HashSet", "<init> add ");
      FILTERS.put("java/util/Hashtable", "<init> containsKey get ");
      FILTERS.put("java/util/HashMap", "");
      FILTERS.put("java/util/Properties", "<init> ");
      FILTERS.put("java/util/jar/JarEntry", "<init> ");
      FILTERS.put("java/util/logging/Logger", "<init> getName ");
   }

   private MockingFilters() {}

   @Nullable
   public static String filtersForClass(@Nonnull String classDesc) { return FILTERS.get(classDesc); }

   public static boolean isUnmockable(@Nonnull String classDesc)
   {
      return
         classDesc.startsWith("java/") && (
            "java/lang/ClassLoader java/lang/Math java/lang/StrictMath java/time/Duration".contains(classDesc) ||
            "java/nio/file/Paths".equals(classDesc)
         );
   }

   public static boolean isFullMockingDisallowed(@Nonnull String classDesc)
   {
      return classDesc.startsWith("java/io/") && (
         "java/io/FileOutputStream".equals(classDesc) || "java/io/FileInputStream".equals(classDesc) ||
         "java/io/FileWriter".equals(classDesc) ||
         "java/io/PrintWriter java/io/Writer java/io/DataInputStream".contains(classDesc)
      );
   }

   public static boolean isUnmockable(@Nonnull String owner, @Nonnull String name)
   {
      String mockingFilters = FILTERS.get(owner);
      return isUnmockableInvocation(mockingFilters, name);
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
}
