/*
 * Copyright (c) 2006-2014 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.coverage.lines;

import org.jetbrains.annotations.*;

import mockit.external.asm.*;

/**
 * Coverage data gathered for a branch inside a line of source code.
 */
public final class BranchCoverageData extends LineSegmentData
{
   private static final long serialVersionUID = 1003335601845442606L;

   @NotNull final transient Label label;

   BranchCoverageData(@NotNull Label label) { this.label = label; }

   @Nullable Integer getLine() { return (Integer) label.info; }
}
