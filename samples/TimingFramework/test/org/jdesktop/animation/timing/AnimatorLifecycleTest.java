/*
 * Copyright (c) 2006-2013 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package org.jdesktop.animation.timing;

import org.junit.*;

import mockit.*;

import static org.jdesktop.animation.timing.Animator.*;
import static org.junit.Assert.*;

public final class AnimatorLifecycleTest
{
   @Capturing TimingSource timer;
   Animator animator;

   @Before
   public void createAnimator()
   {
      animator = new Animator(500);
   }

   @Test
   public void startTheAnimator()
   {
      assertFalse(animator.isRunning());

      animator.start();

      assertTrue(animator.isRunning());
      new Verifications() {{ timer.start(); }};
   }

   @Test
   public void startForwardAtIntermediateFraction()
   {
      animator.setStartFraction(0.2f);

      animator.start();

      assertTrue(animator.isRunning());
      new Verifications() {{ timer.start(); }};
   }

   @Test
   public void startBackwardAtIntermediateFraction()
   {
      animator.setStartDirection(Direction.BACKWARD);
      animator.setStartFraction(0.8f);

      animator.start();

      assertTrue(animator.isRunning());
      new Verifications() {{ timer.start(); }};
   }

   @Test
   public void stopTheAnimator()
   {
      animator.start();
      assertTrue(animator.isRunning());

      animator.stop();
      assertFalse(animator.isRunning());

      new Verifications() {{ timer.stop(); }};
   }

   @Test
   public void cancelTheAnimator()
   {
      animator.start();
      animator.cancel();

      assertFalse(animator.isRunning());
      new Verifications() {{ timer.stop(); }};
   }

   @Test
   public void pauseTheAnimator()
   {
      animator.start();
      animator.pause();

      assertFalse(animator.isRunning());
      new Verifications() {{ timer.stop(); }};
   }

   @Test
   public void resumeTheAnimatorAfterPausingIt()
   {
      animator.start();

      animator.pause();
      assertFalse(animator.isRunning());

      animator.resume();
      assertTrue(animator.isRunning());

      new VerificationsInOrder() {{
         timer.start();
         timer.stop();
         timer.start();
      }};
   }

   @Test(expected = IllegalStateException.class)
   public void changeConfigurationWhileRunning()
   {
      animator.start();
      animator.setDuration(100);
   }
}
