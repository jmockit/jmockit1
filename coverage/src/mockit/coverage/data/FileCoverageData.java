/*
 * Copyright (c) 2006-2015 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.coverage.data;

import java.io.*;
import java.util.*;
import javax.annotation.*;

import mockit.coverage.*;
import mockit.coverage.dataItems.*;
import mockit.coverage.lines.*;
import mockit.coverage.paths.*;

/**
 * Coverage data gathered for the lines and branches of a single source file.
 */
public final class FileCoverageData implements Serializable
{
   private static final long serialVersionUID = 3508572808457541012L;

   @Nonnull private static final PerFileLineCoverage NO_LINE_INFO = new PerFileLineCoverage();
   @Nonnull private static final PerFilePathCoverage NO_PATH_INFO = new PerFilePathCoverage();
   @Nonnull private static final PerFileDataCoverage NO_DATA_INFO = new PerFileDataCoverage();

   @Nonnull public PerFileLineCoverage lineCoverageInfo;
   @Nonnull public PerFilePathCoverage pathCoverageInfo;
   @Nonnull public PerFileDataCoverage dataCoverageInfo;

   // Used for fast indexed access.
   public final int index;

   // Used for output styling in the HTML report:
   @Nullable public String kindOfTopLevelType;

   // Used to track the last time the ".class" file was modified, to decide if merging can be done:
   long lastModified;

   private final boolean loadedAfterTestCompletion;

   public FileCoverageData(int index, @Nullable String kindOfTopLevelType)
   {
      this.index = index;
      this.kindOfTopLevelType = kindOfTopLevelType;
      lineCoverageInfo = Metrics.LineCoverage.active ? new PerFileLineCoverage() : NO_LINE_INFO;
      pathCoverageInfo = Metrics.PathCoverage.active ? new PerFilePathCoverage() : NO_PATH_INFO;
      dataCoverageInfo = Metrics.DataCoverage.active ? new PerFileDataCoverage() : NO_DATA_INFO;
      loadedAfterTestCompletion = TestRun.isTerminated();
   }

   public boolean wasLoadedAfterTestCompletion() { return loadedAfterTestCompletion; }

   @Nonnull
   public PerFileLineCoverage getLineCoverageData() { return lineCoverageInfo; }

   public void addMethod(@Nonnull MethodCoverageData methodData) { pathCoverageInfo.addMethod(methodData); }

   @Nonnull
   public Collection<MethodCoverageData> getMethods() { return pathCoverageInfo.firstLineToMethodData.values(); }

   @Nonnull
   public PerFileCoverage getPerFileCoverage(@Nonnull Metrics metric)
   {
      switch (metric) {
         case LineCoverage: return lineCoverageInfo;
         case PathCoverage: return pathCoverageInfo;
         default: return dataCoverageInfo;
      }
   }

   public int getTotalItemsForAllMetrics()
   {
      int totalItems = 0;

      if (lineCoverageInfo != NO_LINE_INFO) {
         totalItems += lineCoverageInfo.getTotalItems();
      }

      if (pathCoverageInfo != NO_PATH_INFO) {
         totalItems += pathCoverageInfo.getTotalItems();
      }

      if (dataCoverageInfo != NO_DATA_INFO) {
         totalItems += dataCoverageInfo.getTotalItems();
      }

      return totalItems;
   }

   void mergeWithDataFromPreviousTestRun(@Nonnull FileCoverageData previousInfo)
   {
      if (lineCoverageInfo == NO_LINE_INFO) {
         lineCoverageInfo = previousInfo.lineCoverageInfo;
      }
      else if (previousInfo.lineCoverageInfo != NO_LINE_INFO) {
         lineCoverageInfo.mergeInformation(previousInfo.lineCoverageInfo);
      }

      if (pathCoverageInfo == NO_PATH_INFO) {
         pathCoverageInfo = previousInfo.pathCoverageInfo;
      }
      else if (previousInfo.pathCoverageInfo != NO_PATH_INFO) {
         pathCoverageInfo.mergeInformation(previousInfo.pathCoverageInfo);
      }

      if (dataCoverageInfo == NO_DATA_INFO) {
         dataCoverageInfo = previousInfo.dataCoverageInfo;
      }
      else if (previousInfo.dataCoverageInfo != NO_DATA_INFO) {
         dataCoverageInfo.mergeInformation(previousInfo.dataCoverageInfo);
      }
   }

   void reset()
   {
      lineCoverageInfo.reset();
      pathCoverageInfo.reset();
   }
}
