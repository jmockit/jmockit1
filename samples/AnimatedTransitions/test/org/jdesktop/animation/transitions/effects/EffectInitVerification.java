/*
 * Copyright (c) 2006-2015 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package org.jdesktop.animation.transitions.effects;

import static org.junit.Assert.*;

import mockit.*;

import org.jdesktop.animation.timing.*;
import org.jdesktop.animation.transitions.*;

public final class EffectInitVerification extends MockUp<Effect>
{
   private final Animator expectedAnimator;

   EffectInitVerification(Animator expectedAnimator) { this.expectedAnimator = expectedAnimator; }

   @Mock(invocations = 1)
   public void init(Animator animator, Effect parent)
   {
      assertSame(expectedAnimator, animator);
      assertNull(parent);
   }
}
