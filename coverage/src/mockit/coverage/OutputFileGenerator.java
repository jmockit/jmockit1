/*
 * Copyright (c) 2006-2015 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.coverage;

import java.io.File;
import java.io.IOException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import mockit.coverage.data.CoverageData;
import mockit.coverage.modification.ClassModification;
import mockit.coverage.modification.ClassesNotLoaded;
import mockit.coverage.reporting.BasicCoverageReport;
import mockit.coverage.reporting.FullCoverageReport;
import mockit.coverage.reporting.XmlCoverageReport;
import mockit.coverage.standalone.Startup;

final class OutputFileGenerator
{
   private static final String[] ALL_SOURCE_DIRS = new String[0];

   @Nullable private final ClassModification classModification;
   @Nonnull private final String[] outputFormats;
   @Nonnull private final String outputDir;
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
         sourceDirs = commaSeparatedDirs.split("\\s*,\\s*");
      }
   }

   @Nonnull
   private static String[] getOutputFormat()
   {
      String format = Configuration.getProperty("output", "");
      return format.isEmpty() ? new String[] {"html-nocp"} : format.trim().split("\\s*,\\s*|\\s+");
   }

   boolean isOutputToBeGenerated()
   {
      return isOutputWithCallPointsToBeGenerated() || hasOutputFormat("html-nocp") || hasOutputFormat("sonarxml");
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

   private boolean hasOutputFormat(@Nonnull String format)
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
         generateHTMLReportIfRequested(coverageData, outputDirCreated);
         generateSonarXmlReportIfRequested(coverageData);
      }
      catch (IOException e) {
         throw new RuntimeException(e);
      }
   }

   // http://docs.sonarqube.org/display/SONAR/Generic+Test+Coverage
   private void generateSonarXmlReportIfRequested(CoverageData coverageData) throws IOException 
   {
	    if (hasOutputFormat("sonarxml")) {
			new XmlCoverageReport(outputDir, coverageData).generate();
	    }
   }

   void generateAggregateReportFromInputFiles(@Nonnull String[] inputPaths)
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

   private void generateAccretionDataFileIfRequested(@Nonnull CoverageData newData) throws IOException
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

   private void generateHTMLReportIfRequested(@Nonnull CoverageData coverageData, boolean outputDirCreated)
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
