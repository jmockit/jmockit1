/*
 * Copyright (c) 2006 JMockit developers
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.coverage;

import java.io.*;
import java.util.Map.*;
import javax.annotation.*;

import mockit.coverage.data.*;
import mockit.coverage.lines.*;

/**
 * Generates a XML file containing the coverage data gathered by the test run.
 * The XML schema used is the one <a href="http://docs.sonarqube.org/display/SONAR/Generic+Test+Data">defined</a> by
 * the SonarQube project:
 * <pre>
 * &lt;coverage version="1">
 *    &lt;file path="com/example/MyClass.java">
 *       &lt;lineToCover lineNumber="5" covered="false"/>
 *       &lt;lineToCover lineNumber="8" covered="true" branchesToCover="2" coveredBranches="1"/>
 *    &lt;/file>
 * &lt;/coverage>
 * </pre>
 */
final class XmlFile
{
   @Nonnull private final String srcDir;
   @Nonnull private final File outputFile;
   @Nonnull private final CoverageData coverageData;

   XmlFile(@Nonnull String outputDir, @Nonnull CoverageData coverageData) {
      //noinspection DynamicRegexReplaceableByCompiledPattern
      String firstSrcDir = Configuration.getProperty("srcDirs", "").split("\\s*,\\s*")[0];
      srcDir = firstSrcDir.isEmpty() ? "" : firstSrcDir + '/';

      String parentDir = Configuration.getOrChooseOutputDirectory(outputDir);
      outputFile = new File(parentDir, "coverage.xml");
      this.coverageData = coverageData;
   }

   void generate() throws IOException {
      try (Writer out = new FileWriter(outputFile)) {
         out.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
         out.write("<coverage version=\"1\">\n");

         for (Entry<String, FileCoverageData> fileAndData : coverageData.getFileToFileData().entrySet()) {
            String sourceFileName = fileAndData.getKey();
            writeOpeningXmlElementForSourceFile(out, sourceFileName);

            PerFileLineCoverage lineInfo = fileAndData.getValue().lineCoverageInfo;
            writeXmlElementsForExecutableLines(out, lineInfo);

            out.write("\t</file>\n");
         }

         out.write("</coverage>\n");
      }

      System.out.println("JMockit: Coverage data written to " + outputFile.getCanonicalPath());
   }

   private void writeOpeningXmlElementForSourceFile(@Nonnull Writer out, @Nonnull String sourceFileName) throws IOException {
      out.write("\t<file path=\"");
      out.write(srcDir);
      out.write(sourceFileName);
      out.write("\">\n");
   }

   private static void writeXmlElementsForExecutableLines(@Nonnull Writer out, @Nonnull PerFileLineCoverage lineInfo) throws IOException {
      int lineCount = lineInfo.getLineCount();

      for (int lineNum = 1; lineNum <= lineCount; lineNum++) {
         if (lineInfo.hasLineData(lineNum)) {
            LineCoverageData lineData = lineInfo.getLineData(lineNum);

            out.write("\t\t<lineToCover lineNumber=\"");
            writeNumber(out, lineNum);
            out.write("\" covered=\"");
            out.write(Boolean.toString(lineData.isCovered()));

            if (lineData.containsBranches()) {
               out.write("\" branchesToCover=\"");
               writeNumber(out, lineData.getNumberOfBranchingSourcesAndTargets());
               out.write("\" coveredBranches=\"");
               writeNumber(out, lineData.getNumberOfCoveredBranchingSourcesAndTargets());
            }

            out.write("\"/>\n");
         }
      }
   }

   private static void writeNumber(@Nonnull Writer out, @Nonnegative int value) throws IOException { out.write(Integer.toString(value)); }
}