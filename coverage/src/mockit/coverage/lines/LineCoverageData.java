/*
 * Copyright (c) 2006-2015 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.coverage.lines;

import java.util.*;

import org.jetbrains.annotations.*;

import mockit.coverage.*;
import mockit.external.asm.*;

/**
 * Coverage data gathered for a single executable line of code in a source file.
 */
public final class LineCoverageData extends LineSegmentData
{
   private static final long serialVersionUID = -6233980722802474992L;

   // Static data:
   @NotNull private List<BranchCoverageData> branches;
   private transient int segments;

   LineCoverageData() { branches = Collections.emptyList(); }

   public int addBranchingPoint(@NotNull Label jumpSource, @NotNull Label jumpTarget)
   {
      int initialIndex = branches.size();

      if (initialIndex == 0) {
         branches = new ArrayList<BranchCoverageData>(4);
      }

      branches.add(new BranchCoverageData(jumpSource));
      branches.add(new BranchCoverageData(jumpTarget));
      return initialIndex;
   }

   void markLastSegmentAsEmpty()
   {
      BranchCoverageData lastBranch = branches.get(branches.size() - 1);
      lastBranch.markAsEmpty();
   }

   public boolean noBranchesYet() { return branches == Collections.<BranchCoverageData>emptyList(); }

   @NotNull public BranchCoverageData getBranchData(int index) { return branches.get(index); }

   boolean acceptsAdditionalCallPoints(int branchIndex)
   {
      BranchCoverageData data = branches.get(branchIndex);
      return data.acceptsAdditionalCallPoints();
   }

   int registerExecution(int branchIndex, @Nullable CallPoint callPoint)
   {
      BranchCoverageData data = branches.get(branchIndex);
      return data.registerExecution(callPoint);
   }

   public boolean containsBranches() { return !noBranchesYet(); }

   @NotNull public List<BranchCoverageData> getBranches() { return branches; }

   public boolean isValidBranch(int branchIndex) { return branches.get(branchIndex) != BranchCoverageData.INVALID; }

   public int getNumberOfSegments()
   {
      int previouslyCounted = segments;

      if (previouslyCounted > 0) {
         return previouslyCounted;
      }

      int n = branches.size();

      if (n == 0) {
         return 1;
      }

      int count = 1;

      for (int targetBranchIndex = 1; targetBranchIndex < n; targetBranchIndex += 2) {
         BranchCoverageData targetBranch = branches.get(targetBranchIndex);
         int targetLine = targetBranch.getLine();

         if (targetLine > 0) {
            BranchCoverageData sourceBranch = branches.get(targetBranchIndex - 1);
            int sourceLine = sourceBranch.getLine();

            if (targetLine == sourceLine) {
               count++;
            }

            if (!targetBranch.isEmpty()) {
               count++;
            }
         }
      }

      segments = count;
      return count;
   }

   public int getNumberOfCoveredSegments()
   {
      int segmentsCovered = executionCount > 0 ? 1 : 0;
      int n = branches.size();

      if (n == 0) {
         return segmentsCovered;
      }

      for (int sourceBranchIndex = 0; sourceBranchIndex < n; sourceBranchIndex += 2) {
         BranchCoverageData sourceBranch = branches.get(sourceBranchIndex);
         BranchCoverageData targetBranch = branches.get(sourceBranchIndex + 1);

         if (sourceBranch.isCovered() && !targetBranch.isEmpty()) {
            segmentsCovered++;
         }

         if (targetBranch.isCovered()) {
            int targetLine = targetBranch.getLine();

            if (targetLine == sourceBranch.getLine()) {
               segmentsCovered++;
            }
         }
      }

      return segmentsCovered;
   }

   void addCountsFromPreviousTestRun(@NotNull LineCoverageData previousData)
   {
      addExecutionCountAndCallPointsFromPreviousTestRun(previousData);

      if (containsBranches()) {
         for (int i = 0, n = branches.size(); i < n; i++) {
            BranchCoverageData segmentData = branches.get(i);
            BranchCoverageData previousSegmentData = previousData.branches.get(i);

            segmentData.addExecutionCountAndCallPointsFromPreviousTestRun(previousSegmentData);
         }
      }
   }

   @Override
   void reset()
   {
      super.reset();

      for (BranchCoverageData branchData : branches) {
         branchData.reset();
      }
   }
}
