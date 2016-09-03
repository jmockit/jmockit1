/*
 * Copyright (c) 2006 Rogério Liesenfeld
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
 * The XML schema used is the one <a href="http://docs.sonarqube.org/display/PLUG/Generic+Test+Coverage">defined</a> by
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
   @Nonnull private Writer output;

   XmlFile(@Nonnull String outputDir, @Nonnull CoverageData coverageData)
   {
      String firstSrcDir = Configuration.getProperty("srcDirs", "").split("\\s*,\\s*")[0];
      srcDir = firstSrcDir.isEmpty() ? "" : firstSrcDir + '/';

      String parentDir = Configuration.getOrChooseOutputDirectory(outputDir);
      outputFile = new File(parentDir, "coverage.xml");
      this.coverageData = coverageData;
   }

   void generate() throws IOException
   {
      output = new FileWriter(outputFile);

      try {
         output.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
         output.write("<coverage version=\"1\">\n");

         for (Entry<String, FileCoverageData> fileAndData : coverageData.getFileToFileDataMap().entrySet()) {
            String sourceFileName = fileAndData.getKey();
            writeOpeningXmlElementForSourceFile(sourceFileName);

            PerFileLineCoverage lineInfo = fileAndData.getValue().lineCoverageInfo;
            writeXmlElementsForExecutableLines(lineInfo);

            output.write("\t</file>\n");
         }

         output.write("</coverage>\n");
      }
      finally {
         output.close();
      }

      System.out.println("JMockit: Coverage data written to " + outputFile.getCanonicalPath());
   }

   private void writeOpeningXmlElementForSourceFile(@Nonnull String sourceFileName) throws IOException
   {
      output.write("\t<file path=\"");
      output.write(srcDir);
      output.write(sourceFileName);
      output.write("\">\n");
   }

   private void writeXmlElementsForExecutableLines(@Nonnull PerFileLineCoverage lineInfo) throws IOException
   {
      int lineCount = lineInfo.getLineCount();

      for (int lineNum = 1; lineNum <= lineCount; lineNum++) {
         if (lineInfo.hasLineData(lineNum)) {
            LineCoverageData lineData = lineInfo.getLineData(lineNum);

            output.write("\t\t<lineToCover lineNumber=\"");
            writeNumber(lineNum);
            output.write("\" covered=\"");
            output.write(Boolean.toString(lineData.isCovered()));

            if (lineData.containsBranches()) {
               output.write("\" branchesToCover=\"");
               writeNumber(lineData.getNumberOfBranchingSourcesAndTargets());
               output.write("\" coveredBranches=\"");
               writeNumber(lineData.getNumberOfCoveredBranchingSourcesAndTargets());
            }

            output.write("\"/>\n");
         }
      }
   }

   private void writeNumber(int value) throws IOException { output.write(Integer.toString(value)); }
}
