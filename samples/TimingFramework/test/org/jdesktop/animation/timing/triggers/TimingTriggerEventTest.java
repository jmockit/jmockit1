/*
 * Copyright (c) 2006-2012 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package org.jdesktop.animation.timing.triggers;

import org.junit.*;

import static org.junit.Assert.*;

public final class TimingTriggerEventTest
{
   @Test
   public void getOppositeEvent()
   {
      assertSame(TimingTriggerEvent.STOP, TimingTriggerEvent.START.getOppositeEvent());
      assertSame(TimingTriggerEvent.START, TimingTriggerEvent.STOP.getOppositeEvent());
      assertSame(TimingTriggerEvent.REPEAT, TimingTriggerEvent.REPEAT.getOppositeEvent());
   }
}
