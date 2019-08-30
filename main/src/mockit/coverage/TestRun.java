/*
 * Copyright (c) 2006 JMockit developers
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.coverage;

import javax.annotation.*;

import mockit.coverage.data.*;
import mockit.coverage.lines.*;
import mockit.coverage.testRedundancy.*;

@SuppressWarnings("unused")
public final class TestRun
{
   private static final Object LOCK = new Object();
   private static boolean terminated;

   private TestRun() {}

   public static void lineExecuted(@Nonnegative int fileIndex, @Nonnegative int line) {
      if (terminated) return;

      synchronized (LOCK) {
         CoverageData coverageData = CoverageData.instance();
         PerFileLineCoverage fileData = coverageData.getFileData(fileIndex).lineCoverageInfo;
         CallPoint callPoint = null;

         if (coverageData.isWithCallPoints() && fileData.acceptsAdditionalCallPoints(line)) {
            callPoint = CallPoint.create(new Throwable());
         }

         int previousExecutionCount = fileData.registerExecution(line, callPoint);
         recordNewLineOrSegmentAsCoveredIfApplicable(previousExecutionCount);
      }
   }

   private static void recordNewLineOrSegmentAsCoveredIfApplicable(@Nonnegative int previousExecutionCount) {
      TestCoverage testCoverage = TestCoverage.INSTANCE;

      if (testCoverage != null) {
         testCoverage.recordNewItemCoveredByTestIfApplicable(previousExecutionCount);
      }
   }

   public static void branchExecuted(@Nonnegative int fileIndex, @Nonnegative int line, @Nonnegative int branchIndex) {
      if (terminated) return;

      synchronized (LOCK) {
         CoverageData coverageData = CoverageData.instance();
         PerFileLineCoverage fileData = coverageData.getFileData(fileIndex).lineCoverageInfo;

         if (fileData.hasValidBranch(line, branchIndex)) {
            CallPoint callPoint = null;

            if (coverageData.isWithCallPoints() && fileData.acceptsAdditionalCallPoints(line, branchIndex)) {
               callPoint = CallPoint.create(new Throwable());
            }

            int previousExecutionCount = fileData.registerExecution(line, branchIndex, callPoint);
            recordNewLineOrSegmentAsCoveredIfApplicable(previousExecutionCount);
         }
      }
   }

   public static void fieldAssigned(@Nonnull String file, @Nonnull String classAndFieldNames) {
      if (terminated) return;

      synchronized (LOCK) {
         CoverageData coverageData = CoverageData.instance();
         FileCoverageData fileData = coverageData.getFileData(file);
         fileData.dataCoverageInfo.registerAssignmentToStaticField(classAndFieldNames);
      }
   }

   public static void fieldRead(@Nonnull String file, @Nonnull String classAndFieldNames) {
      if (terminated) return;

      synchronized (LOCK) {
         CoverageData coverageData = CoverageData.instance();
         FileCoverageData fileData = coverageData.getFileData(file);
         fileData.dataCoverageInfo.registerReadOfStaticField(classAndFieldNames);
      }
   }

   public static void fieldAssigned(@Nonnull Object instance, @Nonnull String file, @Nonnull String classAndFieldNames) {
      if (terminated) return;

      synchronized (LOCK) {
         CoverageData coverageData = CoverageData.instance();
         FileCoverageData fileData = coverageData.getFileData(file);
         fileData.dataCoverageInfo.registerAssignmentToInstanceField(instance, classAndFieldNames);
      }
   }

   public static void fieldRead(@Nonnull Object instance, @Nonnull String file, @Nonnull String classAndFieldNames) {
      if (terminated) return;

      synchronized (LOCK) {
         CoverageData coverageData = CoverageData.instance();
         FileCoverageData fileData = coverageData.getFileData(file);
         fileData.dataCoverageInfo.registerReadOfInstanceField(instance, classAndFieldNames);
      }
   }

   static void terminate() { terminated = true; }
   public static boolean isTerminated() { return terminated; }
}