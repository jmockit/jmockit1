/*
 * Copyright (c) 2006-2013 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.coverage.reporting.lineCoverage;

import java.io.*;

import org.jetbrains.annotations.*;

import mockit.coverage.lines.*;
import mockit.coverage.reporting.parsing.*;

public final class LineCoverageOutput
{
   @NotNull private final PrintWriter output;
   @NotNull private final PerFileLineCoverage lineCoverageData;
   @NotNull private final LineCoverageFormatter lineCoverageFormatter;

   public LineCoverageOutput(
      @NotNull PrintWriter output, @NotNull PerFileLineCoverage lineCoverageData, boolean withCallPoints)
   {
      this.output = output;
      this.lineCoverageData = lineCoverageData;
      lineCoverageFormatter = new LineCoverageFormatter(withCallPoints);
   }

   public boolean writeLineWithCoverageInfo(@NotNull LineParser lineParser)
   {
      int line = lineParser.getNumber();

      if (!lineCoverageData.hasLineData(line)) {
         return false;
      }

      int lineExecutionCount = lineCoverageData.getExecutionCount(line);

      if (lineExecutionCount < 0) {
         return false;
      }

      writeLineExecutionCount(lineExecutionCount);
      writeExecutableCode(lineParser);
      return true;
   }

   private void writeLineExecutionCount(int lineExecutionCount)
   {
      output.write("<td class='count'>");
      output.print(lineExecutionCount);
      output.println("</td>");
   }

   private void writeExecutableCode(@NotNull LineParser lineParser)
   {
      String formattedLine = lineCoverageFormatter.format(lineParser, lineCoverageData);
      output.write("      <td>");
      output.write(formattedLine);
      output.println("</td>");
   }
}
