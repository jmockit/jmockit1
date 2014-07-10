/*
 * Copyright (c) 2006-2012 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package org.jdesktop.animation.timing;

import mockit.*;

import org.junit.*;

public final class TimingSourceTest
{
   @Tested TimingSource source;
   @Mocked TimingEventListener timingEventListener;

   @Test
   public void addedEventListenerReceivesTimingEvents()
   {
      source.addEventListener(timingEventListener);
      source.timingEvent();

      new Verifications() {{ timingEventListener.timingSourceEvent(source); }};
   }

   @Test
   public void removedEventListenerNoLongerReceivesTimingEvents()
   {
      source.addEventListener(timingEventListener);
      source.removeEventListener(timingEventListener);
      source.timingEvent();

      new FullVerifications() {};
   }
}
