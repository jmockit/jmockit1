/*
 * Copyright (c) 2006-2014 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.coverage.paths;

import java.io.*;
import java.util.*;

import org.jetbrains.annotations.*;

import mockit.coverage.*;
import mockit.coverage.data.*;

public final class PerFilePathCoverage implements PerFileCoverage
{
   private static final long serialVersionUID = 6075064821486644269L;

   @NotNull public final Map<Integer, MethodCoverageData> firstLineToMethodData =
      new LinkedHashMap<Integer, MethodCoverageData>();

   // Computed on demand:
   private transient int totalPaths;
   private transient int coveredPaths;

   public PerFilePathCoverage() { initializeCache(); }
   private void initializeCache() { totalPaths = coveredPaths = -1; }

   private void readObject(@NotNull ObjectInputStream in) throws IOException, ClassNotFoundException
   {
      initializeCache();
      in.defaultReadObject();
   }

   public void addMethod(@NotNull MethodCoverageData methodData)
   {
      firstLineToMethodData.put(methodData.getFirstLineInBody(), methodData);
   }

   public void registerExecution(int firstLineInMethodBody, int node)
   {
      MethodCoverageData methodData = firstLineToMethodData.get(firstLineInMethodBody);

      if (methodData != null) {
         methodData.markNodeAsReached(node);
      }
   }

   @Override
   public int getTotalItems()
   {
      computeValuesIfNeeded();
      return totalPaths;
   }

   @Override
   public int getCoveredItems()
   {
      computeValuesIfNeeded();
      return coveredPaths;
   }

   @Override
   public int getCoveragePercentage()
   {
      computeValuesIfNeeded();
      return CoveragePercentage.calculate(coveredPaths, totalPaths);
   }

   private void computeValuesIfNeeded()
   {
      if (totalPaths >= 0) return;

      totalPaths = coveredPaths = 0;

      for (MethodCoverageData method : firstLineToMethodData.values()) {
         totalPaths += method.getTotalPaths();
         coveredPaths += method.getCoveredPaths();
      }
   }

   public void reset()
   {
      for (MethodCoverageData methodData : firstLineToMethodData.values()) {
         methodData.reset();
      }

      initializeCache();
   }

   public void mergeInformation(@NotNull PerFilePathCoverage previousCoverage)
   {
      Map<Integer, MethodCoverageData> previousInfo = previousCoverage.firstLineToMethodData;

      for (Map.Entry<Integer, MethodCoverageData> firstLineAndInfo : firstLineToMethodData.entrySet()) {
         Integer firstLine = firstLineAndInfo.getKey();
         MethodCoverageData previousPathInfo = previousInfo.get(firstLine);

         if (previousPathInfo != null) {
            MethodCoverageData pathInfo = firstLineAndInfo.getValue();
            pathInfo.addCountsFromPreviousTestRun(previousPathInfo);
         }
      }

      for (Map.Entry<Integer, MethodCoverageData> firstLineAndInfo : previousInfo.entrySet()) {
         Integer firstLine = firstLineAndInfo.getKey();

         if (!firstLineToMethodData.containsKey(firstLine)) {
            firstLineToMethodData.put(firstLine, firstLineAndInfo.getValue());
         }
      }
   }
}
