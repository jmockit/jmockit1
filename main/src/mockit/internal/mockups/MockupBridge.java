/*
 * Copyright (c) 2006-2014 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.mockups;

import java.lang.reflect.*;

import org.jetbrains.annotations.*;

import mockit.internal.*;
import mockit.internal.state.*;

public final class MockupBridge extends MockingBridge
{
   @NotNull public static final MockingBridge MB = new MockupBridge();

   private MockupBridge() { super("$MUB"); }

   @Nullable @Override
   public Object invoke(@Nullable Object mocked, Method method, @NotNull Object[] args) throws Throwable
   {
      String mockClassDesc = (String) args[0];

      if (notToBeMocked(mocked, mockClassDesc)) {
         return false;
      }

      Integer mockStateIndex = (Integer) args[1];
      return TestRun.updateMockState(mockClassDesc, mocked, mockStateIndex);
   }
}
