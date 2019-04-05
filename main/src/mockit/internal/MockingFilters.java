/*
 * Copyright (c) 2006 JMockit developers
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal;

import java.util.*;
import javax.annotation.*;

import mockit.asm.types.*;

public final class MockingFilters
{
   private static final Map<String, String> FILTERS = new HashMap<>();
   static {
      FILTERS.put("java/lang/Object", "<init> clone getClass hashCode wait notify notifyAll ");
      FILTERS.put("java/io/File", "compareTo equals getName getPath hashCode toString ");
      FILTERS.put("java/util/logging/Logger", "<init> getName ");
      FILTERS.put("java/util/jar/JarEntry", "<init> ");
   }

   private MockingFilters() {}

   @Nullable
   public static String filtersForClass(@Nonnull String classDesc) { return FILTERS.get(classDesc); }

   public static void validateAsMockable(@Nonnull Class<?> type) {
      String typeDesc = JavaType.getInternalName(type);
      validateAsMockable(typeDesc);
   }

   public static void validateAsMockable(@Nonnull String typeDesc) {
      boolean unmockable =
         ("java/lang/String java/lang/AbstractStringBuilder java/util/Iterator java/util/Comparator java/util/Spliterator " +
          "java/util/Collection java/util/List java/util/Set java/util/SortedSet java/util/Queue java/util/Enumeration " +
          "java/util/Map java/util/SortedMap java/util/Map$Entry java/util/AbstractCollection java/util/AbstractMap " +
          "java/util/Hashtable java/lang/Throwable java/lang/Object java/lang/Enum java/lang/System java/lang/ThreadLocal " +
          "java/lang/ClassLoader java/lang/Math java/lang/StrictMath java/time/Duration").contains(typeDesc) ||
         "java/nio/file/Paths".equals(typeDesc) || typeDesc.startsWith("java/util/jar/");

      if (unmockable) {
         throw new IllegalArgumentException(typeDesc.replace('/', '.') + " is not mockable");
      }
   }

   public static boolean isFullMockingDisallowed(@Nonnull String classDesc) {
      return classDesc.startsWith("java/io/") && (
         "java/io/FileOutputStream".equals(classDesc) || "java/io/FileInputStream".equals(classDesc) ||
         "java/io/FileWriter".equals(classDesc) || "java/io/PrintWriter java/io/Writer java/io/DataInputStream".contains(classDesc)
      );
   }

   public static boolean isUnmockableInvocation(@Nullable String mockingFilters, @Nonnull String name) {
      if (mockingFilters == null) {
         return false;
      }

      if (mockingFilters.isEmpty()) {
         return true;
      }

      int i = mockingFilters.indexOf(name);
      return i > -1 && mockingFilters.charAt(i + name.length()) == ' ';
   }

   public static boolean isSubclassOfUnmockable(@Nonnull Class<?> aClass) {
      return
         Collection.class.isAssignableFrom(aClass) ||
         Map.class.isAssignableFrom(aClass) ||
         Iterator.class.isAssignableFrom(aClass) ||
         Comparator.class.isAssignableFrom(aClass) ||
         Enumeration.class.isAssignableFrom(aClass) ||
         Throwable.class.isAssignableFrom(aClass) ||
         ClassLoader.class.isAssignableFrom(aClass) ||
         ThreadLocal.class.isAssignableFrom(aClass) ||
         Number.class.isAssignableFrom(aClass);
   }
}
