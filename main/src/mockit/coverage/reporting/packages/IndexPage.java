/*
 * Copyright (c) 2006 JMockit developers
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.coverage.reporting.packages;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;
import javax.annotation.*;

import mockit.coverage.*;
import mockit.coverage.data.*;
import mockit.coverage.reporting.*;
import mockit.coverage.testRedundancy.*;

public final class IndexPage extends ListWithFilesAndPercentages
{
   @Nullable private final List<File> sourceDirs;
   @Nonnull private final Map<String, List<String>> packageToFiles;
   @Nonnull private final Map<String, Integer> packageToPackagePercentages;
   @Nonnull private final PackageCoverageReport packageReport;
   @Nonnegative private final int totalFileCount;

   public IndexPage(
      @Nonnull File outputFile, @Nullable List<File> sourceDirs, @Nullable Collection<String> sourceFilesNotFound,
      @Nonnull Map<String, List<String>> packageToFiles, @Nonnull Map<String, FileCoverageData> fileToFileData
   ) throws IOException {
      super(new OutputFile(outputFile), "    ");
      this.sourceDirs = sourceDirs;
      this.packageToFiles = packageToFiles;
      packageToPackagePercentages = new HashMap<>();
      packageReport = new PackageCoverageReport(output, sourceFilesNotFound, fileToFileData, packageToFiles.values());
      totalFileCount = totalNumberOfSourceFilesWithCoverageData(fileToFileData.values());
   }

   @Nonnegative
   private static int totalNumberOfSourceFilesWithCoverageData(@Nonnull Collection<FileCoverageData> fileData) {
      return fileData.size() - Collections.frequency(fileData, null);
   }

   public void generate() {
      try {
         writeHeader();

         List<String> packages = new ArrayList<>(packageToFiles.keySet());
         writeMetricsForEachFile(null, packages);
         writeLineWithCoverageTotal();
         output.println("  </table>");

         writeListOfRedundantTestsIfAny();
         writeFooter();
      }
      finally {
         output.close();
      }
   }

   private void writeHeader() {
      ((OutputFile) output).writeCommonHeader("Code Coverage Report");

      output.println("  <table id='packages'>");

      writeTableCaption();
      writeTableFirstRowWithColumnTitles();
   }

   private void writeTableCaption() {
      if (sourceDirs == null) {
         output.println("    <caption>All Packages and Files</caption>");
      }
      else {
         output.write("    <caption>All Packages and Files<div style='font-size: smaller'>");
         output.write(getCommaSeparatedListOfSourceDirs());
         output.println("</div></caption>");
      }
   }

   @Nonnull
   private String getCommaSeparatedListOfSourceDirs() {
      List<File> dirs = sourceDirs;
      assert dirs != null;
      removeRedundantSourceDirectories(dirs);

      String concatenatedSourceDirs = dirs.toString();
      String prefixToRemove = ".." + File.separatorChar;
      String commaSepDirs = concatenatedSourceDirs.replace(prefixToRemove, "");
      return commaSepDirs.substring(1, commaSepDirs.length() - 1);
   }

   private static void removeRedundantSourceDirectories(@Nonnull List<File> dirs) {
      for (int i = 0; i < dirs.size(); i++) {
         i = removeRedundantSourceDirectory(dirs, i);
      }
   }

   private static int removeRedundantSourceDirectory(@Nonnull List<File> dirs, @Nonnegative int i) {
      String dir1 = dirs.get(i).getPath();
      int j = i + 1;

      while (j < dirs.size()) {
         String dir2 = dirs.get(j).getPath();

         if (dir1.startsWith(dir2)) {
            dirs.remove(j);
         }
         else if (dir2.startsWith(dir1)) {
            dirs.remove(i);
            i--;
            break;
         }
         else {
            j++;
         }
      }

      return i;
   }

   private void writeTableFirstRowWithColumnTitles() {
      output.println("    <tr>");
      output.write("      <th style='cursor: col-resize' onclick='showHideAllFiles()'>Packages: ");
      output.print(packageToFiles.keySet().size());
      output.println("</th>");
      output.write(
         "      <th onclick='location.reload()' style='cursor: n-resize' title='" +
         "Click on the column title to the right to sort by size (total number of items).'>Files: ");
      output.print(totalFileCount);
      output.println("</th>");
      writeHeaderCellWithMetricNameAndDescription();
      output.println("    </tr>");
   }

   private void writeHeaderCellWithMetricNameAndDescription() {
      output.println(
         "      <th onclick='sortTables()' style='cursor: n-resize' title='" +
         "Measures how much of the executable production code (executable lines and fields) was exercised by tests.\r\n" +
         "An executable line of code contains one or more executable segments, separated by branching points\r\n" +
         "(if..else instructions, logical operators, etc.).\r\n" +
         "To be fully exercised, a field must have the last value assigned to it read by at least one test.\r\n" +
         "Percentages are calculated as 100*(NE + NFE)/(NS + NF), where NS is the number of segments, NF the number\r\n" +
         "of non-final fields, NE the number of executed segments, and NFE the number of fully exercised fields." +
         "'>Cvrg</th>");
   }

   private void writeLineWithCoverageTotal() {
      output.println("    <tr class='total'>");
      output.println("      <td>Total</td><td>&nbsp;</td>");

      int covered = coveredItems;
      int total = totalItems;
      int percentage = CoveragePercentage.calculate(covered, total);
      printCoveragePercentage(covered, total, percentage);

      output.println("    </tr>");
   }

   @Override @SuppressWarnings("ParameterNameDiffersFromOverriddenParameter")
   protected void writeMetricsForFile(String unused, @Nonnull String packageName) {
      writeRowStart();
      writeTableCellWithPackageName(packageName);
      writeInternalTableForSourceFiles(packageName);
      writeCoveragePercentageForPackage(packageName);
      writeRowClose();
   }

   private void writeTableCellWithPackageName(@Nonnull String packageName) {
      printIndent();
      output.write("  <td class='pkg");

      List<String> filesInPackage = packageToFiles.get(packageName);

      if (filesInPackage.size() > 1) {
         output.write(" click' onclick='showHideFiles(this)");
      }

      output.write("'>");
      output.write(packageName.replace('/', '.'));
      output.println("</td>");
   }

   private void writeInternalTableForSourceFiles(@Nonnull String packageName) {
      printIndent();
      output.println("  <td>");

      printIndent();
      output.println("    <table>");

      List<String> fileNames = packageToFiles.get(packageName);
      packageReport.writeMetricsForEachFile(packageName, fileNames);

      recordCoverageInformationForPackage(packageName);

      printIndent();
      output.println("    </table>");
      printIndent();

      writeInitiallyHiddenSourceFileCount(fileNames.size());
      printIndent();
      output.println("  </td>");
   }

   private void recordCoverageInformationForPackage(@Nonnull String packageName) {
      int coveredInPackage = packageReport.coveredItems;
      int totalInPackage = packageReport.totalItems;
      int packagePercentage = CoveragePercentage.calculate(coveredInPackage, totalInPackage);

      totalItems += totalInPackage;
      coveredItems += coveredInPackage;
      packageToPackagePercentages.put(packageName, packagePercentage);
   }

   private void writeInitiallyHiddenSourceFileCount(@Nonnegative int fileCount) {
      output.write("    <span>(");
      output.print(fileCount);
      output.println(" source files)</span>");
   }

   private void writeCoveragePercentageForPackage(@Nonnull String packageName) {
      int filePercentage = packageToPackagePercentages.get(packageName);
      printCoveragePercentage(packageReport.coveredItems, packageReport.totalItems, filePercentage);
   }

   private void writeListOfRedundantTestsIfAny() {
      TestCoverage testCoverage = TestCoverage.INSTANCE;

      if (testCoverage == null) {
         return;
      }

      List<Method> redundantTests = testCoverage.getRedundantTests();

      if (!redundantTests.isEmpty()) {
         output.println("  <br>Redundant tests:");
         output.println(
            "  <ol title=\"Tests are regarded as redundant when they don't cover any additional line " +
            "segments or fields that haven't already been covered by a previous test.\n" +
            "Note this means the list of redundant tests depends on the order of test execution.\n" +
            "Such a test can be removed without weakening the test suite, as long as another test " +
            "for the same scenario performs its assertions.\">");

         for (Method testMethod : redundantTests) {
            String testDescription = testMethod.getDeclaringClass().getSimpleName() + '.' + testMethod.getName();

            output.append("");
            output.write("    <li>");
            output.write(testDescription);
            output.println("</li>");
         }

         output.println("  </ol>");
      }
   }

   private void writeFooter() {
      output.println("  <p>");
      output.println("    <a href='http://jmockit.github.io'><img src='logo.png'></a>");
      output.write("    Generated on ");
      output.println(new Date());
      output.println("  </p>");
      ((OutputFile) output).writeCommonFooter();
   }
}