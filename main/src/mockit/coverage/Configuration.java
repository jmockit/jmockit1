/*
 * Copyright (c) 2006 JMockit developers
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.coverage;

import javax.annotation.*;

public final class Configuration
{
   private static final String COVERAGE_PREFIX = "coverage-";

   private Configuration() {}

   @Nullable
   public static String getProperty(@Nonnull String nameSuffix) {
      return getProperty(nameSuffix, null);
   }

   public static String getProperty(@Nonnull String nameSuffix, @Nullable String defaultValue) {
      return System.getProperty(COVERAGE_PREFIX + nameSuffix, defaultValue);
   }

   @Nullable
   public static String getOrChooseOutputDirectory(@Nonnull String outputDir) {
      if (!outputDir.isEmpty()) {
         return outputDir;
      }

      String mavenBaseDir = System.getProperty("basedir");
      return mavenBaseDir == null ? null : "target";
   }

   @Nonnull
   public static String getOrChooseOutputDirectory(@Nonnull String outputDir, @Nonnull String defaultDir) {
      if (!outputDir.isEmpty()) {
         return outputDir;
      }

      String mavenBaseDir = System.getProperty("basedir");
      return mavenBaseDir == null ? defaultDir : "target/" + defaultDir;
   }
}
