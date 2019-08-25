/*
 * Copyright (c) 2006 JMockit developers
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.coverage.lines;

import java.io.*;
import java.util.*;
import javax.annotation.*;

import mockit.coverage.*;
import mockit.coverage.data.*;

public final class PerFileLineCoverage implements PerFileCoverage
{
   private static final long serialVersionUID = 6318915843739466316L;
   private static final int[] NO_EXECUTIONS_YET = new int[0];

   @Nonnull private final Map<Integer, LineCoverageData> lineToLineData = new HashMap<>(128);

   @Nonnull private int[] executionCounts = NO_EXECUTIONS_YET;

   @Nonnull private transient LineCoverageData sharedLineData;

   // Computed on demand:
   @Nonnegative private int lastLine;
   private transient int totalSegments;
   private transient int coveredSegments;

   public PerFileLineCoverage() {
      sharedLineData = new LineCoverageData();
      initializeCache();
   }

   private void initializeCache() { totalSegments = coveredSegments = -1; }

   private void readObject(@Nonnull ObjectInputStream in) throws IOException, ClassNotFoundException {
      sharedLineData = new LineCoverageData();
      initializeCache();
      in.defaultReadObject();

      if (executionCounts.length == 0) {
         executionCounts = NO_EXECUTIONS_YET;
      }
   }

   public void addLine(@Nonnegative int line) {
      if (!lineToLineData.containsKey(line)) {
         lineToLineData.put(line, null);
      }

      if (line > lastLine) {
         // Happens for source files with multiple types, where one is only loaded
         // after another has already executed some code.
         int[] initialExecutionCounts = executionCounts;

         if (initialExecutionCounts != NO_EXECUTIONS_YET && line >= initialExecutionCounts.length) {
            int[] newCounts = new int[line + 30];
            System.arraycopy(initialExecutionCounts, 0, newCounts, 0, initialExecutionCounts.length);
            executionCounts = newCounts;
         }

         lastLine = line;
      }
   }

   @Nonnull
   public LineCoverageData getOrCreateLineData(@Nonnegative int line) {
      LineCoverageData lineData = lineToLineData.get(line);

      if (lineData == null) {
         lineData = new LineCoverageData();
         lineToLineData.put(line, lineData);
      }

      return lineData;
   }

   @Nonnull
   public BranchCoverageData getBranchData(@Nonnegative int line, @Nonnegative int index) {
      LineCoverageData lineData = lineToLineData.get(line);
      return lineData.getBranchData(index);
   }

   public void markLastLineSegmentAsEmpty(@Nonnegative int line) {
      LineCoverageData lineData = lineToLineData.get(line);
      lineData.markLastSegmentAsEmpty();
   }

   public boolean acceptsAdditionalCallPoints(@Nonnegative int line) {
      LineCoverageData lineData = getOrCreateLineData(line);
      return lineData.acceptsAdditionalCallPoints();
   }

   @Nonnegative
   public int registerExecution(@Nonnegative int line, @Nullable CallPoint callPoint) {
      if (executionCounts == NO_EXECUTIONS_YET) {
         executionCounts = new int[lastLine + 1];
      }

      int previousExecutionCount = executionCounts[line]++;

      if (callPoint != null) {
         LineCoverageData lineData = lineToLineData.get(line);
         lineData.registerExecution(callPoint);
      }

      return previousExecutionCount;
   }

   public boolean hasValidBranch(@Nonnegative int line, @Nonnegative int branchIndex) {
      LineCoverageData lineData = lineToLineData.get(line);
      return lineData.isValidBranch(branchIndex);
   }

   public boolean acceptsAdditionalCallPoints(@Nonnegative int line, @Nonnegative int branchIndex) {
      LineCoverageData lineData = lineToLineData.get(line);
      return lineData.acceptsAdditionalCallPoints(branchIndex);
   }

   public int registerExecution(@Nonnegative int line, @Nonnegative int branchIndex, @Nullable CallPoint callPoint) {
      LineCoverageData lineData = lineToLineData.get(line);
      return lineData.registerExecution(branchIndex, callPoint);
   }

   @Nonnegative public int getLineCount() { return lastLine; }
   @Nonnegative public int getExecutableLineCount() { return lineToLineData.size(); }

   public boolean hasLineData(@Nonnegative int line) {
      return executionCounts != NO_EXECUTIONS_YET && lineToLineData.containsKey(line);
   }

   @Nonnull
   public LineCoverageData getLineData(@Nonnegative int line) {
      LineCoverageData data = lineToLineData.get(line);

      if (data == null) {
         data = sharedLineData;
      }

      data.setExecutionCount(executionCounts[line]);
      return data;
   }

   public void markLineAsReachable(@Nonnegative int line) {
      LineCoverageData data = lineToLineData.get(line);

      if (data != null) {
         data.markAsReachable();
      }
   }

   public int getExecutionCount(@Nonnegative int line) {
      return line < executionCounts.length ? executionCounts[line] : -1;
   }

   @Override @Nonnegative
   public int getTotalItems() {
      computeValuesIfNeeded();
      return totalSegments;
   }

   @Override @Nonnegative
   public int getCoveredItems() {
      computeValuesIfNeeded();
      return coveredSegments;
   }

   @Override
   public int getCoveragePercentage() {
      computeValuesIfNeeded();
      return CoveragePercentage.calculate(coveredSegments, totalSegments);
   }

   private void computeValuesIfNeeded() {
      if (totalSegments >= 0) return;
      totalSegments = coveredSegments = 0;

      for (int line = 1, n = lastLine; line <= n; line++) {
         if (lineToLineData.containsKey(line)) {
            LineCoverageData lineData = lineToLineData.get(line);
            int executionCount = executionCounts == NO_EXECUTIONS_YET ? 0 : executionCounts[line];

            if (lineData == null) {
               totalSegments++;

               if (executionCount > 0) {
                  coveredSegments++;
               }
            }
            else {
               lineData.setExecutionCount(executionCount);
               totalSegments += lineData.getNumberOfSegments();
               coveredSegments += lineData.getNumberOfCoveredSegments();
            }
         }
      }
   }

   @Nonnegative
   public int getNumberOfSegments(@Nonnegative int line) {
      if (!lineToLineData.containsKey(line)) {
         return 0;
      }

      LineCoverageData lineData = lineToLineData.get(line);
      return lineData == null ? 1 : lineData.getNumberOfSegments();
   }

   @Nonnegative
   public int getNumberOfBranchingSourcesAndTargets(@Nonnegative int line) {
      LineCoverageData lineData = lineToLineData.get(line);

      if (lineData == null) {
         return 0;
      }

      return lineData.getNumberOfBranchingSourcesAndTargets();
   }

   public void mergeInformation(@Nonnull PerFileLineCoverage previousCoverage) {
      Map<Integer, LineCoverageData> previousInfo = previousCoverage.lineToLineData;
      boolean previousRunHadLinesExecuted = previousCoverage.executionCounts.length > 0;

      for (Map.Entry<Integer, LineCoverageData> lineAndInfo : lineToLineData.entrySet()) {
         Integer line = lineAndInfo.getKey();
         LineCoverageData previousLineInfo = previousInfo.get(line);

         if (previousLineInfo != null) {
            LineCoverageData lineInfo = lineAndInfo.getValue();

            if (lineInfo == null) {
               lineInfo = new LineCoverageData();
               lineAndInfo.setValue(lineInfo);
            }

            lineInfo.addCountsFromPreviousTestRun(previousLineInfo);

            if (previousRunHadLinesExecuted) {
               createExecutionCountsArrayIfNeeded(previousCoverage);
               executionCounts[line] += previousCoverage.executionCounts[line];
            }
         }
      }

      for (Map.Entry<Integer, LineCoverageData> lineAndInfo : previousInfo.entrySet()) {
         Integer line = lineAndInfo.getKey();

         if (!lineToLineData.containsKey(line)) {
            LineCoverageData previousLineInfo = lineAndInfo.getValue();
            lineToLineData.put(line, previousLineInfo);

            if (previousRunHadLinesExecuted) {
               createExecutionCountsArrayIfNeeded(previousCoverage);
               executionCounts[line] = previousCoverage.executionCounts[line];
            }
         }
      }
   }

   private void createExecutionCountsArrayIfNeeded(@Nonnull PerFileLineCoverage previousCoverage) {
      if (executionCounts == NO_EXECUTIONS_YET) {
         executionCounts = new int[previousCoverage.executionCounts.length];
      }
   }
}