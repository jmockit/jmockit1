/*
 * Copyright (c) 2006-2014 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package org.jdesktop.animation.timing;

import mockit.*;
import static org.jdesktop.animation.timing.Animator.*;
import org.junit.*;
import static org.junit.Assert.*;

public final class AnimatorRunningTest
{
   static long currentTimeInMillis;
   Animator animator;

   @BeforeClass
   public static void setUpMocks()
   {
      new MockUp<System>() {
         @Mock
         long nanoTime()
         {
            // Checks the current thread to avoid problems when called from the EDT.
            String currentThread = Thread.currentThread().getName();
            long timeInMillis = "main".equals(currentThread) ? currentTimeInMillis : System.currentTimeMillis();

            return timeInMillis * 1000000L;
         }
      };
   }

   @Before
   public void setUp()
   {
      currentTimeInMillis = 0;
      animator = new Animator(500);
   }

   @Test
   public void getTotalElapsedTimeFromGivenTime()
   {
      currentTimeInMillis = 0;

      // Just to check the initial state:
      assertEquals(0, animator.getTotalElapsedTime(0));

      animator.start();

      long elapsedTime = animator.getTotalElapsedTime(10);
      assertEquals(10, elapsedTime);
   }

   @Test
   public void getTotalElapsedTimeFromCurrentTime()
   {
      assertEquals(0, animator.getTotalElapsedTime(0));

      animator.start();
      currentTimeInMillis = 10;

      long elapsedTime = animator.getTotalElapsedTime();
      assertEquals(10, elapsedTime);
   }

   @Test
   public void getCycleElapsedTimeFromGivenTime()
   {
      currentTimeInMillis = 0;
      assertEquals(0, animator.getCycleElapsedTime(0));

      animator.start();

      long elapsedTime = animator.getCycleElapsedTime(10);
      assertEquals(10, elapsedTime);
   }

   @Test
   public void getCycleElapsedTimeFromCurrentTime()
   {
      currentTimeInMillis = 0;
      assertEquals(0, animator.getCycleElapsedTime(0));

      animator.start();
      currentTimeInMillis = 18;

      long elapsedTime = animator.getCycleElapsedTime();
      assertEquals(18, elapsedTime);
   }

   @Test
   public void getTimingFractionAtStartWithInfiniteDurationForward()
   {
      currentTimeInMillis = 0;
      animator.setDuration(INFINITE);
      animator.start();
      assertEquals(0, animator.getTimingFraction(), 0);
   }

   @Test
   public void getTimingFractionAtStartWithFiniteDurationForward()
   {
      currentTimeInMillis = 0;
      animator.start();
      assertEquals(0, animator.getTimingFraction(), 0.01);
   }

   @Test
   public void getTimingFractionAtStartWithFiniteDurationBackward()
   {
      currentTimeInMillis = 0;
      animator.setStartDirection(Direction.BACKWARD);
      animator.setStartFraction(1.0f);
      animator.start();
      assertEquals(1, animator.getTimingFraction(), 0.01);
   }

   @Test
   public void getTimingFractionAtEndWithFiniteDurationForward()
   {
      currentTimeInMillis = 0;
      animator.setDuration(5);
      animator.start();
      currentTimeInMillis = 5;
      assertEquals(1, animator.getTimingFraction(), 0.01);
   }

   @Test
   public void getTimingFractionAtStartWithAccelerationAndFiniteDurationForward()
   {
      animator.setAcceleration(0.1f);
      animator.start();
      assertEquals(0, animator.getTimingFraction(), 0.01);
   }

   @Test
   public void getTimingFractionAtStartWithDecelerationAndFiniteDurationForward()
   {
      animator.setDeceleration(0.2f);
      animator.start();
      assertEquals(0, animator.getTimingFraction(), 0.01);
   }

   @Test
   public void getTimingFractionAtEndWithDecelerationAndFiniteDurationForward()
   {
      currentTimeInMillis = 0;
      animator.setDuration(5);
      animator.setDeceleration(0.2f);
      animator.start();
      currentTimeInMillis = 5;
      assertEquals(1, animator.getTimingFraction(), 0.01);
   }

   @Test
   public void getTimingFractionAfterFirstCycleWithFiniteDurationForward()
   {
      currentTimeInMillis = 0;
      animator.setDuration(20);
      animator.setRepeatCount(2);
      animator.start();
      currentTimeInMillis = 30;
      assertEquals(0.5, animator.getTimingFraction(), 0.01);
   }

   @Test
   public void getTimingFractionAfterFirstCycleWithFiniteDurationBackward()
   {
      currentTimeInMillis = 0;
      animator.setDuration(20);
      animator.setRepeatCount(2);
      animator.setStartDirection(Direction.BACKWARD);
      animator.setStartFraction(1.0f);
      animator.start();
      currentTimeInMillis = 30;
      assertEquals(0.5, animator.getTimingFraction(), 0.01);
   }

   @Test
   public void getTimingFractionAtEndWithFiniteDurationForwardAndResetting()
   {
      currentTimeInMillis = 0;
      animator.setDuration(20);
      animator.setEndBehavior(EndBehavior.RESET);
      animator.start();
      currentTimeInMillis = 21;
      assertEquals(0, animator.getTimingFraction(), 0.01);
   }

   @Test
   public void getTimingFractionAtEndWithFiniteDurationForwardAndFractionalRepeatCount()
   {
      currentTimeInMillis = 0;
      animator.setDuration(20);
      animator.setRepeatCount(1.5);
      animator.start();
      currentTimeInMillis = 31;
      assertEquals(1, animator.getTimingFraction(), 0.01);
   }

   @Test
   public void getTimingFractionAtEndWithFiniteDurationBackward()
   {
      currentTimeInMillis = 0;
      animator.setDuration(20);
      animator.setStartDirection(Direction.BACKWARD);
      animator.setStartFraction(1.0f);
      animator.start();
      currentTimeInMillis = 21;
      assertEquals(0, animator.getTimingFraction(), 0.01);
   }

   // This test is for a private method in the Animator class. Usually, such methods should be covered through other
   // tests targeted at the public methods which use the private one. In this case, however, there was no obvious way
   // to fully exercise the private method except by calling it directly.
   @Test
   public void testClampedBetweenZeroAndOne()
   {
      Float minFraction = Deencapsulation.invoke(animator, "clampedBetweenZeroAndOne", -0.02f);
      assertEquals(0.0, minFraction.doubleValue(), 0.0);

      Float maxFraction = Deencapsulation.invoke(animator, "clampedBetweenZeroAndOne", 1.02f);
      assertEquals(1.0, maxFraction.doubleValue(), 0.0);
   }
}
