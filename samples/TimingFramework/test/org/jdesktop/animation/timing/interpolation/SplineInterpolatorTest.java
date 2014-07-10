/*
 * Copyright (c) 2006-2011 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package org.jdesktop.animation.timing.interpolation;

import org.junit.*;

import static org.junit.Assert.*;

public final class SplineInterpolatorTest
{
   @Test
   public void testInterpolate()
   {
      Interpolator interp = new SplineInterpolator(0.0f, 0.0f, 1.0f, 0.0f);

      assertEquals(0.0f, interp.interpolate(0.0f), 0.0f);
      assertEquals(0.28f, interp.interpolate(0.5f), 0.001f);
      assertEquals(1.0f, interp.interpolate(1.0f), 0.0001f);
      assertEquals(1.0f, interp.interpolate(1.1f), 0.0001f);
   }

   @Test(expected = IllegalArgumentException.class)
   public void testCreateSplineWithInvalidControlPoint()
   {
      new SplineInterpolator(0.0f, 0.0f, -1.0f, 2.0f);
   }
}
