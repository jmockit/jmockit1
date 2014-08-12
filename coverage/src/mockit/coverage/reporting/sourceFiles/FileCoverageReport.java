/*
 * Copyright (c) 2006-2014 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.coverage.reporting.sourceFiles;

import java.io.*;
import java.util.*;

import org.jetbrains.annotations.*;

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
   @NotNull private final InputFile inputFile;
   @NotNull private final OutputFile output;
   @NotNull private final FileParser fileParser;
   @NotNull private final NeutralOutput neutralOutput;
   @NotNull private final LineCoverageOutput lineCoverage;
   @Nullable private final PathCoverageOutput pathCoverage;
   @Nullable private final DataCoverageOutput dataCoverage;

   public FileCoverageReport(
      @NotNull String outputDir, @NotNull InputFile inputFile, @NotNull FileCoverageData fileData,
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
   private PathCoverageOutput createPathCoverageOutput(@NotNull FileCoverageData fileData)
   {
      if (Metrics.PathCoverage.active) {
         Collection<MethodCoverageData> methods = fileData.getMethods();
         return methods.isEmpty() ? null : new PathCoverageOutput(output, methods);
      }

      return null;
   }

   @Nullable
   private static DataCoverageOutput createDataCoverageOutput(@NotNull FileCoverageData fileData)
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

   private void writeLineWithoutCoverageInfo(@NotNull LineElement initialElement)
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
