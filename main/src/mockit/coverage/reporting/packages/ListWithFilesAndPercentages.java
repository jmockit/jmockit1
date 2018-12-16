/*
 * Copyright (c) 2006 JMockit developers
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.coverage.reporting.packages;

import java.io.*;
import java.util.*;
import javax.annotation.*;

import mockit.coverage.*;

abstract class ListWithFilesAndPercentages
{
   @Nonnull protected final PrintWriter output;
   @Nonnull private final String baseIndent;
   @Nonnegative int totalItems;
   @Nonnegative int coveredItems;

   ListWithFilesAndPercentages(@Nonnull PrintWriter output, @Nonnull String baseIndent) {
      this.output = output;
      this.baseIndent = baseIndent;
   }

   final void writeMetricsForEachFile(@Nullable String packageName, @Nonnull List<String> fileNames) {
      if (fileNames.isEmpty()) {
         return;
      }

      Collections.sort(fileNames);
      totalItems = 0;
      coveredItems = 0;

      for (String fileName : fileNames) {
         writeMetricsForFile(packageName, fileName);
      }
   }

   final void writeRowStart() {
      printIndent();
      output.println("<tr>");
   }

   final void writeRowClose() {
      printIndent();
      output.println("</tr>");
   }

   final void printIndent() { output.write(baseIndent); }

   protected abstract void writeMetricsForFile(@Nullable String packageName, @Nonnull String fileName);

   final void printCoveragePercentage(@Nonnegative int covered, @Nonnegative int total, int percentage) {
      printIndent();
      output.write("  <td ");

      if (total > 0) {
         writeRowCellWithCoveragePercentage(covered, total, percentage);
      }
      else {
         output.write("class='nocode'>N/A");
      }

      output.println("</td>");
   }

   private void writeRowCellWithCoveragePercentage(@Nonnegative int covered, @Nonnegative int total, @Nonnegative int percentage) {
      writeClassAttributeForCoveragePercentageCell();
      output.write("style='background-color:#");
      output.write(CoveragePercentage.percentageColor(covered, total));
      output.write("' title='Items: ");
      output.print(covered);
      output.write('/');
      output.print(total);
      output.write("'>");
      writePercentageValue(covered, total, percentage);
      output.print("%");
   }

   protected abstract void writeClassAttributeForCoveragePercentageCell();

   private void writePercentageValue(@Nonnegative int covered, @Nonnegative int total, @Nonnegative int percentage) {
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
