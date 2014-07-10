/*
 * Copyright (c) 2006-2012 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package org.jdesktop.animation.transitions.effects;

import org.jdesktop.animation.timing.*;
import org.jdesktop.animation.transitions.*;

import org.junit.*;
import static org.junit.Assert.*;

import mockit.*;

public final class FadeInTest
{
   @Tested FadeIn fadeIn;
   @Injectable ComponentState endState;
   @Mocked Animator animator;

   @Test
   public void createsWithGivenEndState()
   {
      assertSame(endState, fadeIn.getEnd());
   }

   @Test
   public void addsTransparentAnimationTargetOnInit(@Mocked final Effect mockedBase)
   {
      Deencapsulation.setField(fadeIn, 1.0f);

      fadeIn.init(animator, null);

      new Verifications() {{
         animator.addTarget((TimingTarget) withNotNull());
         mockedBase.init(animator, null);
      }};

      float initialOpacity = Deencapsulation.getField(fadeIn, float.class);
      assertEquals(0.0f, initialOpacity, 0);
   }

   @Test
   public void removesAnimationTargetOnCleanup(@Mocked Effect mockedBase)
   {
      fadeIn.init(animator, null);
      fadeIn.cleanup(animator);

      new Verifications() {{ animator.removeTarget((TimingTarget) withNotNull()); }};
   }
}
