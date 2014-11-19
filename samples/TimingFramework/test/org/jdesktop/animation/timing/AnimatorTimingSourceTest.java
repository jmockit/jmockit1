/*
 * Copyright (c) 2006-2014 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package org.jdesktop.animation.timing;

import static org.jdesktop.animation.timing.Animator.*;
import static org.junit.Assert.*;
import org.junit.*;

import mockit.*;

public final class AnimatorTimingSourceTest
{
   @Test
   public void setTimer(@Mocked final TimingSource timingSource)
   {
      final Animator animator = new Animator(500);

      animator.setTimer(timingSource);
      animator.start();
      animator.stop();

      // Expectations for setTimer:
      new Verifications() {{
         timingSource.addEventListener(withInstanceOf(TimingEventListener.class));
         timingSource.setResolution(animator.getResolution());
         timingSource.setStartDelay(animator.getStartDelay());
      }};

      // Expectations for notification of events:
      new VerificationsInOrder() {{
         timingSource.start();
         timingSource.stop();
      }};
   }

   @Test
   public void setTimerToCustomTimingSourceThenResetBackToOriginal(@Mocked final TimingSource timingSource)
   {
      Animator animator = new Animator(50);

      animator.setTimer(timingSource);
      animator.setTimer(null);

      // Verifies timer was originally set (non-null):
      new Verifications() {{
         timingSource.addEventListener(withInstanceOf(TimingEventListener.class));
         timingSource.setResolution(anyInt);
         timingSource.setStartDelay(anyInt);
      }};

      // Verifies timer was un-set:
      new Verifications() {{
         timingSource.removeEventListener(withInstanceOf(TimingEventListener.class));
      }};
   }

   @Tested TimingSource timingSource;

   @Test
   public void timingEventOnTimingSource(@Capturing final TimingEventListener timingEventTarget)
   {
      Animator animator = new Animator(50);
      animator.setTimer(timingSource);

      timingSource.timingEvent();

      new Verifications() {{ timingEventTarget.timingSourceEvent(timingSource); }};
   }

   @Test
   public void timingSourceEventOnTimingSourceTargetForNonRunningAnimator(@Mocked final TimingTarget timingTarget)
   {
      Animator animator = new Animator(50);
      animator.addTarget(timingTarget);
      animator.setTimer(timingSource);

      timingSource.timingEvent();

      new Verifications() {{
         timingTarget.timingEvent(0.0f);
         times = 0; // Animator is not running, so no timing event is expected.
      }};
   }

   @Test
   public void timingSourceEventOnTimingSourceTargetForRunningAnimator(@Mocked final TimingTarget timingTarget)
   {
      Animator animator = new Animator(50);
      animator.addTarget(timingTarget);
      animator.setTimer(timingSource);

      animator.start();
      timingSource.timingEvent();

      new VerificationsInOrder() {{
         timingTarget.begin();
         timingTarget.timingEvent(withEqual(0.0f, 0.04));
      }};
   }

   @Test
   public void timingSourceEventOnTimingSourceTargetForRunningAnimatorAtTimeToStop(
      @Mocked System system, @Mocked final TimingTarget timingTarget)
   {
      Animator animator = new Animator(50);
      animator.addTarget(timingTarget);
      animator.setTimer(timingSource);

      new Expectations() {{
         // Consecutive values for the calls to animator.start() then timingSource.timingEvent():
         System.nanoTime(); returns(0L, 50L * 1000000);
      }};

      animator.start();
      timingSource.timingEvent();

      new VerificationsInOrder() {{
         timingTarget.begin();
         timingTarget.timingEvent(1.0F);
         timingTarget.end();
      }};
   }

   @Test
   public void timingSourceEventOnTimingSourceTargetForRunningRepeatingAnimator(
      @Mocked System system, @Mocked final TimingTarget timingTarget)
   {
      Animator animator = new Animator(50, INFINITE, RepeatBehavior.LOOP, null);
      animator.addTarget(timingTarget);
      animator.setTimer(timingSource);

      new Expectations() {{
         System.nanoTime(); returns(0L, 60L * 1000000);
      }};

      animator.start();
      timingSource.timingEvent();

      new VerificationsInOrder() {{
         timingTarget.begin();
         timingTarget.repeat();

         float fraction;
         timingTarget.timingEvent(fraction = withCapture());
         assertTrue(fraction > 0.0f);
      }};
   }
}
