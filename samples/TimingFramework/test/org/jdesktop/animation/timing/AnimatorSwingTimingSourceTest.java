/*
 * Copyright (c) 2006-2015 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package org.jdesktop.animation.timing;

import java.awt.event.*;
import javax.swing.*;

import org.junit.*;
import static org.junit.Assert.*;

import mockit.*;

public final class AnimatorSwingTimingSourceTest
{
   ActionListener timerTarget;

   @Test
   public void timingSourceEventOnSwingTimingSourceForRunningAnimator(@Mocked final TimingTarget timingTarget)
   {
      new MockTimer();

      Animator animator = new Animator(50);
      animator.addTarget(timingTarget);

      new MockUp<System>() {
         @Mock long nanoTime(Invocation inv) { return inv.getInvocationIndex() == 0 ? 0L : 50L * 1000000;}
      };

      animator.start();
      timerTarget.actionPerformed(null);

      new VerificationsInOrder() {{
         timingTarget.begin();
         timingTarget.timingEvent(1.0f);
         timingTarget.end();
      }};

      // Exercise other methods of SwingTimingSource to fully cover the code, verifying through MockTimer.
      animator.setResolution(10);
      animator.setStartDelay(0);
   }

   class MockTimer extends MockUp<Timer>
   {
      @Mock(invocations = 1) // invocation from Animator(d)
      void $init(int delay, ActionListener actionListener)
      {
         assertEquals(20, delay); // 20 is the initial Animator resolution
         assertNotNull(actionListener);
         timerTarget = actionListener;
      }

      @Mock(invocations = 1)
      void start() {}

      @Mock(invocations = 1)
      void stop() {}

      @Mock(invocations = 1) // invocation from animator.setResolution
      void setDelay(int delay)
      {
         assertEquals(10, delay);
      }

      @Mock(invocations = 2) // one invocation from Animator(d), another from animator.setStartDelay
      void setInitialDelay(int initialDelay)
      {
         assertEquals(0, initialDelay);
      }
   }
}
