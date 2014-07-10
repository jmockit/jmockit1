/*
 * Copyright (c) 2006-2014 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.coverage;

import org.jetbrains.annotations.*;

import mockit.coverage.data.*;
import mockit.coverage.lines.*;

@SuppressWarnings("unused")
public final class TestRun
{
   private static boolean terminated;

   private TestRun() {}

   public static void lineExecuted(int fileIndex, int line)
   {
      if (terminated) return;

      CoverageData coverageData = CoverageData.instance();
      PerFileLineCoverage fileData = coverageData.getFileData(fileIndex).lineCoverageInfo;
      CallPoint callPoint = null;

      if (coverageData.isWithCallPoints() && fileData.acceptsAdditionalCallPoints(line)) {
         callPoint = CallPoint.create(new Throwable());
      }

      fileData.registerExecution(line, callPoint);
   }

   public static void jumpTargetExecuted(int fileIndex, int line, int segment)
   {
      if (terminated) return;

      CoverageData coverageData = CoverageData.instance();
      PerFileLineCoverage fileData = coverageData.getFileData(fileIndex).lineCoverageInfo;
      CallPoint callPoint = null;

      if (coverageData.isWithCallPoints() && fileData.acceptsAdditionalCallPoints(line, segment)) {
         callPoint = CallPoint.create(new Throwable());
      }

      fileData.registerExecution(line, segment, true, callPoint);
   }

   public static void noJumpTargetExecuted(int fileIndex, int line, int segment)
   {
      if (terminated) return;

      CoverageData coverageData = CoverageData.instance();
      PerFileLineCoverage fileData = coverageData.getFileData(fileIndex).lineCoverageInfo;
      CallPoint callPoint = null;

      if (coverageData.isWithCallPoints() && fileData.acceptsAdditionalCallPoints(line, segment)) {
         callPoint = CallPoint.create(new Throwable());
      }

      fileData.registerExecution(line, segment, false, callPoint);
   }

   public static void nodeReached(@NotNull String file, int firstLineInMethodBody, int node)
   {
      if (terminated) return;

      CoverageData coverageData = CoverageData.instance();
      FileCoverageData fileData = coverageData.getFileData(file);
      fileData.pathCoverageInfo.registerExecution(firstLineInMethodBody, node);
   }

   public static void fieldAssigned(@NotNull String file, @NotNull String classAndFieldNames)
   {
      if (terminated) return;

      CoverageData coverageData = CoverageData.instance();
      FileCoverageData fileData = coverageData.getFileData(file);
      fileData.dataCoverageInfo.registerAssignmentToStaticField(classAndFieldNames);
   }

   public static void fieldRead(@NotNull String file, @NotNull String classAndFieldNames)
   {
      if (terminated) return;

      CoverageData coverageData = CoverageData.instance();
      FileCoverageData fileData = coverageData.getFileData(file);
      fileData.dataCoverageInfo.registerReadOfStaticField(classAndFieldNames);
   }

   public static void fieldAssigned(@NotNull Object instance, @NotNull String file, @NotNull String classAndFieldNames)
   {
      if (terminated) return;

      CoverageData coverageData = CoverageData.instance();
      FileCoverageData fileData = coverageData.getFileData(file);
      fileData.dataCoverageInfo.registerAssignmentToInstanceField(instance, classAndFieldNames);
   }

   public static void fieldRead(@NotNull Object instance, @NotNull String file, @NotNull String classAndFieldNames)
   {
      if (terminated) return;

      CoverageData coverageData = CoverageData.instance();
      FileCoverageData fileData = coverageData.getFileData(file);
      fileData.dataCoverageInfo.registerReadOfInstanceField(instance, classAndFieldNames);
   }

   static void terminate() { terminated = true; }
}
