/*
 * Copyright (c) 2006-2013 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package org.jdesktop.animation.timing;

import org.junit.*;

import mockit.*;

import static org.jdesktop.animation.timing.Animator.*;
import org.jdesktop.animation.timing.interpolation.*;
import static org.junit.Assert.*;

public final class AnimatorInitializationTest
{
   @Test
   public void getDuration()
   {
      assertEquals(500, new Animator(500).getDuration());
   }

   @Test
   public void setDuration()
   {
      Animator animator = new Animator(500);
      animator.setDuration(300);
      assertEquals(300, animator.getDuration());
   }

   @Test
   public void getStartDirection()
   {
      assertSame(Direction.FORWARD, new Animator(500).getStartDirection());
   }

   @Test
   public void setStartDirection()
   {
      Animator animator = new Animator(500);
      animator.setStartDirection(Direction.BACKWARD);
      assertSame(Direction.BACKWARD, animator.getStartDirection());
   }

   @Test
   public void getInterpolator()
   {
      assertSame(LinearInterpolator.getInstance(), new Animator(500).getInterpolator());
   }

   @Test
   public void setInterpolator()
   {
      Animator animator = new Animator(500);
      DiscreteInterpolator interpolator = DiscreteInterpolator.getInstance();

      animator.setInterpolator(interpolator);

      assertSame(interpolator, animator.getInterpolator());
   }

   @Test
   public void getAcceleration()
   {
      assertEquals(0, new Animator(500).getAcceleration(), 0);
   }

   @Test
   public void setAcceleration()
   {
      Animator animator = new Animator(500);
      animator.setAcceleration(0.2f);
      assertEquals(0.2f, animator.getAcceleration(), 0);
   }

   @Test(expected = IllegalArgumentException.class)
   public void setInvalidAcceleration()
   {
      Animator animator = new Animator(500);
      animator.setAcceleration(1.2f);
   }

   @Test(expected = IllegalArgumentException.class)
   public void setAccelerationIncompatibleWithDeceleration()
   {
      Animator animator = new Animator(500);
      animator.setDeceleration(0.6f);
      animator.setAcceleration(0.5f);
   }

   @Test
   public void getDeceleration()
   {
      assertEquals(0, new Animator(500).getDeceleration(), 0);
   }

   @Test
   public void setDeceleration()
   {
      Animator animator = new Animator(500);
      animator.setDeceleration(0.2f);
      assertEquals(0.2f, animator.getDeceleration(), 0);
   }

   @Test(expected = IllegalArgumentException.class)
   public void setInvalidDeceleration()
   {
      Animator animator = new Animator(500);
      animator.setDeceleration(1.2f);
   }

   @Test(expected = IllegalArgumentException.class)
   public void setDecelerationIncompatibleWithAcceleration()
   {
      Animator animator = new Animator(500);
      animator.setAcceleration(0.5f);
      animator.setDeceleration(0.6f);
   }

   @Test
   public void fullConstructor()
   {
      Animator animator = new Animator(250, 3, RepeatBehavior.LOOP, null);

      assertEquals(250, animator.getDuration());
      assertEquals(3, animator.getRepeatCount(), 0);
      assertSame(RepeatBehavior.LOOP, animator.getRepeatBehavior());
   }

   @Test
   public void fullConstructorWithDefaultRepeatBehavior()
   {
      Animator animator = new Animator(250, 3, null, null);

      assertSame(RepeatBehavior.REVERSE, animator.getRepeatBehavior());
   }

   @Test
   public void setRepeatCount()
   {
      Animator animator = new Animator(250);
      animator.setRepeatCount(5);
      assertEquals(5, animator.getRepeatCount(), 0);
   }

   @Test(expected = IllegalArgumentException.class)
   public void setInvalidRepeatCount()
   {
      Animator animator = new Animator(250);
      animator.setRepeatCount(-5);
   }

   @Test
   public void getResolution()
   {
      assertEquals(20, new Animator(500).getResolution());
   }

   @Test
   public void setResolution()
   {
      Animator animator = new Animator(500);
      animator.setResolution(30);
      assertEquals(30, animator.getResolution());
   }

   @Test(expected = IllegalArgumentException.class)
   public void setInvalidResolution()
   {
      Animator animator = new Animator(500);
      animator.setResolution(-10);
   }

   @Test
   public void getStartDelay()
   {
      assertEquals(0, new Animator(500).getStartDelay());
   }

   @Test
   public void setStartDelay()
   {
      Animator animator = new Animator(500);
      animator.setStartDelay(40);
      assertEquals(40, animator.getStartDelay());
   }

   @Test(expected = IllegalArgumentException.class)
   public void setInvalidStartDelay()
   {
      Animator animator = new Animator(500);
      animator.setStartDelay(-4);
   }

   @Test
   public void getRepeatBehavior()
   {
      assertSame(RepeatBehavior.REVERSE, new Animator(500).getRepeatBehavior());
   }

   @Test
   public void setRepeatBehavior()
   {
      Animator animator = new Animator(500);
      animator.setRepeatBehavior(RepeatBehavior.LOOP);
      assertSame(RepeatBehavior.LOOP, animator.getRepeatBehavior());
   }

   @Test
   public void setDefaultRepeatBehavior()
   {
      Animator animator = new Animator(500);
      animator.setRepeatBehavior(null);
      assertSame(RepeatBehavior.REVERSE, animator.getRepeatBehavior());
   }

   @Test
   public void getEndBehavior()
   {
      assertSame(EndBehavior.HOLD, new Animator(500).getEndBehavior());
   }

   @Test
   public void setEndBehavior()
   {
      Animator animator = new Animator(500);
      animator.setEndBehavior(EndBehavior.RESET);
      assertSame(EndBehavior.RESET, animator.getEndBehavior());
   }

   @Test
   public void getStartFraction()
   {
      assertEquals(0, new Animator(500).getStartFraction(), 0);
   }

   @Test
   public void setStartFraction()
   {
      Animator animator = new Animator(500);
      animator.setStartFraction(0.1f);
      assertEquals(0.1f, animator.getStartFraction(), 0);
   }

   @Test(expected = IllegalArgumentException.class)
   public void setNegativeStartFraction()
   {
      Animator animator = new Animator(500);
      animator.setStartFraction(-0.1f);
   }

   @Test
   public void addTarget(@Mocked final TimingTarget target)
   {
      final Animator animator = new Animator(500);

      animator.addTarget(target);

      // Causes an event to be sent to the timing target:
      animator.stop();

      new Verifications() {{ target.end(); }};
   }

   @Test
   public void removeTarget(@Mocked final TimingTarget target)
   {
      final Animator animator = new Animator(500);

      animator.addTarget(target);

      // Verify the target indirectly:
      animator.removeTarget(target);
      animator.stop();

      new Verifications() {{
         target.end(); times = 0;
      }};
   }
}
