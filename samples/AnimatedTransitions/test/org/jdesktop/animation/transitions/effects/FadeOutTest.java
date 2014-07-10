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

public final class FadeOutTest
{
   @Tested FadeOut fadeOut;
   @Injectable ComponentState startState;
   @Mocked Animator animator;

   @Test
   public void createsWithGivenStartState()
   {
      assertSame(startState, fadeOut.getStart());
   }

   @Test
   public void addsTransparentAnimationTargetOnInit(@Mocked final Effect mockedBase)
   {
      float initialOpacity = Deencapsulation.getField(fadeOut, float.class);
      assertEquals(0.0f, initialOpacity, 0);

      fadeOut.init(animator, null);

      new Verifications() {{
         animator.addTarget((TimingTarget) withNotNull());
         mockedBase.init(animator, null);
      }};

      float opacityAfterInit = Deencapsulation.getField(fadeOut, float.class);
      assertEquals(1.0f, opacityAfterInit, 0);
   }

   @Test
   public void removesAnimationTargetOnCleanup(@Mocked Effect mockedBase)
   {
      fadeOut.init(animator, null);
      fadeOut.cleanup(animator);

      new Verifications() {{ animator.removeTarget((TimingTarget) withNotNull()); }};
   }
}
