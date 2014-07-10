/*
 * Copyright (c) 2006-2014 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.mockups;

import org.testng.annotations.*;
import static org.testng.Assert.*;

import mockit.internal.mockups.MockStateBetweenTestMethodsJUnit45Test.*;

public final class MockStateBetweenTestMethodsNGTest
{
   @BeforeClass
   public void setUpMocks()
   {
      new TheMockClass();
   }

   @Test
   public void firstTest()
   {
      TheMockClass.assertMockState(0);
      assertEquals(new RealClass().doSomething(), 1);
      TheMockClass.assertMockState(1);
   }

   @Test(dependsOnMethods = "firstTest")
   public void secondTest()
   {
      TheMockClass.assertMockState(0);
      assertEquals(new RealClass().doSomething(), 2);
      TheMockClass.assertMockState(1);
   }
}