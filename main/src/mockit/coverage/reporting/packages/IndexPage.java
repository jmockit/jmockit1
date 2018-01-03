/*
 * Copyright (c) 2006 Rogério Liesenfeld
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
   @Nonnull private final Map<String, int[]> packageToPackagePercentages;
   @Nonnull private final PackageCoverageReport packageReport;
   private final int totalFileCount;

   public IndexPage(
      @Nonnull File outputFile, @Nullable List<File> sourceDirs, @Nullable Collection<String> sourceFilesNotFound,
      @Nonnull Map<String, List<String>> packageToFiles, @Nonnull Map<String, FileCoverageData> fileToFileData)
      throws IOException
   {
      super(new OutputFile(outputFile), "    ");
      this.sourceDirs = sourceDirs;
      this.packageToFiles = packageToFiles;
      packageToPackagePercentages = new HashMap<String, int[]>();
      packageReport = new PackageCoverageReport(output, sourceFilesNotFound, fileToFileData, packageToFiles.values());
      totalFileCount = totalNumberOfSourceFilesWithCoverageData(fileToFileData.values());
   }

   private static int totalNumberOfSourceFilesWithCoverageData(@Nonnull Collection<FileCoverageData> fileData)
   {
      return fileData.size() - Collections.frequency(fileData, null);
   }

   public void generate()
   {
      try {
         writeHeader();

         List<String> packages = new ArrayList<String>(packageToFiles.keySet());
         writeMetricsForEachFile(null, packages);
         writeLineWithCoverageTotals();
         output.println("  </table>");

         writeListOfRedundantTestsIfAny();
         writeFooter();
      }
      finally {
         output.close();
      }
   }

   private void writeHeader()
   {
      ((OutputFile) output).writeCommonHeader("Code Coverage Report");

      output.println("  <table id='packages'>");

      writeTableCaption();
      writeTableFirstRowWithColumnTitles();
   }

   private void writeTableCaption()
   {
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
   private String getCommaSeparatedListOfSourceDirs()
   {
      List<File> dirs = sourceDirs;
      assert dirs != null;
      removeRedundantSourceDirectories(dirs);

      String concatenatedSourceDirs = dirs.toString();
      String prefixToRemove = ".." + File.separatorChar;
      String commaSepDirs = concatenatedSourceDirs.replace(prefixToRemove, "");
      return commaSepDirs.substring(1, commaSepDirs.length() - 1);
   }

   private static void removeRedundantSourceDirectories(@Nonnull List<File> dirs)
   {
      for (int i = 0; i < dirs.size(); i++) {
         i = removeRedundantSourceDirectory(dirs, i);
      }
   }

   private static int removeRedundantSourceDirectory(@Nonnull List<File> dirs, @Nonnegative int i)
   {
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

   private void writeTableFirstRowWithColumnTitles()
   {
      output.println("    <tr>");
      output.write("      <th style='cursor: col-resize' onclick='showHideAllFiles()'>Packages: ");
      output.print(packageToFiles.keySet().size());
      output.println("</th>");
      output.write(
         "      <th onclick='location.reload()' style='cursor: n-resize' title='" +
         "Click on the title for each metric to sort by size (total number of line segments, paths, or fields)." +
         "'>Files: ");
      output.print(totalFileCount);
      output.println("</th>");

      Metrics.performAction(new Metrics.Action() {
         int tableColumn = 1;

         @Override
         public void perform(@Nonnull Metrics metric)
         {
            writeHeaderCellWithMetricNameAndDescription(metric);
            tableColumn++;
         }

         private void writeHeaderCellWithMetricNameAndDescription(@Nonnull Metrics metric)
         {
            output.write("      <th onclick='sortTables(");
            output.print(tableColumn);
            output.write(")' style='cursor: n-resize' title='");
            output.write(metric.htmlDescription);
            output.write("'>");
            output.write(metric.toString());
            output.println("</th>");
         }
      });

      output.println("    </tr>");
   }

   private void writeLineWithCoverageTotals()
   {
      output.println("    <tr class='total'>");
      output.println("      <td>Total</td><td>&nbsp;</td>");

      Metrics.performAction(new Metrics.Action() {
         @Override
         public void perform(@Nonnull Metrics metric) { writeLineWithCoverageTotals(metric); }
      });

      output.println("    </tr>");
   }

   private void writeLineWithCoverageTotals(@Nonnull Metrics metric)
   {
      int covered = coveredItems[metric.ordinal()];
      int total = totalItems[metric.ordinal()];
      int percentage = CoveragePercentage.calculate(covered, total);

      printCoveragePercentage(metric, covered, total, percentage);
   }

   @Override @SuppressWarnings("ParameterNameDiffersFromOverriddenParameter")
   protected void writeMetricsForFile(String unused, @Nonnull final String packageName)
   {
      writeRowStart();
      writeTableCellWithPackageName(packageName);
      writeInternalTableForSourceFiles(packageName);

      Metrics.performAction(new Metrics.Action() {
         @Override
         public void perform(@Nonnull Metrics metric) { writeCoveragePercentageForPackage(packageName, metric); }
      });

      writeRowClose();
   }

   private void writeTableCellWithPackageName(@Nonnull String packageName)
   {
      printIndent();
      output.write("  <td class='package");

      List<String> filesInPackage = packageToFiles.get(packageName);

      if (filesInPackage.size() > 1) {
         output.write(" click' onclick='showHideFiles(this)");
      }

      output.write("'>");
      output.write(packageName.replace('/', '.'));
      output.println("</td>");
   }

   private void writeInternalTableForSourceFiles(@Nonnull final String packageName)
   {
      printIndent();
      output.println("  <td>");

      printIndent();
      output.println("    <table width='100%'>");

      List<String> fileNames = packageToFiles.get(packageName);
      packageReport.writeMetricsForEachFile(packageName, fileNames);

      Metrics.performAction(new Metrics.Action() {
         @Override
         public void perform(@Nonnull Metrics metric) { recordCoverageInformationForPackage(packageName, metric); }
      });

      printIndent();
      output.println("    </table>");
      printIndent();

      writeInitiallyHiddenSourceFileCount(fileNames.size());
      output.println("  </td>");
   }

   private void recordCoverageInformationForPackage(@Nonnull String packageName, @Nonnull Metrics metric)
   {
      int coveredInPackage = packageReport.coveredItems[metric.ordinal()];
      int totalInPackage = packageReport.totalItems[metric.ordinal()];
      int packagePercentage = CoveragePercentage.calculate(coveredInPackage, totalInPackage);

      setPackageCoveragePercentage(packageName, metric, packagePercentage);

      totalItems[metric.ordinal()] += totalInPackage;
      coveredItems[metric.ordinal()] += coveredInPackage;
   }

   private void setPackageCoveragePercentage(@Nonnull String packageName, @Nonnull Metrics metric, int percentage)
   {
      int[] percentages = packageToPackagePercentages.get(packageName);

      if (percentages == null) {
         percentages = new int[Metrics.values().length];
         packageToPackagePercentages.put(packageName, percentages);
      }

      percentages[metric.ordinal()] = percentage;
   }

   private void writeInitiallyHiddenSourceFileCount(int fileCount)
   {
      output.write("    <span>(");
      output.print(fileCount);
      output.println(" source files)</span>");
   }

   private void writeCoveragePercentageForPackage(@Nonnull String packageName, @Nonnull Metrics metric)
   {
      int coveredInPackage = packageReport.coveredItems[metric.ordinal()];
      int totalInPackage = packageReport.totalItems[metric.ordinal()];
      int filePercentage = packageToPackagePercentages.get(packageName)[metric.ordinal()];

      printCoveragePercentage(metric, coveredInPackage, totalInPackage, filePercentage);
   }

   @Override
   protected void writeClassAttributeForCoveragePercentageCell()
   {
      output.write("class='pt' ");
   }

   private void writeListOfRedundantTestsIfAny()
   {
      List<Method> redundantTests = TestCoverage.INSTANCE.getRedundantTests();

      if (!redundantTests.isEmpty()) {
         output.println("  <br/>Redundant tests:");
         output.println(
            "  <ol title=\"Tests are regarded as redundant when they don't cover any additional line " +
            "segments or paths that haven't already been covered by a previous test.\n" +
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

   private void writeFooter()
   {
      output.println("  <p>");
      output.println("    <a href='http://jmockit.org'><img src='logo.png'></a>");
      output.write("    Generated on ");
      output.println(new Date());
      output.println("  </p>");
      ((OutputFile) output).writeCommonFooter();
   }
}
