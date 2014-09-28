/*
 * Copyright (c) 2006-2014 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package org.jdesktop.animation.transitions.effects;

import java.awt.*;
import javax.swing.*;

import org.jdesktop.animation.timing.*;
import org.jdesktop.animation.transitions.*;

import org.junit.*;

import mockit.*;

public final class RotateTest
{
   @Injectable Animator animator;

   @Test
   public void callsSuperOnInit(@Injectable JComponent component)
   {
      final Effect base = new Effect() {};
      new Expectations(Effect.class) {{ base.init(animator, null); times = 1; }};

      new Rotate(30, component).init(animator, null);
   }

   @Test
   public void addsAnimationTargetOnInit(@Injectable ComponentState start, @Injectable ComponentState end)
   {
      Rotate rotate = new Rotate(start, end, 45, 100, 60);

      rotate.init(animator, null);

      new Verifications() {{ animator.addTarget((TimingTarget) withNotNull()); }};
   }

   @Test
   public void removesAnimationTargetOnCleanup(@Mocked Effect mockedBase)
   {
      Rotate rotate = new Rotate(10, 200, 100);

      rotate.init(animator, null);
      rotate.cleanup(animator);

      new Verifications() {{ animator.removeTarget((TimingTarget) withNotNull()); }};
   }

   @Test
   public void rotatesComponentByCurrentAngleOnSetup(@Injectable final Graphics2D g2D)
   {
      final Effect base = new Unchanging();

      new Expectations(Effect.class) {{ base.setup(g2D); times = 1; }};

      Rotate rotate = new Rotate(90, new JButton());
      rotate.setRadians(0.2);

      rotate.setup(g2D);

      new Verifications() {{ g2D.rotate(0.2); }};
   }
}
