/*
 * Copyright (c) 2006 JMockit developers
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.coverage;

import java.io.*;
import javax.annotation.*;

import mockit.coverage.data.*;
import mockit.coverage.reporting.*;

@SuppressWarnings("DynamicRegexReplaceableByCompiledPattern")
final class OutputFileGenerator
{
   private static final String[] ALL_SOURCE_DIRS = new String[0];

   @Nonnull private final String[] outputFormats;
   @Nonnull private final String outputDir;
   @Nullable private final String[] sourceDirs;

   OutputFileGenerator() {
      outputFormats = getOutputFormat();
      outputDir = Configuration.getProperty("outputDir", "");

      String commaSeparatedDirs = Configuration.getProperty("srcDirs");

      if (commaSeparatedDirs == null) {
         sourceDirs = ALL_SOURCE_DIRS;
      }
      else if (commaSeparatedDirs.isEmpty()) {
         sourceDirs = null;
      }
      else {
         sourceDirs = commaSeparatedDirs.split("\\s*,\\s*");
      }
   }

   @Nonnull
   private static String[] getOutputFormat() {
      String format = Configuration.getProperty("output", "");
      return format.isEmpty() ? new String[] {"html"} : format.trim().split("\\s*,\\s*|\\s+");
   }

   boolean isOutputToBeGenerated() {
      return isHTMLWithNoCallPoints() || isWithCallPoints() ||
             hasOutputFormat("serial") || hasOutputFormat("serial-append") || hasOutputFormat("xml");
   }

   private boolean isHTMLWithNoCallPoints() { return hasOutputFormat("html") || hasOutputFormat("html-nocp"); }
   boolean isWithCallPoints()               { return hasOutputFormat("html-cp"); }

   private boolean hasOutputFormat(@Nonnull String format) {
      for (String outputFormat : outputFormats) {
         if (format.equals(outputFormat)) {
            return true;
         }
      }

      return false;
   }

   void generate() {
      CoverageData coverageData = CoverageData.instance();

      if (coverageData.isEmpty()) {
         System.out.print("JMockit: No classes were instrumented for coverage; please make sure that ");

         String classesRegexp = Configuration.getProperty("classes");

         if (classesRegexp == null) {
            System.out.print("classes exercised by tests are in a directory included in the runtime classpath");
         }
         else {
            System.out.print(
               "classes selected for coverage through the regular expression \"" + classesRegexp +
               "\" are available from the runtime classpath");
         }

         System.out.println(", and that they have been compiled with debug information.");
         return;
      }

      boolean outputDirCreated = createOutputDirIfSpecifiedButNotExists();

      try {
         generateAccretionDataFileIfRequested(coverageData);
         generateXmlDataFileIfRequested(coverageData);
         generateHTMLReportIfRequested(coverageData, outputDirCreated);
      }
      catch (IOException e) {
         throw new RuntimeException(e);
      }
   }

   void generateAggregateReportFromInputFiles(@Nonnull String[] inputPaths) {
      boolean outputDirCreated = createOutputDirIfSpecifiedButNotExists();

      try {
         CoverageData coverageData = new DataFileMerging(inputPaths).merge();
         generateHTMLReportIfRequested(coverageData, outputDirCreated);
      }
      catch (IOException e) {
         throw new RuntimeException(e);
      }
   }

   private boolean createOutputDirIfSpecifiedButNotExists() {
      if (outputDir.isEmpty()) {
         return false;
      }

      File outDir = new File(outputDir);
      boolean dirCreated = outDir.mkdirs();
      return dirCreated;
   }

   private void generateAccretionDataFileIfRequested(@Nonnull CoverageData newData) throws IOException {
      if (hasOutputFormat("serial")) {
         new AccretionFile(outputDir, newData).generate();
      }
      else if (hasOutputFormat("serial-append")) {
         AccretionFile accretionFile = new AccretionFile(outputDir, newData);
         accretionFile.mergeDataFromExistingFileIfAny();
         accretionFile.generate();
      }
   }

   private void generateXmlDataFileIfRequested(@Nonnull CoverageData newData) throws IOException {
      if (hasOutputFormat("xml")) {
         new XmlFile(outputDir, newData).generate();
      }
   }

   private void generateHTMLReportIfRequested(@Nonnull CoverageData coverageData, boolean outputDirCreated) throws IOException {
      if (isHTMLWithNoCallPoints()) {
         new CoverageReport(outputDir, outputDirCreated, sourceDirs, coverageData, false).generate();
      }
      else if (isWithCallPoints()) {
         new CoverageReport(outputDir, outputDirCreated, sourceDirs, coverageData, true).generate();
      }
   }
}