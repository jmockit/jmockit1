/*
 * Copyright (c) 2006-2014 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package org.easymock.samples;

import org.junit.*;

import mockit.*;

import static org.junit.Assert.*;

public final class PartialClassMock_JMockit_Test
{
   @Tested @Mocked Rect rect;

   @Test
   public void testGetArea()
   {
      new Expectations() {{
         rect.getX(); result = 4;
         rect.getY(); result = 5;
      }};

      assertEquals(20, rect.getArea());
   }
}
