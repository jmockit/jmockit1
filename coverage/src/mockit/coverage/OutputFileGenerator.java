/*
 * Copyright (c) 2006-2014 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.coverage;

import java.io.*;

import org.jetbrains.annotations.*;

import mockit.coverage.data.*;
import mockit.coverage.modification.*;
import mockit.coverage.reporting.*;
import mockit.coverage.standalone.*;

final class OutputFileGenerator
{
   private static final String[] ALL_SOURCE_DIRS = new String[0];

   @Nullable private final ClassModification classModification;
   @NotNull private final String[] outputFormats;
   @NotNull private final String outputDir;
   @Nullable private final String[] sourceDirs;

   OutputFileGenerator(@Nullable ClassModification classModification)
   {
      this.classModification = classModification;
      outputFormats = getOutputFormat();
      outputDir = Configuration.getProperty("outputDir", "");

      String commaSeparatedDirs = Configuration.getProperty("srcDirs");

      if (commaSeparatedDirs == null) {
         sourceDirs = Startup.isTestRun() ? ALL_SOURCE_DIRS : null;
      }
      else if (commaSeparatedDirs.isEmpty()) {
         sourceDirs = null;
      }
      else {
         sourceDirs = commaSeparatedDirs.split(",");
      }
   }

   @NotNull private String[] getOutputFormat()
   {
      String format = Configuration.getProperty("output", "");
      return format.isEmpty() ? new String[] {"html-nocp"} : format.trim().split("\\s*,\\s*|\\s+");
   }

   boolean isOutputToBeGenerated()
   {
      return isOutputWithCallPointsToBeGenerated() || hasOutputFormat("html-nocp");
   }

   private boolean isOutputWithCallPointsToBeGenerated()
   {
      return hasOutputFormat("html") || hasOutputFormat("serial") || hasOutputFormat("serial-append");
   }

   boolean isWithCallPoints()
   {
      return
         Startup.isTestRun() && Startup.isJMockitAvailable() &&
         isOutputWithCallPointsToBeGenerated() && !hasOutputFormat("html-nocp");
   }

   private boolean hasOutputFormat(@NotNull String format)
   {
      for (String outputFormat : outputFormats) {
         if (format.equals(outputFormat)) {
            return true;
         }
      }

      return false;
   }

   void generate(@Nullable CodeCoverage codeCoverage)
   {
      if (classModification != null && classModification.shouldConsiderClassesNotLoaded()) {
         new ClassesNotLoaded(classModification).gatherCoverageData();
      }

      if (codeCoverage != null) {
         codeCoverage.deactivate();
      }

      CoverageData coverageData = CoverageData.instance();

      if (coverageData.isEmpty()) {
         System.out.println(
            "JMockit: No classes were instrumented for coverage; please make sure that classes selected for coverage " +
            "have been compiled with debug information.");
         return;
      }

      boolean outputDirCreated = createOutputDirIfSpecifiedButNotExists();

      try {
         generateAccretionDataFileIfRequested(coverageData);
         generateHTMLReportIfRequested(coverageData, outputDirCreated);
      }
      catch (IOException e) {
         throw new RuntimeException(e);
      }
   }

   void generateAggregateReportFromInputFiles(@NotNull String[] inputPaths)
   {
      boolean outputDirCreated = createOutputDirIfSpecifiedButNotExists();

      try {
         CoverageData coverageData = new DataFileMerging(inputPaths).merge();
         generateHTMLReportIfRequested(coverageData, outputDirCreated);
      }
      catch (IOException e) {
         throw new RuntimeException(e);
      }
   }

   private boolean createOutputDirIfSpecifiedButNotExists()
   {
      if (outputDir.isEmpty()) {
         return false;
      }

      File outDir = new File(outputDir);
      boolean dirCreated = outDir.mkdirs();
      return dirCreated;
   }

   private void generateAccretionDataFileIfRequested(@NotNull CoverageData newData) throws IOException
   {
      if (hasOutputFormat("serial")) {
         new AccretionFile(outputDir, newData).generate();
      }
      else if (hasOutputFormat("serial-append")) {
         AccretionFile accretionFile = new AccretionFile(outputDir, newData);
         accretionFile.mergeDataFromExistingFileIfAny();
         accretionFile.generate();
      }
   }

   private void generateHTMLReportIfRequested(@NotNull CoverageData coverageData, boolean outputDirCreated)
      throws IOException
   {
      if (hasOutputFormat("html-nocp")) {
         new BasicCoverageReport(outputDir, outputDirCreated, sourceDirs, coverageData).generate();
      }
      else if (hasOutputFormat("html")) {
         new FullCoverageReport(outputDir, outputDirCreated, sourceDirs, coverageData).generate();
      }
   }
}
