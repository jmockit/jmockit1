/*
 * Copyright (c) 2006-2015 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package org.jdesktop.animation.transitions.effects;

import java.awt.*;

import static org.junit.Assert.*;

import mockit.*;

import org.jdesktop.animation.transitions.*;

public final class EffectSetupVerification extends MockUp<Effect>
{
   private final Graphics2D expectedGraphics;

   EffectSetupVerification(Graphics2D expectedGraphics) { this.expectedGraphics = expectedGraphics; }

   @Mock(invocations = 1)
   public void setup(Graphics2D g2d)
   {
      assertSame(expectedGraphics, g2d);
   }
}
