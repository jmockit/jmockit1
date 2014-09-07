/*
 * Copyright 2003-2009 OFFIS, Henri Tremblay
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.easymock.samples;

import org.junit.*;

import static org.easymock.EasyMock.*;
import org.easymock.*;
import static org.junit.Assert.*;

/**
 * Example of how to perform partial mocking.
 */
public final class PartialClassMockTest extends EasyMockSupport
{
   private Rect rect;

   @Before
   public void setUp()
   {
      rect = createMockBuilder(Rect.class).addMockedMethods("getX", "getY").createMock();
   }

   @Test
   public void testGetArea()
   {
      expect(rect.getX()).andReturn(4);
      expect(rect.getY()).andReturn(5);
      replayAll();

      assertEquals(20, rect.getArea());
      verifyAll();
   }
}
