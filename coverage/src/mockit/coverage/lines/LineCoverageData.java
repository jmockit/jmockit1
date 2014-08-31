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

   public int addBranchingPoint(@NotNull Label jumpSource, @NotNull Label jumpTarget)
   {
      int initialIndex = branches.size();

      if (initialIndex == 0) {
         branches = new ArrayList<BranchCoverageData>(4);
      }
      else {
         int existingBranchIndex = findBranchIndex(jumpSource, jumpTarget);

         if (existingBranchIndex >= 0) {
            return existingBranchIndex;
         }
      }

      branches.add(new BranchCoverageData(jumpSource));
      branches.add(new BranchCoverageData(jumpTarget));
      return initialIndex;
   }

   public boolean noBranchesYet() { return branches == Collections.<BranchCoverageData>emptyList(); }

   private int findBranchIndex(@NotNull Label jumpSource, @NotNull Label jumpTarget)
   {
      for (int i = branches.size() - 1; i >= 0; i -= 2) {
         BranchCoverageData targetBranch = branches.get(i);
         BranchCoverageData sourceBranch = branches.get(i - 1);

         if (sourceBranch.label == jumpSource && targetBranch.label == jumpTarget) {
            return i;
         }
      }

      return -1;
   }

   @NotNull public BranchCoverageData getBranchData(int index) { return branches.get(index); }

   boolean acceptsAdditionalCallPoints(int branchIndex)
   {
      BranchCoverageData data = branches.get(branchIndex);
      return data.acceptsAdditionalCallPoints();
   }

   void registerExecution(int branchIndex, @Nullable CallPoint callPoint)
   {
      BranchCoverageData data = branches.get(branchIndex);
      data.registerExecution(callPoint);
   }

   public boolean containsBranches() { return !noBranchesYet(); }
   @NotNull public List<BranchCoverageData> getBranches() { return branches; }

   int getNumberOfSegments()
   {
      int n = branches.size();

      if (n == 0) {
         return 1;
      }

      BranchCoverageData branchData = branches.get(0);
      Integer sourceLine = branchData.getLine();
      int count = 2;

      for (int i = 1; i < n; i += 2) {
         branchData = branches.get(i);
         Integer targetLine = branchData.getLine();

         if (targetLine != null && targetLine.equals(sourceLine)) {
            count++;
         }
      }

      return count;
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
      int segmentsCovered = executionCount > 0 ? 1 : 0;

      for (int i = 0, n = branches.size(); i < n; i++) {
         BranchCoverageData branch = branches.get(i);

         if (branch.isCovered()) {
            segmentsCovered++;
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
