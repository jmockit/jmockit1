/*
 * Copyright (c) 2006-2013 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package org.jdesktop.animation.transitions;

import javax.swing.*;

import org.jdesktop.animation.timing.*;

import org.junit.*;
import static org.junit.Assert.*;

import mockit.*;

public final class ScreenTransitionInitializationTest
{
   @Mocked JComponent container;
   @Mocked TransitionTarget target;

   @Test
   public void createScreenTransitionWithNonRunningAnimator(@Mocked Animator animator)
   {
      ScreenTransition st = new ScreenTransition(container, target, animator);

      assertSame(animator, st.getAnimator());
   }

   @Test
   public void createScreenTransitionForGivenDuration()
   {
      final int duration = 100;

      ScreenTransition st = new ScreenTransition(container, target, duration);

      Animator animatorCreated = st.getAnimator();
      assertNotNull(animatorCreated);
      assertEquals(duration, animatorCreated.getDuration());
   }
}
