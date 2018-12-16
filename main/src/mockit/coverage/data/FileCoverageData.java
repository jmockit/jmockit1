/*
 * Copyright (c) 2006 JMockit developers
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.coverage.data;

import java.io.*;
import javax.annotation.*;

import mockit.coverage.*;
import mockit.coverage.dataItems.*;
import mockit.coverage.lines.*;

/**
 * Coverage data gathered for the lines, branching points, and fields of a single source file.
 */
public final class FileCoverageData implements Serializable
{
   private static final long serialVersionUID = 3508572808457541012L;

   @Nonnull private static final PerFileLineCoverage NO_LINE_INFO = new PerFileLineCoverage();
   @Nonnull private static final PerFileDataCoverage NO_DATA_INFO = new PerFileDataCoverage();

   @Nonnull public PerFileLineCoverage lineCoverageInfo;
   @Nonnull public PerFileDataCoverage dataCoverageInfo;

   // Used for fast indexed access.
   @Nonnegative public final int index;

   // Used for output styling in the HTML report.
   @Nullable public String kindOfTopLevelType;

   // Used to track the last time the ".class" file was modified, to decide if merging can be done.
   long lastModified;

   private final boolean loadedAfterTestCompletion;

   FileCoverageData(@Nonnegative int index, @Nullable String kindOfTopLevelType) {
      this.index = index;
      this.kindOfTopLevelType = kindOfTopLevelType;
      lineCoverageInfo = new PerFileLineCoverage();
      dataCoverageInfo = new PerFileDataCoverage();
      loadedAfterTestCompletion = TestRun.isTerminated();
   }

   boolean wasLoadedAfterTestCompletion() { return loadedAfterTestCompletion; }

   @Nonnull
   public PerFileLineCoverage getLineCoverageData() { return lineCoverageInfo; }

   @Nonnegative public int getTotalItems()   { return lineCoverageInfo.getTotalItems()   + dataCoverageInfo.getTotalItems(); }
   @Nonnegative public int getCoveredItems() { return lineCoverageInfo.getCoveredItems() + dataCoverageInfo.getCoveredItems(); }

   public int getCoveragePercentage() {
      int totalItems = getTotalItems();
      int coveredItems = getCoveredItems();
      return CoveragePercentage.calculate(coveredItems, totalItems);
   }

   void mergeWithDataFromPreviousTestRun(@Nonnull FileCoverageData previousInfo) {
      if (lineCoverageInfo == NO_LINE_INFO) {
         lineCoverageInfo = previousInfo.lineCoverageInfo;
      }
      else if (previousInfo.lineCoverageInfo != NO_LINE_INFO) {
         lineCoverageInfo.mergeInformation(previousInfo.lineCoverageInfo);
      }

      if (dataCoverageInfo == NO_DATA_INFO) {
         dataCoverageInfo = previousInfo.dataCoverageInfo;
      }
      else if (previousInfo.dataCoverageInfo != NO_DATA_INFO) {
         dataCoverageInfo.mergeInformation(previousInfo.dataCoverageInfo);
      }
   }
}
