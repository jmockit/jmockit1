/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.faking;

import java.lang.reflect.*;
import javax.annotation.*;

import mockit.internal.*;
import mockit.internal.state.*;

public final class FakeBridge extends MockingBridge
{
   @Nonnull public static final MockingBridge MB = new FakeBridge();

   private FakeBridge() { super("$FB"); }

   @Nonnull @Override
   public Object invoke(@Nullable Object faked, Method method, @Nonnull Object[] args) throws Throwable
   {
      if (TestRun.isInsideNoMockingZone()) {
         return false;
      }

      TestRun.enterNoMockingZone();

      try {
         String fakeClassDesc = (String) args[0];

         if (notToBeMocked(faked, fakeClassDesc)) {
            return false;
         }

         Integer mockStateIndex = (Integer) args[1];
         return TestRun.updateFakeState(fakeClassDesc, faked, mockStateIndex);
      }
      finally {
         TestRun.exitNoMockingZone();
      }
   }
}
