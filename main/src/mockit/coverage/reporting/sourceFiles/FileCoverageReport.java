/*
 * Copyright (c) 2006 JMockit developers
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.coverage.reporting.sourceFiles;

import java.io.*;
import javax.annotation.*;

import mockit.coverage.data.*;
import mockit.coverage.dataItems.*;
import mockit.coverage.reporting.*;
import mockit.coverage.reporting.dataCoverage.*;
import mockit.coverage.reporting.lineCoverage.*;
import mockit.coverage.reporting.parsing.*;

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
   @Nullable private final DataCoverageOutput dataCoverage;

   public FileCoverageReport(
      @Nonnull String outputDir, @Nonnull InputFile inputFile, @Nonnull FileCoverageData fileData, boolean withCallPoints
   ) throws IOException {
      this.inputFile = inputFile;
      output = new OutputFile(outputDir, inputFile.filePath);
      fileParser = new FileParser();
      neutralOutput = new NeutralOutput(output);
      lineCoverage = new LineCoverageOutput(output, fileData.getLineCoverageData(), withCallPoints);
      dataCoverage = createDataCoverageOutput(fileData);
   }

   @Nullable
   private static DataCoverageOutput createDataCoverageOutput(@Nonnull FileCoverageData fileData) {
      PerFileDataCoverage dataCoverageInfo = fileData.dataCoverageInfo;
      return dataCoverageInfo.hasFields() ? new DataCoverageOutput(dataCoverageInfo) : null;
   }

   public void generate() throws IOException {
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

   private void writeHeader() {
      output.writeCommonHeader(inputFile.getSourceFileName());
      output.println("  <table>");
      output.println("    <caption>" + inputFile.getSourceFilePath() + "</caption>");
   }

   private void writeFormattedSourceLines() throws IOException {
      LineParser lineParser = fileParser.lineParser;
      String line;

      while ((line = inputFile.nextLine()) != null) {
         boolean lineWithCodeElements = fileParser.parseCurrentLine(line);

         if (lineWithCodeElements) {
            if (dataCoverage != null) {
               dataCoverage.writeCoverageInfoIfLineStartsANewFieldDeclaration(fileParser);
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

   private void writeOpeningOfNewLine(@Nonnegative int lineNumber) {
      output.println("    <tr>");
      output.write("      <td>");
      output.print(lineNumber);
      output.write("</td>");
   }

   private void writeLineWithoutCoverageInfo(@Nonnull LineElement initialElement) {
      output.println("<td></td>");
      output.write("      <td><pre class='");
      output.write(initialElement.isComment() ? "cm'>" : "pp'>");
      output.write(initialElement.toString());
      output.println("</pre></td>");
   }

   private void writeFooter() {
      output.println("  </table>");
      output.writeCommonFooter();
   }
}