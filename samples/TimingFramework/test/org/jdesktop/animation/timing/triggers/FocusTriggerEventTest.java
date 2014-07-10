/*
 * Copyright (c) 2006-2012 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package org.jdesktop.animation.timing.triggers;

import static org.junit.Assert.*;

import org.junit.*;

public final class FocusTriggerEventTest
{
   @Test
   public void getOppositeEvent()
   {
      assertSame(FocusTriggerEvent.OUT, FocusTriggerEvent.IN.getOppositeEvent());
      assertSame(FocusTriggerEvent.IN, FocusTriggerEvent.OUT.getOppositeEvent());
   }
}
