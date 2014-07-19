/*
 * Copyright (c) 2006-2014 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.expectations.mocking;

import java.lang.reflect.*;

import org.jetbrains.annotations.*;

import mockit.internal.*;
import mockit.internal.expectations.*;
import mockit.internal.state.*;
import mockit.internal.util.*;

public final class MockedBridge extends MockingBridge
{
   @NotNull public static final MockingBridge MB = new MockedBridge();

   private MockedBridge() { super("$MB"); }

   @Nullable @Override
   public Object invoke(@Nullable Object mocked, Method method, @NotNull Object[] args) throws Throwable
   {
      String mockedClassDesc = (String) args[1];

      if (notToBeMocked(mocked, mockedClassDesc)) {
         return Void.class;
      }

      String mockName = (String) args[2];
      String mockDesc = (String) args[3];
      String mockNameAndDesc = mockName + mockDesc;
      int executionMode = (Integer) args[5];
      Object[] mockArgs = extractMockArguments(6, args);

      boolean lockHeldByCurrentThread = RecordAndReplayExecution.RECORD_OR_REPLAY_LOCK.isHeldByCurrentThread();

      if (lockHeldByCurrentThread && mocked != null && executionMode == 3) {
         Object rv = ObjectMethods.evaluateOverride(mocked, mockNameAndDesc, args);

         if (rv != null) {
            return rv;
         }
      }

      if (TestRun.getExecutingTest().isProceedingIntoRealImplementation() || TestRun.isInsideNoMockingZone()) {
         return Void.class;
      }

      String genericSignature = (String) args[4];

      if (lockHeldByCurrentThread && executionMode == 3) {
         return RecordAndReplayExecution.defaultReturnValue(
            mocked, mockedClassDesc, mockNameAndDesc, genericSignature, 1, mockArgs);
      }

      int mockAccess = (Integer) args[0];

      TestRun.enterNoMockingZone();

      try {
         return RecordAndReplayExecution.recordOrReplay(
            mocked, mockAccess, mockedClassDesc, mockNameAndDesc, genericSignature, executionMode, mockArgs);
      }
      finally {
         TestRun.exitNoMockingZone();
      }
   }
}
