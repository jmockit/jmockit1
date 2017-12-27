/*
 * Copyright (c) 2006 Rogério Liesenfeld
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

   @Nonnull private final Map<Integer, LineCoverageData> lineToLineData = new HashMap<Integer, LineCoverageData>(128);

   @SuppressWarnings("FieldAccessedSynchronizedAndUnsynchronized")
   @Nonnull private int[] executionCounts = NO_EXECUTIONS_YET;

   @Nonnull private transient LineCoverageData sharedLineData;

   // Computed on demand:
   private int lastLine;
   private transient int totalSegments;
   private transient int coveredSegments;

   public PerFileLineCoverage()
   {
      sharedLineData = new LineCoverageData();
      initializeCache();
   }

   private void initializeCache() { totalSegments = coveredSegments = -1; }

   private void readObject(@Nonnull ObjectInputStream in) throws IOException, ClassNotFoundException
   {
      sharedLineData = new LineCoverageData();
      initializeCache();
      in.defaultReadObject();

      if (executionCounts.length == 0) {
         executionCounts = NO_EXECUTIONS_YET;
      }
   }

   public void addLine(int line)
   {
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
   public LineCoverageData getOrCreateLineData(int line)
   {
      LineCoverageData lineData = lineToLineData.get(line);

      if (lineData == null) {
         lineData = new LineCoverageData();
         lineToLineData.put(line, lineData);
      }

      return lineData;
   }

   @Nonnull
   public BranchCoverageData getBranchData(int line, int index)
   {
      LineCoverageData lineData = lineToLineData.get(line);
      return lineData.getBranchData(index);
   }

   public void markLastLineSegmentAsEmpty(int line)
   {
      LineCoverageData lineData = lineToLineData.get(line);
      lineData.markLastSegmentAsEmpty();
   }

   public boolean acceptsAdditionalCallPoints(int line)
   {
      LineCoverageData lineData = getOrCreateLineData(line);
      return lineData.acceptsAdditionalCallPoints();
   }

   public int registerExecution(int line, @Nullable CallPoint callPoint)
   {
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

   public boolean hasValidBranch(int line, int branchIndex)
   {
      LineCoverageData lineData = lineToLineData.get(line);
      return lineData.isValidBranch(branchIndex);
   }

   public boolean acceptsAdditionalCallPoints(int line, int branchIndex)
   {
      LineCoverageData lineData = lineToLineData.get(line);
      return lineData.acceptsAdditionalCallPoints(branchIndex);
   }

   public int registerExecution(int line, int branchIndex, @Nullable CallPoint callPoint)
   {
      LineCoverageData lineData = lineToLineData.get(line);
      return lineData.registerExecution(branchIndex, callPoint);
   }

   public int getLineCount() { return lastLine; }
   public int getExecutableLineCount() { return lineToLineData.size(); }

   public boolean hasLineData(int line)
   {
      return executionCounts != NO_EXECUTIONS_YET && lineToLineData.containsKey(line);
   }

   @Nonnull
   public LineCoverageData getLineData(int line)
   {
      LineCoverageData data = lineToLineData.get(line);

      if (data == null) {
         data = sharedLineData;
      }

      data.setExecutionCount(executionCounts[line]);
      return data;
   }

   public int getExecutionCount(int line)
   {
      return line < executionCounts.length ? executionCounts[line] : -1;
   }

   @Override
   public int getTotalItems()
   {
      computeValuesIfNeeded();
      return totalSegments;
   }

   @Override
   public int getCoveredItems()
   {
      computeValuesIfNeeded();
      return coveredSegments;
   }

   @Override
   public int getCoveragePercentage()
   {
      computeValuesIfNeeded();
      return CoveragePercentage.calculate(coveredSegments, totalSegments);
   }

   private void computeValuesIfNeeded()
   {
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

   public int getNumberOfSegments(int line)
   {
      if (!lineToLineData.containsKey(line)) {
         return 0;
      }

      LineCoverageData lineData = lineToLineData.get(line);
      return lineData == null ? 1 : lineData.getNumberOfSegments();
   }

   public int getNumberOfBranchingSourcesAndTargets(int line)
   {
      LineCoverageData lineData = lineToLineData.get(line);

      if (lineData == null) {
         return 0;
      }

      return lineData.getNumberOfBranchingSourcesAndTargets();
   }

   public void mergeInformation(@Nonnull PerFileLineCoverage previousCoverage)
   {
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

   private void createExecutionCountsArrayIfNeeded(@Nonnull PerFileLineCoverage previousCoverage)
   {
      if (executionCounts == NO_EXECUTIONS_YET) {
         executionCounts = new int[previousCoverage.executionCounts.length];
      }
   }
}
