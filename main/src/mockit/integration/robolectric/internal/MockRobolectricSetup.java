/*
 * Copyright (c) 2006-2014 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.integration.robolectric.internal;

import mockit.*;

@SuppressWarnings({"rawtypes", "unchecked"})
public final class MockRobolectricSetup extends MockUp
{
   public MockRobolectricSetup() throws ClassNotFoundException
   {
      super(Class.forName("org.robolectric.bytecode.Setup"));
   }

   @Mock
   public boolean shouldAcquire(Invocation inv, String name)
   {
      if (name.startsWith("mockit.")) {
         return false;
      }

      return inv.proceed();
   }
}
