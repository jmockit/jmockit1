/*
 * Copyright (c) 2006 Rogério Liesenfeld
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
   private static boolean terminated;

   private TestRun() {}

   public static void lineExecuted(int fileIndex, int line)
   {
      if (terminated) return;

      synchronized (TestRun.class) {
         CoverageData coverageData = CoverageData.instance();
         PerFileLineCoverage fileData = coverageData.getFileData(fileIndex).lineCoverageInfo;
         CallPoint callPoint = null;

         if (coverageData.isWithCallPoints() && fileData.acceptsAdditionalCallPoints(line)) {
            callPoint = CallPoint.create(new Throwable());
         }

         int previousExecutionCount = fileData.registerExecution(line, callPoint);
         TestCoverage.INSTANCE.recordNewItemCoveredByTestIfApplicable(previousExecutionCount);
      }
   }

   public static void branchExecuted(int fileIndex, int line, int branchIndex)
   {
      if (terminated) return;

      synchronized (TestRun.class) {
         CoverageData coverageData = CoverageData.instance();
         PerFileLineCoverage fileData = coverageData.getFileData(fileIndex).lineCoverageInfo;

         if (fileData.hasValidBranch(line, branchIndex)) {
            CallPoint callPoint = null;

            if (coverageData.isWithCallPoints() && fileData.acceptsAdditionalCallPoints(line, branchIndex)) {
               callPoint = CallPoint.create(new Throwable());
            }

            int previousExecutionCount = fileData.registerExecution(line, branchIndex, callPoint);
            TestCoverage.INSTANCE.recordNewItemCoveredByTestIfApplicable(previousExecutionCount);
         }
      }
   }

   public static void nodeReached(@Nonnull String file, int firstLineInMethodBody, int node)
   {
      if (terminated) return;

      synchronized (TestRun.class) {
         CoverageData coverageData = CoverageData.instance();
         FileCoverageData fileData = coverageData.getFileData(file);

         int previousExecutionCount = fileData.pathCoverageInfo.registerExecution(firstLineInMethodBody, node);
         TestCoverage.INSTANCE.recordNewItemCoveredByTestIfApplicable(previousExecutionCount);
      }
   }

   public static void fieldAssigned(@Nonnull String file, @Nonnull String classAndFieldNames)
   {
      if (terminated) return;

      synchronized (TestRun.class) {
         CoverageData coverageData = CoverageData.instance();
         FileCoverageData fileData = coverageData.getFileData(file);
         fileData.dataCoverageInfo.registerAssignmentToStaticField(classAndFieldNames);
      }
   }

   public static void fieldRead(@Nonnull String file, @Nonnull String classAndFieldNames)
   {
      if (terminated) return;

      synchronized (TestRun.class) {
         CoverageData coverageData = CoverageData.instance();
         FileCoverageData fileData = coverageData.getFileData(file);
         fileData.dataCoverageInfo.registerReadOfStaticField(classAndFieldNames);
      }
   }

   public static void fieldAssigned(@Nonnull Object instance, @Nonnull String file, @Nonnull String classAndFieldNames)
   {
      if (terminated) return;

      synchronized (TestRun.class) {
         CoverageData coverageData = CoverageData.instance();
         FileCoverageData fileData = coverageData.getFileData(file);
         fileData.dataCoverageInfo.registerAssignmentToInstanceField(instance, classAndFieldNames);
      }
   }

   public static void fieldRead(@Nonnull Object instance, @Nonnull String file, @Nonnull String classAndFieldNames)
   {
      if (terminated) return;

      synchronized (TestRun.class) {
         CoverageData coverageData = CoverageData.instance();
         FileCoverageData fileData = coverageData.getFileData(file);
         fileData.dataCoverageInfo.registerReadOfInstanceField(instance, classAndFieldNames);
      }
   }

   static void terminate() { terminated = true; }
   public static boolean isTerminated() { return terminated; }
}
