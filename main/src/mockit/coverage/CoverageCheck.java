/*
 * Copyright (c) 2006 JMockit developers
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.coverage;

import java.io.*;
import java.util.*;
import java.util.regex.*;
import javax.annotation.*;

import mockit.coverage.data.*;

final class CoverageCheck
{
   private static final class Threshold {
      private static final Pattern PARAMETER_SEPARATORS = Pattern.compile(":|=");

      @Nullable private final String sourceFilePrefix;
      @Nonnull private final String scopeDescription;
      @Nonnegative private int minPercentage;

      Threshold(@Nonnull String configurationParameter) {
         String[] sourceFilePrefixAndMinPercentage = PARAMETER_SEPARATORS.split(configurationParameter);
         String textualPercentage;

         if (sourceFilePrefixAndMinPercentage.length == 1) {
            sourceFilePrefix = null;
            scopeDescription = "";
            textualPercentage = sourceFilePrefixAndMinPercentage[0];
         }
         else {
            String scope = sourceFilePrefixAndMinPercentage[0].trim();

            if (isPerFile(scope)) {
               sourceFilePrefix = scope;
               scopeDescription = " for some source files";
            }
            else {
               sourceFilePrefix = scope.replace('.', '/');
               scopeDescription = " for " + scope;
            }

            textualPercentage = sourceFilePrefixAndMinPercentage[1];
         }

         try { minPercentage = Integer.parseInt(textualPercentage.trim()); } catch (NumberFormatException ignore) {}
      }

      private static boolean isPerFile(@Nullable String scope) { return "perFile".equalsIgnoreCase(scope); }

      boolean verifyMinimum() {
         CoverageData coverageData = CoverageData.instance();
         int percentage;

         if (isPerFile(sourceFilePrefix)) {
            percentage = coverageData.getSmallestPerFilePercentage();
         }
         else {
            percentage = coverageData.getPercentage(sourceFilePrefix);
         }

         return percentage < 0 || verifyMinimum(percentage);
      }

      private boolean verifyMinimum(int percentage) {
         if (percentage < minPercentage) {
            System.out.println("JMockit: coverage too low" + scopeDescription + ": " + percentage + "% < " + minPercentage + '%');
            return false;
         }

         return true;
      }
   }

   @Nullable private final List<Threshold> thresholds;
   private boolean allThresholdsSatisfied;

   CoverageCheck() {
      String configuration = Configuration.getProperty("check", "");

      if (configuration.isEmpty()) {
         thresholds = null;
         return;
      }

      String[] configurationParameters = configuration.split(";");
      int n = configurationParameters.length;
      thresholds = new ArrayList<>(n);

      for (String configurationParameter : configurationParameters) {
         thresholds.add(new Threshold(configurationParameter));
      }
   }

   void verifyThresholds() {
      if (thresholds == null) return;
      allThresholdsSatisfied = true;

      for (Threshold threshold : thresholds) {
         allThresholdsSatisfied &= threshold.verifyMinimum();
      }

      createOrDeleteIndicatorFile();

      if (!allThresholdsSatisfied) {
         throw new AssertionError("JMockit: minimum coverage percentages not reached; see previous messages.");
      }
   }

   @SuppressWarnings("ResultOfMethodCallIgnored")
   private void createOrDeleteIndicatorFile() {
      String parentDir = Configuration.getOrChooseOutputDirectory("");
      File indicatorFile = new File(parentDir, "coverage.check.failed");

      if (indicatorFile.exists()) {
         if (allThresholdsSatisfied) {
            indicatorFile.delete();
         }
         else {
            indicatorFile.setLastModified(System.currentTimeMillis());
         }
      }
      else if (!allThresholdsSatisfied) {
         try { indicatorFile.createNewFile(); } catch (IOException e) { throw new RuntimeException(e); }
      }
   }
}
