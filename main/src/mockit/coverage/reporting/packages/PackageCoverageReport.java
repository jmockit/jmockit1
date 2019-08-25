/*
 * Copyright (c) 2006 JMockit developers
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.coverage.reporting.packages;

import java.io.*;
import java.util.*;
import javax.annotation.*;

import mockit.coverage.data.*;

final class PackageCoverageReport extends ListWithFilesAndPercentages
{
   @Nonnull private final Map<String, FileCoverageData> filesToFileData;
   @Nullable private final Collection<String> sourceFilesNotFound;
   @Nonnull private final char[] fileNameWithSpaces;

   PackageCoverageReport(
      @Nonnull PrintWriter output, @Nullable Collection<String> sourceFilesNotFound,
      @Nonnull Map<String, FileCoverageData> filesToFileData, @Nonnull Collection<List<String>> allSourceFileNames
   ) {
      super(output, "          ");
      this.sourceFilesNotFound = sourceFilesNotFound;
      this.filesToFileData = filesToFileData;
      fileNameWithSpaces = new char[maximumSourceFileNameLength(allSourceFileNames)];
   }

   @Nonnegative
   private static int maximumSourceFileNameLength(@Nonnull Collection<List<String>> allSourceFileNames) {
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
   protected void writeMetricsForFile(@Nullable String packageName, @Nonnull String fileName) {
      String filePath = packageName == null || packageName.isEmpty() ? fileName : packageName + '/' + fileName;
      FileCoverageData fileData = filesToFileData.get(filePath);

      writeRowStart();
      printIndent();
      output.write("  <td class='");
      output.write(fileData.kindOfTopLevelType != null ? fileData.kindOfTopLevelType : "cls");
      output.write("'>");

      int fileNameLength = buildFileNameWithTrailingSpaces(fileName);
      writeTableCellWithFileName(filePath, fileNameLength);
      writeCodeCoverageMetricForFile(fileData);
      writeRowClose();
   }

   @Nonnegative
   private int buildFileNameWithTrailingSpaces(@Nonnull String fileName) {
      int n = fileName.length();

      fileName.getChars(0, n, fileNameWithSpaces, 0);
      Arrays.fill(fileNameWithSpaces, n, fileNameWithSpaces.length, ' ');
      
      return n;
   }

   private void writeTableCellWithFileName(@Nonnull String filePath, @Nonnegative int fileNameLen) {
      if (sourceFilesNotFound == null || sourceFilesNotFound.contains(filePath)) {
         output.write(fileNameWithSpaces);
      }
      else {
         output.write("<a href='");
         int p = filePath.lastIndexOf('.');
         output.write(filePath.substring(0, p));
         output.write(".html'>");
         output.write(fileNameWithSpaces, 0, fileNameLen);
         output.write("</a>");
         output.write(fileNameWithSpaces, fileNameLen, fileNameWithSpaces.length - fileNameLen);
      }

      output.println("</td>");
   }

   private void writeCodeCoverageMetricForFile(@Nonnull FileCoverageData coverageInfo) {
      int percentage = coverageInfo.getCoveragePercentage();
      int covered = coverageInfo.getCoveredItems();
      int total = coverageInfo.getTotalItems();

      coveredItems += covered;
      totalItems += total;

      printCoveragePercentage(covered, total, percentage);
   }
}