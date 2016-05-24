/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.coverage.reporting.lineCoverage;

import java.io.*;
import javax.annotation.*;

import mockit.coverage.lines.*;
import mockit.coverage.reporting.parsing.*;

public final class LineCoverageOutput
{
   @Nonnull private final PrintWriter output;
   @Nonnull private final PerFileLineCoverage lineCoverageData;
   @Nonnull private final LineCoverageFormatter lineCoverageFormatter;

   public LineCoverageOutput(
      @Nonnull PrintWriter output, @Nonnull PerFileLineCoverage lineCoverageData, boolean withCallPoints)
   {
      this.output = output;
      this.lineCoverageData = lineCoverageData;
      lineCoverageFormatter = new LineCoverageFormatter(withCallPoints);
   }

   public boolean writeLineWithCoverageInfo(@Nonnull LineParser lineParser)
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

   private void writeExecutableCode(@Nonnull LineParser lineParser)
   {
      String formattedLine = lineCoverageFormatter.format(lineParser, lineCoverageData);
      output.write("      <td>");
      output.write(formattedLine);
      output.println("</td>");
   }
}
