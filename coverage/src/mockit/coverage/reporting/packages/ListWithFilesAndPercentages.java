/*
 * Copyright (c) 2006-2014 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.coverage.reporting.packages;

import java.io.*;
import java.util.*;

import org.jetbrains.annotations.*;

import mockit.coverage.*;

abstract class ListWithFilesAndPercentages
{
   @NotNull protected final PrintWriter output;
   @NotNull private final String baseIndent;
   @NotNull final int[] totalItems = new int[Metrics.values().length];
   @NotNull final int[] coveredItems = new int[Metrics.values().length];

   protected ListWithFilesAndPercentages(@NotNull PrintWriter output, @NotNull String baseIndent)
   {
      this.output = output;
      this.baseIndent = baseIndent;
   }

   final void writeMetricsForEachFile(@Nullable String packageName, @NotNull List<String> fileNames)
   {
      if (fileNames.isEmpty()) {
         return;
      }

      Collections.sort(fileNames);
      Arrays.fill(totalItems, 0);
      Arrays.fill(coveredItems, 0);

      for (String fileName : fileNames) {
         writeMetricsForFile(packageName, fileName);
      }
   }

   protected final void writeRowStart()
   {
      printIndent();
      output.println("<tr>");
   }

   protected final void writeRowClose()
   {
      printIndent();
      output.println("</tr>");
   }

   final void printIndent() { output.write(baseIndent); }

   protected abstract void writeMetricsForFile(@Nullable String packageName, @NotNull String fileName);

   final void printCoveragePercentage(@NotNull Metrics metric, int covered, int total, int percentage)
   {
      printIndent();
      output.write("  <td ");

      if (total > 0) {
         writeRowCellWithCoveragePercentage(metric, covered, total, percentage);
      }
      else {
         output.write("class='nocode'>N/A");
      }

      output.println("</td>");
   }

   private void writeRowCellWithCoveragePercentage(@NotNull Metrics metric, int covered, int total, int percentage)
   {
      writeClassAttributeForCoveragePercentageCell();
      output.write("style='background-color:#");
      output.write(CoveragePercentage.percentageColor(covered, total));
      output.write("' title='");
      output.write(metric.itemName);
      output.write(": ");
      output.print(covered);
      output.write('/');
      output.print(total);
      output.write("'>");
      writePercentageValue(covered, total, percentage);
      output.print("%");
   }

   protected abstract void writeClassAttributeForCoveragePercentageCell();

   private void writePercentageValue(int covered, int total, int percentage)
   {
      if (percentage < 100) {
         output.print(percentage);
      }
      else if (covered == total) {
         output.print("100");
      }
      else {
         output.print(">99");
      }
   }
}