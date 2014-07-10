/*
 * Copyright (c) 2006-2011 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package org.jdesktop.animation.timing;

import org.junit.*;

/**
 * Created only to achieve full code coverage. Unfortunately, there is no way to actually verify
 * that the methods in the adapter class do nothing.
 */
public final class TimingTargetAdapterTest
{
   private final TimingTarget adapter = new TimingTargetAdapter();

   @Test
   public void testTimingEvent()
   {
      adapter.timingEvent(0.5f);
   }

   @Test
   public void testBegin()
   {
      adapter.begin();
   }

   @Test
   public void testEnd()
   {
      adapter.end();
   }

   @Test
   public void testRepeat()
   {
      adapter.repeat();
   }
}
