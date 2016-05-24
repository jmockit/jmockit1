/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.coverage.reporting.packages;

import java.io.*;
import java.util.*;
import javax.annotation.*;

import mockit.coverage.*;
import mockit.coverage.data.*;

final class PackageCoverageReport extends ListWithFilesAndPercentages
{
   @Nonnull private final Map<String, FileCoverageData> filesToFileData;
   @Nullable private final Collection<String> sourceFilesNotFound;
   @Nonnull private final char[] fileNameWithSpaces;

   PackageCoverageReport(
      @Nonnull PrintWriter output, @Nullable Collection<String> sourceFilesNotFound,
      @Nonnull Map<String, FileCoverageData> filesToFileData, @Nonnull Collection<List<String>> allSourceFileNames)
   {
      super(output, "          ");
      this.sourceFilesNotFound = sourceFilesNotFound;
      this.filesToFileData = filesToFileData;
      fileNameWithSpaces = new char[maximumSourceFileNameLength(allSourceFileNames)];
   }

   private static int maximumSourceFileNameLength(@Nonnull Collection<List<String>> allSourceFileNames)
   {
      int maxLength = 0;

      for (List<String> files : allSourceFileNames) {
         for (String fileName : files) {
            int n = fileName.length();

            if (n > maxLength) {
               maxLength = n;
            }
         }
      }

      return maxLength;
   }

   @Override
   protected void writeMetricsForFile(@Nullable String packageName, @Nonnull String fileName)
   {
      String filePath = packageName == null || packageName.isEmpty() ? fileName : packageName + '/' + fileName;
      final FileCoverageData fileData = filesToFileData.get(filePath);

      writeRowStart();
      printIndent();
      output.write("  <td class='file ");
      output.write(fileData.kindOfTopLevelType != null ? fileData.kindOfTopLevelType : "class");
      output.write("'>");

      int fileNameLength = buildFileNameWithTrailingSpaces(fileName);
      writeTableCellWithFileName(filePath, fileNameLength);

      Metrics.performAction(new Metrics.Action() {
         @Override
         public void perform(@Nonnull Metrics metric)
         {
            writeCodeCoverageMetricForFile(metric, fileData.getPerFileCoverage(metric));
         }
      });

      writeRowClose();
   }

   private int buildFileNameWithTrailingSpaces(@Nonnull String fileName)
   {
      int n = fileName.length();

      fileName.getChars(0, n, fileNameWithSpaces, 0);
      Arrays.fill(fileNameWithSpaces, n, fileNameWithSpaces.length, ' ');
      
      return n;
   }

   private void writeTableCellWithFileName(@Nonnull String filePath, int fileNameLen)
   {
      if (sourceFilesNotFound == null || sourceFilesNotFound.contains(filePath)) {
         output.write(fileNameWithSpaces);
      }
      else {
         output.write("<a target='_blank' href='");
         int p = filePath.lastIndexOf('.');
         output.write(filePath.substring(0, p));
         output.write(".html'>");
         output.write(fileNameWithSpaces, 0, fileNameLen);
         output.write("</a>");
         output.write(fileNameWithSpaces, fileNameLen, fileNameWithSpaces.length - fileNameLen);
      }

      output.println("</td>");
   }

   private void writeCodeCoverageMetricForFile(@Nonnull Metrics metric, @Nonnull PerFileCoverage coverageInfo)
   {
      int percentage = coverageInfo.getCoveragePercentage();
      int covered = coverageInfo.getCoveredItems();
      int total = coverageInfo.getTotalItems();

      coveredItems[metric.ordinal()] += covered;
      totalItems[metric.ordinal()] += total;

      printCoveragePercentage(metric, covered, total, percentage);
   }

   @Override
   protected void writeClassAttributeForCoveragePercentageCell() {}
}