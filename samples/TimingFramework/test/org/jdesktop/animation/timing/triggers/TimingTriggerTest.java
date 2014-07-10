/*
 * Copyright (c) 2006-2013 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package org.jdesktop.animation.timing.triggers;

import org.jdesktop.animation.timing.*;

import org.junit.*;
import static org.junit.Assert.*;

import mockit.*;

import static mockit.Deencapsulation.*;

public final class TimingTriggerTest
{
   @Test
   public void addTrigger(@Mocked final Animator source, @Mocked final Animator target)
   {
      final TimingTriggerEvent event = TimingTriggerEvent.START;

      final TimingTrigger triggerAdded = TimingTrigger.addTrigger(source, target, event);

      assertAddedTrigger(triggerAdded, source, target, event, false);
   }

   void assertAddedTrigger(
      final TimingTrigger triggerAdded, final Animator source, Animator target, TriggerEvent event, boolean autoReverse)
   {
      assertNotNull(triggerAdded);

      Animator animatorInNewTrigger = getField(triggerAdded, Animator.class);
      assertSame(target, animatorInNewTrigger);

      final TriggerEvent eventInNewTrigger = getField(triggerAdded, TriggerEvent.class);
      assertSame(event, eventInNewTrigger);

      boolean autoReverseInNewTrigger = getField(triggerAdded, "autoReverse");
      assertEquals(autoReverse, autoReverseInNewTrigger);

      new Verifications() {{
         TimingTrigger trigger;
         source.addTarget(trigger = withCapture());
         assertSame(triggerAdded, trigger);
      }};
   }

   @Test
   public void addTriggerWithAutoReverse(@Mocked final Animator source, @Mocked final Animator target)
   {
      final TimingTriggerEvent event = TimingTriggerEvent.STOP;

      TimingTrigger triggerAdded = TimingTrigger.addTrigger(source, target, event, true);

      assertAddedTrigger(triggerAdded, source, target, event, true);
   }

   @Test
   public void begin(@Mocked final Trigger base)
   {
      TimingTrigger timingTrigger = new TimingTrigger(null, TimingTriggerEvent.START);

      timingTrigger.begin();

      new Verifications() {{ base.fire(TimingTriggerEvent.START); }};
   }

   @Test
   public void end(@Mocked final Trigger base)
   {
      TimingTrigger timingTrigger = new TimingTrigger(null, TimingTriggerEvent.STOP);

      timingTrigger.end();

      new Verifications() {{ base.fire(TimingTriggerEvent.STOP); }};
   }

   @Test
   public void repeat(@Mocked final Trigger base)
   {
      TimingTrigger timingTrigger = new TimingTrigger(null, TimingTriggerEvent.REPEAT);

      timingTrigger.repeat();

      new Verifications() {{ base.fire(TimingTriggerEvent.REPEAT); }};
   }

   @Test
   public void timingEvent(@Mocked Trigger base)
   {
      TimingTrigger timingTrigger = new TimingTrigger(null, TimingTriggerEvent.STOP);

      timingTrigger.timingEvent(0.0f);

      // Makes sure no methods were called on the base class.
      new FullVerifications() {};
   }
}
