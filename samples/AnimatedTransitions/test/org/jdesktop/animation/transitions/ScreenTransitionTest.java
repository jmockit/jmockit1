/*
 * Copyright (c) 2006-2014 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package org.jdesktop.animation.transitions;

import javax.swing.*;

import org.junit.*;

import mockit.*;

import org.jdesktop.animation.timing.*;

public final class ScreenTransitionTest
{
   @Tested ScreenTransition transition;
   @Injectable final JComponent transitionComponent = new JPanel();
   @Injectable TransitionTarget transitionTarget;
   @Injectable final int duration = 5000;
   @Mocked Animator animator;

   @Test
   public void setAnimator_addsAnimationTargetToAnimator()
   {
      transition.setAnimator(animator);

      new Verifications() {{
         animator.addTarget((TimingTarget) withNotNull());
      }};
   }

   @Test(expected = IllegalArgumentException.class)
   public void setAnimatorToNull()
   {
      transition.setAnimator(null);
   }

   @Test(expected = IllegalStateException.class)
   public void setAnimatorWithAnimationAlreadyRunning()
   {
      new Expectations() {{ animator.isRunning(); result = true; }};

      transition.setAnimator(animator);
   }

   @Test
   public void startWithNonRunningAnimator()
   {
      transition.start();

      new Verifications() {{
         animator.stop(); times = 0;
         animator.start();
      }};
   }

   @Test
   public void startWithRunningAnimator()
   {
      new Expectations() {{
         animator.isRunning(); result = true;
      }};

      transition.start();

      new VerificationsInOrder() {{
         animator.stop();
         animator.start();
      }};
   }
}
