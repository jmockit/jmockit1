/*
 * Copyright (c) 2006-2012 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package org.jdesktop.animation.transitions.effects;

import java.awt.*;

import org.jdesktop.animation.transitions.*;

import org.junit.*;
import static org.junit.Assert.*;

import mockit.*;

public final class FadeTest
{
   @Tested Fade fade;

   @Test
   public void createsAlphaCompositeWithCurrentOpacityOnSetup(
      @Injectable final Graphics2D g2D, @Mocked final Effect effect)
   {
      final float opacity = 0.5f;
      fade.setOpacity(opacity);

      fade.setup(g2D);

      new Verifications() {{
         AlphaComposite composite;
         g2D.setComposite(composite = withCapture());
         assertEquals(opacity, composite.getAlpha(), 0);

         effect.setup(g2D);
      }};
   }
}
