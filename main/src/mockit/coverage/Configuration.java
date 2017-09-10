/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.coverage;

import javax.annotation.*;

public final class Configuration
{
   private static final String COVERAGE_PREFIX1 = "jmockit-coverage-";
   private static final String COVERAGE_PREFIX2 = "coverage-";

   private Configuration() {}

   @Nullable
   public static String getProperty(@Nonnull String nameSuffix)
   {
      return getProperty(nameSuffix, null);
   }

   public static String getProperty(@Nonnull String nameSuffix, @Nullable String defaultValue)
   {
      String property = System.getProperty(COVERAGE_PREFIX1 + nameSuffix);

      if (property != null) {
         return property;
      }

      return System.getProperty(COVERAGE_PREFIX2 + nameSuffix, defaultValue);
   }

   @Nullable
   public static String getOrChooseOutputDirectory(@Nonnull String outputDir)
   {
      if (!outputDir.isEmpty()) {
         return outputDir;
      }

      String mavenBaseDir = System.getProperty("basedir");
      return mavenBaseDir == null ? null : "target";
   }

   @Nonnull
   public static String getOrChooseOutputDirectory(@Nonnull String outputDir, @Nonnull String defaultDir)
   {
      if (!outputDir.isEmpty()) {
         return outputDir;
      }

      String mavenBaseDir = System.getProperty("basedir");
      return mavenBaseDir == null ? defaultDir : "target/" + defaultDir;
   }
}
