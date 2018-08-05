/*
 * Copyright (c) 2006 JMockit developers
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.expectations;

import java.util.*;
import javax.annotation.*;

abstract class Phase
{
   @Nonnull final RecordAndReplayExecution recordAndReplay;

   Phase(@Nonnull RecordAndReplayExecution recordAndReplay) { this.recordAndReplay = recordAndReplay; }

   @Nonnull
   public final Map<Object, Object> getInstanceMap() { return recordAndReplay.executionState.equivalentInstances.instanceMap; }

   @Nonnull
   final Map<Object, Object> getReplacementMap() { return recordAndReplay.executionState.equivalentInstances.replacementMap; }

   @Nullable
   abstract Object handleInvocation(
      @Nullable Object mock, int mockAccess, @Nonnull String mockClassDesc, @Nonnull String mockNameAndDesc,
      @Nullable String genericSignature, boolean withRealImpl, @Nonnull Object[] args
   ) throws Throwable;
}
