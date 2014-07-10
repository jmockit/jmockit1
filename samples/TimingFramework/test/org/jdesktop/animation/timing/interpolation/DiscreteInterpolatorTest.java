/*
 * Copyright (c) 2006-2011 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package org.jdesktop.animation.timing.interpolation;

import org.junit.*;

import static org.junit.Assert.*;

public final class DiscreteInterpolatorTest
{
   @Test
   public void testInterpolate()
   {
      Interpolator interp = DiscreteInterpolator.getInstance();

      assertEquals(0.0f, interp.interpolate(0.0f), 0.0f);
      //noinspection MagicNumber
      assertEquals(0.0f, interp.interpolate(0.2f), 0.0f);
      assertEquals(1.0f, interp.interpolate(1.0f), 0.0f);
   }
}
