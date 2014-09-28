/*
 * Copyright (c) 2006-2014 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package org.jdesktop.animation.transitions.effects;

import org.jdesktop.animation.timing.*;
import org.jdesktop.animation.transitions.*;

import org.junit.*;

import mockit.*;

public final class MoveTest
{
   @Tested Move move;
   @Injectable ComponentState start;
   @Injectable ComponentState end;
   @Injectable Animator animator;

   @Test
   public void callsSuperOnInit()
   {
      final Effect base = new Effect() {};
      new Expectations(Effect.class) {{ base.init(animator, null); times = 1; }};

      move.init(animator, null);
   }

   @Test
   public void addsAnimationTargetOnInitWithoutParentEffect()
   {
      move.init(animator, null);

      new Verifications() {{ animator.addTarget((TimingTarget) withNotNull()); }};
   }

   @Test
   public void addsAnimationTargetOnInitWithParentEffect()
   {
      Effect parentEffect = new Unchanging();

      move.init(animator, parentEffect);

      new Verifications() {{ animator.addTarget((TimingTarget) withNotNull()); }};
   }

   @Test
   public void removesAnimationTargetFromAnimatorOnCleanup()
   {
      move.init(animator, null);
      move.cleanup(animator);

      new Verifications() {{ animator.removeTarget((TimingTarget) withNotNull()); }};
   }
}
