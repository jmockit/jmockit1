/*
 * Copyright (c) 2006-2014 Rogério Liesenfeld
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

   LineCoverageData() { branches = Collections.emptyList(); }

   public int addBranch(@NotNull Label jumpSource, @NotNull Label jumpTarget)
   {
      if (noBranchesYet()) {
         branches = new ArrayList<BranchCoverageData>(4);
      }
      else {
         int existingBranchIndex = findBranchIndex(jumpSource, jumpTarget);

         if (existingBranchIndex >= 0) {
            return existingBranchIndex;
         }
      }

      BranchCoverageData data = new BranchCoverageData(jumpSource, jumpTarget);
      branches.add(data);

      return branches.size() - 1;
   }

   private boolean noBranchesYet() { return branches == Collections.<BranchCoverageData>emptyList(); }

   private int findBranchIndex(@NotNull Label jumpSource, @NotNull Label jumpTarget)
   {
      for (int i = branches.size() - 1; i >= 0; i--) {
         BranchCoverageData data = branches.get(i);

         if (data.jumpSource == jumpSource && data.jumpTarget == jumpTarget) {
            return i;
         }
      }

      return -1;
   }

   @NotNull public BranchCoverageData getBranchData(int index) { return branches.get(index); }

   public boolean acceptsAdditionalCallPoints(int branchIndex)
   {
      BranchCoverageData data = branches.get(branchIndex);
      return data.acceptsAdditionalCallPoints();
   }

   void registerExecution(int branchIndex, boolean jumped, @Nullable CallPoint callPoint)
   {
      BranchCoverageData data = branches.get(branchIndex);

      if (jumped) {
         data.registerJumpExecution(callPoint);
      }
      else {
         data.registerNoJumpExecution(callPoint);
      }
   }

   public boolean containsBranches() { return !noBranchesYet(); }
   @NotNull public List<BranchCoverageData> getBranches() { return branches; }

   public int getNumberOfSegments()
   {
      return noBranchesYet() ? 1 : 1 + branches.size();
   }

   public int getNumberOfCoveredSegments()
   {
      if (noBranchesYet()) {
         return executionCount > 0 ? 1 : 0;
      }

      return getSegmentsCovered();
   }

   private int getSegmentsCovered()
   {
      int segmentsCovered = 1;

      for (BranchCoverageData branch : branches) {
         if (branch.isCovered()) {
            segmentsCovered++;
         }
      }

      return segmentsCovered;
   }

   public void addCountsFromPreviousTestRun(@NotNull LineCoverageData previousData)
   {
      addExecutionCountAndCallPointsFromPreviousTestRun(previousData);

      if (containsBranches()) {
         for (int i = 0; i < branches.size(); i++) {
            BranchCoverageData segmentData = branches.get(i);
            BranchCoverageData previousSegmentData = previousData.branches.get(i);

            segmentData.addCountsFromPreviousTestRun(previousSegmentData);
         }
      }
   }

   @Override
   public void reset()
   {
      super.reset();

      for (BranchCoverageData branchData : branches) {
         branchData.reset();
      }
   }
}
