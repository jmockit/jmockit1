/*
 * Copyright (c) 2006-2014 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.expectations;

import java.util.*;

import org.jetbrains.annotations.*;

abstract class Phase
{
   @NotNull final RecordAndReplayExecution recordAndReplay;

   Phase(@NotNull RecordAndReplayExecution recordAndReplay) { this.recordAndReplay = recordAndReplay; }

   @NotNull
   public final Map<Object, Object> getInstanceMap() { return recordAndReplay.executionState.instanceMap; }

   @NotNull
   final Map<Object, Object> getReplacementMap() { return recordAndReplay.executionState.replacementMap; }

   @Nullable
   abstract Object handleInvocation(
      @Nullable Object mock, int mockAccess, @NotNull String mockClassDesc, @NotNull String mockNameAndDesc,
      @Nullable String genericSignature, boolean withRealImpl, @NotNull Object[] args)
      throws Throwable;
}
