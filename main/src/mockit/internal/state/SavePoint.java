/*
 * Copyright (c) 2006-2014 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.state;

import java.util.*;

import org.jetbrains.annotations.*;

import mockit.internal.*;
import static mockit.internal.expectations.RecordAndReplayExecution.*;

public final class SavePoint
{
   @NotNull private final Set<ClassIdentification> previousTransformedClasses;
   @NotNull private final Map<Class<?>, byte[]> previousRedefinedClasses;
   private final int previousCaptureTransformerCount;
   @NotNull private final List<Class<?>> previousMockedClasses;
   @NotNull private final MockClasses.SavePoint previousMockClasses;

   public SavePoint()
   {
      MockFixture mockFixture = TestRun.mockFixture();
      previousTransformedClasses = mockFixture.getTransformedClasses();
      previousRedefinedClasses = mockFixture.getRedefinedClasses();
      previousCaptureTransformerCount = mockFixture.getCaptureTransformerCount();
      previousMockedClasses = mockFixture.getMockedClasses();
      previousMockClasses = TestRun.getMockClasses().new SavePoint();
   }

   public synchronized void rollback()
   {
      RECORD_OR_REPLAY_LOCKS.get().lock();

      try {
         MockFixture mockFixture = TestRun.mockFixture();
         mockFixture.removeCaptureTransformers(previousCaptureTransformerCount);
         mockFixture.restoreTransformedClasses(previousTransformedClasses);
         mockFixture.restoreRedefinedClasses(previousRedefinedClasses);
         mockFixture.removeMockedClasses(previousMockedClasses);
         previousMockClasses.rollback();
      }
      finally {
         RECORD_OR_REPLAY_LOCKS.get().unlock();
      }
   }
}
