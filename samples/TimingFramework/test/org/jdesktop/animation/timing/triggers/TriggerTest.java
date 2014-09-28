/*
 * Copyright (c) 2006-2014 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package org.jdesktop.animation.timing.triggers;

import org.junit.*;

import mockit.*;

import org.jdesktop.animation.timing.*;

import static org.jdesktop.animation.timing.Animator.Direction;

public final class TriggerTest
{
   @Tested Trigger trigger;
   @Injectable Animator animator;

   @Test
   public void fireTriggerWithoutEventOnNonRunningAnimator()
   {
      trigger.fire();

      new Verifications() {{ animator.start(); }};
   }

   @Test
   public void fireTriggerWithoutEventOnRunningAnimator()
   {
      new Expectations() {{ animator.isRunning(); result = true; }};

      trigger.fire();

      new VerificationsInOrder() {{
         animator.stop();
         animator.start();
      }};
   }

   @Test
   public void fireTriggerWithoutEventAfterDisarmingIt()
   {
      trigger.disarm();
      trigger.fire();

      // Nothing should happen on the animator.
      new FullVerifications() {};
   }

   @Test
   public void fireTriggerWithoutEventWhilePassingACurrentEvent()
   {
      trigger.fire(MouseTriggerEvent.CLICK);

      new FullVerifications() {};
   }

   @Test
   public void fireTriggerWithSpecificEventAfterDisarmingIt()
   {
      TriggerEvent event = FocusTriggerEvent.IN;
      trigger = new Trigger(animator, event);

      trigger.disarm();
      trigger.fire(event);

      new FullVerifications() {};
   }

   @Test
   public void fireTriggerWithSpecificEventAndNotOnAutoReverseWhilePassingAnUnexpectedCurrentEvent()
   {
      TriggerEvent event = MouseTriggerEvent.ENTER;
      trigger = new Trigger(animator, event);

      trigger.fire(MouseTriggerEvent.PRESS);

      new FullVerifications() {};
   }

   @Test
   public void fireTriggerWithSpecificEventAndNotOnAutoReverseOnNonRunningAnimator()
   {
      TriggerEvent event = FocusTriggerEvent.IN;
      trigger = new Trigger(animator, event);

      trigger.fire(event);

      new VerificationsInOrder() {{
         animator.setStartDirection(Direction.FORWARD);

         // The original Timing Framework "fire(event)" method did an inappropriate call to "fire()",
         // which would then recheck disarming of the trigger and that the animator was not running;
         // writing this test revealed this issue, leading to better production code.
         animator.start(); times = 1;
      }};
   }

   @Test
   public void fireTriggerWithSpecificEventAndNotOnAutoReverseOnRunningAnimator()
   {
      TriggerEvent event = FocusTriggerEvent.IN;
      trigger = new Trigger(animator, event, false);

      new Expectations() {{ animator.isRunning(); result = true; }};

      trigger.fire(event);

      new VerificationsInOrder() {{
         animator.stop();
         animator.setStartDirection(Direction.FORWARD);
         animator.start(); times = 1;
      }};
   }

   @Test
   public void fireTriggerWithSpecificEventAndOnAutoReverseOnNonRunningAnimator()
   {
      TriggerEvent event = FocusTriggerEvent.IN;
      trigger = new Trigger(animator, event, true);

      trigger.fire(event);

      new VerificationsInOrder() {{
         animator.setStartFraction(0.0f);
         animator.setStartDirection(Direction.FORWARD);
         animator.start();
      }};
   }

   @Test
   public void fireTriggerWithSpecificEventAndOnAutoReverseOnRunningAnimator()
   {
      TriggerEvent event = FocusTriggerEvent.IN;
      trigger = new Trigger(animator, event, true);
      final float timingFraction = 0.2f;

      new Expectations() {{
         animator.isRunning(); result = true;
         animator.getTimingFraction(); result = timingFraction;
      }};

      trigger.fire(event);

      new VerificationsInOrder() {{
         animator.stop();
         animator.setStartFraction(timingFraction);
         animator.setStartDirection(Direction.FORWARD);
         animator.start();
      }};
   }

   @Test
   public void fireTriggerWithOppositeEventAndNotOnAutoReverseOnNonRunningAnimator()
   {
      TriggerEvent event = FocusTriggerEvent.IN;
      trigger = new Trigger(animator, event, false);

      trigger.fire(FocusTriggerEvent.OUT);

      new FullVerifications() {};
   }

   @Test
   public void fireTriggerWithOppositeEventAndOnAutoReverseOnNonRunningAnimator()
   {
      TriggerEvent event = FocusTriggerEvent.IN;
      trigger = new Trigger(animator, event, true);

      new Expectations() {{
         animator.getStartFraction(); result = 0.2f;
      }};

      trigger.fire(event.getOppositeEvent());

      new VerificationsInOrder() {{
         animator.setStartFraction(0.8f);
         animator.setStartDirection(Direction.BACKWARD);
         animator.start(); times = 1;
      }};
   }

   @Test
   public void fireTriggerWithOppositeEventAndOnAutoReverseOnRunningAnimator()
   {
      TriggerEvent event = FocusTriggerEvent.IN;
      trigger = new Trigger(animator, event, true);
      final float timingFraction = 0.2f;

      new Expectations() {{
         animator.isRunning(); result = true;
         animator.getTimingFraction(); result = timingFraction;
      }};

      trigger.fire(event.getOppositeEvent());

      new VerificationsInOrder() {{
         animator.stop();
         animator.setStartFraction(timingFraction);
         animator.setStartDirection(Direction.BACKWARD);
         animator.start(); times = 1;
      }};
   }
}
