/*
 * Copyright (c) 2006-2012 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package org.jdesktop.animation.timing.triggers;

import org.junit.*;

import static org.junit.Assert.*;

public final class MouseTriggerEventTest
{
   @Test
   public void getOppositeEvent()
   {
      assertSame(MouseTriggerEvent.EXIT, MouseTriggerEvent.ENTER.getOppositeEvent());
      assertSame(MouseTriggerEvent.ENTER, MouseTriggerEvent.EXIT.getOppositeEvent());
      assertSame(MouseTriggerEvent.RELEASE, MouseTriggerEvent.PRESS.getOppositeEvent());
      assertSame(MouseTriggerEvent.PRESS, MouseTriggerEvent.RELEASE.getOppositeEvent());
      assertSame(MouseTriggerEvent.CLICK, MouseTriggerEvent.CLICK.getOppositeEvent());
   }
}
