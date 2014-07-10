/*
 * Copyright (c) 2006-2014 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.coverage.reporting;

import org.jetbrains.annotations.*;

import mockit.coverage.data.*;

public final class FullCoverageReport extends CoverageReport
{
   public FullCoverageReport(
      @NotNull String outputDir, boolean outputDirCreated, @Nullable String[] sourceDirs,
      @NotNull CoverageData coverageData)
   {
      super(outputDir, outputDirCreated, sourceDirs, coverageData, true);
   }
}