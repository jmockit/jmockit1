/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.coverage.reporting.sourceFiles;

import java.io.*;
import java.util.*;
import javax.annotation.*;

import mockit.coverage.*;
import mockit.coverage.data.*;
import mockit.coverage.dataItems.*;
import mockit.coverage.paths.*;
import mockit.coverage.reporting.*;
import mockit.coverage.reporting.dataCoverage.*;
import mockit.coverage.reporting.lineCoverage.*;
import mockit.coverage.reporting.parsing.*;
import mockit.coverage.reporting.pathCoverage.*;

/**
 * Generates an HTML page containing line-by-line coverage information for a single source file.
 */
public final class FileCoverageReport
{
   @Nonnull private final InputFile inputFile;
   @Nonnull private final OutputFile output;
   @Nonnull private final FileParser fileParser;
   @Nonnull private final NeutralOutput neutralOutput;
   @Nonnull private final LineCoverageOutput lineCoverage;
   @Nullable private final PathCoverageOutput pathCoverage;
   @Nullable private final DataCoverageOutput dataCoverage;

   public FileCoverageReport(
      @Nonnull String outputDir, @Nonnull InputFile inputFile, @Nonnull FileCoverageData fileData,
      boolean withCallPoints)
      throws IOException
   {
      this.inputFile = inputFile;
      output = new OutputFile(outputDir, inputFile.filePath);
      fileParser = new FileParser();
      neutralOutput = new NeutralOutput(output);
      lineCoverage = new LineCoverageOutput(output, fileData.getLineCoverageData(), withCallPoints);
      pathCoverage = createPathCoverageOutput(fileData);
      dataCoverage = createDataCoverageOutput(fileData);
   }

   @Nullable
   private PathCoverageOutput createPathCoverageOutput(@Nonnull FileCoverageData fileData)
   {
      if (Metrics.PathCoverage.active) {
         Collection<MethodCoverageData> methods = fileData.getMethods();
         return methods.isEmpty() ? null : new PathCoverageOutput(output, methods);
      }

      return null;
   }

   @Nullable
   private static DataCoverageOutput createDataCoverageOutput(@Nonnull FileCoverageData fileData)
   {
      if (Metrics.DataCoverage.active) {
         PerFileDataCoverage dataCoverageInfo = fileData.dataCoverageInfo;
         return dataCoverageInfo.hasFields() ? new DataCoverageOutput(dataCoverageInfo) : null;
      }

      return null;
   }

   public void generate() throws IOException
   {
      try {
         writeHeader();
         writeFormattedSourceLines();
         writeFooter();
      }
      finally {
         inputFile.close();
         output.close();
      }
   }

   private void writeHeader()
   {
      output.writeCommonHeader(inputFile.getSourceFileName());
      output.println("  <table cellpadding='0' cellspacing='1'>");
      output.println("    <caption>" + inputFile.getSourceFilePath() + "</caption>");
   }

   private void writeFormattedSourceLines() throws IOException
   {
      LineParser lineParser = fileParser.lineParser;
      String line;

      while ((line = inputFile.nextLine()) != null) {
         boolean lineWithCodeElements = fileParser.parseCurrentLine(line);

         if (lineWithCodeElements) {
            if (dataCoverage != null) {
               dataCoverage.writeCoverageInfoIfLineStartsANewFieldDeclaration(fileParser);
            }

            if (pathCoverage != null) {
               pathCoverage.writePathCoverageInfoIfLineStartsANewMethodOrConstructor(lineParser.getNumber());
            }
         }

         if (!neutralOutput.writeLineWithoutCoverageInfo(lineParser)) {
            writeOpeningOfNewLine(lineParser.getNumber());

            if (!lineCoverage.writeLineWithCoverageInfo(lineParser)) {
               writeLineWithoutCoverageInfo(lineParser.getInitialElement());
            }

            output.println("    </tr>");
         }
      }
   }

   private void writeOpeningOfNewLine(int lineNumber)
   {
      output.println("    <tr>");
      output.write("      <td class='line'>");
      output.print(lineNumber);
      output.write("</td>");
   }

   private void writeLineWithoutCoverageInfo(@Nonnull LineElement initialElement)
   {
      output.println("<td>&nbsp;</td>");
      output.write("      <td><pre class='");
      output.write(initialElement.isComment() ? "comment'>" : "prettyprint'>");
      output.write(initialElement.toString());
      output.println("</pre></td>");
   }

   private void writeFooter()
   {
      output.println("  </table>");
      output.writeCommonFooter();
   }
}
