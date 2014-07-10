/*
 * Copyright (c) 2006-2012 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package org.jdesktop.animation.transitions.effects;

import org.jdesktop.animation.timing.*;
import org.jdesktop.animation.timing.interpolation.*;
import org.jdesktop.animation.transitions.*;
import org.junit.*;

import mockit.*;

public final class ScaleTest
{
   @Tested Scale scale;
   @Injectable ComponentState start;
   @Injectable ComponentState end;
   @Mocked Animator animator;
   @Cascading @Mocked Effect base;

   @Test
   public void addsAnimationTargetsOnInit()
   {
      scale.init(animator, null);

      new Verifications() {{
         animator.addTarget((TimingTarget) withNotNull()); times = 2;
         base.init(animator, null);
      }};
   }

   @Test
   public void addsAnimationTargetsOnInitWithParentEffect()
   {
      Effect parentEffect = new Unchanging();

      scale.init(animator, parentEffect);

      new Verifications() {{
         animator.addTarget((TimingTarget) withNotNull()); times = 2;
         base.init(animator, null);
      }};
   }

   @Test
   public void removesAnimationTargetsOnCleanup()
   {
      scale.init(animator, null);
      scale.cleanup(animator);

      new Verifications() {{ animator.removeTarget((TimingTarget) withNotNull()); times = 2; }};
   }
}
