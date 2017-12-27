/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.coverage.lines;

import java.util.*;
import javax.annotation.*;

import mockit.coverage.*;
import mockit.external.asm.*;

/**
 * Coverage data gathered for a single executable line of code in a source file.
 */
public final class LineCoverageData extends LineSegmentData
{
   private static final long serialVersionUID = -6233980722802474992L;

   // Static data:
   @Nonnull private List<BranchCoverageData> branches;
   private transient int segments;

   LineCoverageData() { branches = Collections.emptyList(); }

   public int addBranchingPoint(@Nonnull Label jumpSource, @Nonnull Label jumpTarget)
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

   @Nonnull public BranchCoverageData getBranchData(int index) { return branches.get(index); }

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

   @Nonnull public List<BranchCoverageData> getBranches() { return branches; }

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

   public int getNumberOfBranchingSourcesAndTargets()
   {
      int n = branches.size();

      if (n == 0) {
         return 0;
      }

      int count = 0;

      for (int sourceBranchIndex = 0; sourceBranchIndex < n; sourceBranchIndex += 2) {
         BranchCoverageData sourceBranch = branches.get(sourceBranchIndex);

         if (!sourceBranch.isEmpty()) {
            count++;
         }

         count++;
      }

      return count;
   }

   public int getNumberOfCoveredBranchingSourcesAndTargets()
   {
      int n = branches.size();

      if (n == 0) {
         return 0;
      }

      int sourcesAndTargetsCovered = 0;

      for (int sourceBranchIndex = 0; sourceBranchIndex < n; sourceBranchIndex += 2) {
         BranchCoverageData sourceBranch = branches.get(sourceBranchIndex);
         BranchCoverageData targetBranch = branches.get(sourceBranchIndex + 1);

         if (sourceBranch.isCovered()) {
            sourcesAndTargetsCovered++;
         }

         if (targetBranch.isCovered()) {
            int targetLine = targetBranch.getLine();

            if (targetLine == sourceBranch.getLine()) {
               sourcesAndTargetsCovered++;
            }
         }
      }

      return sourcesAndTargetsCovered;
   }

   void addCountsFromPreviousTestRun(@Nonnull LineCoverageData previousData)
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
}
