/*
 * Copyright (c) 2006-2011 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package org.jdesktop.animation.timing.interpolation;

import org.junit.*;

import static org.junit.Assert.*;

public final class LinearInterpolatorTest
{
   @Test
   public void testInterpolate()
   {
      Interpolator interp = LinearInterpolator.getInstance();

      assertEquals(0.0f, interp.interpolate(0.0f), 0.0f);
      assertEquals(0.1f, interp.interpolate(0.1f), 0.0f);
      assertEquals(1.0f, interp.interpolate(1.0f), 0.0f);
   }
}
