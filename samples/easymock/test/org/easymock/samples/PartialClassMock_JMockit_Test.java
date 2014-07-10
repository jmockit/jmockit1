/*
 * Copyright (c) 2006-2013 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package org.easymock.samples;

import org.junit.*;

import mockit.*;

import static org.junit.Assert.*;

public final class PartialClassMock_JMockit_Test
{
   @Test
   public void testGetArea()
   {
      final Rect rect = new Rect();

      new NonStrictExpectations(rect) {{
         rect.getX(); result = 4;
         rect.getY(); result = 5;
      }};

      assertEquals(20, rect.getArea());
   }
}
