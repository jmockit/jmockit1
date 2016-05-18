/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.integration.testng;

import static org.testng.Assert.*;
import org.testng.annotations.*;

import mockit.*;

public final class DynamicMockingInBeforeMethodTest
{
   static final class MockedClass
   {
      boolean doSomething(int i) { return i > 0; }
   }

   final MockedClass anInstance = new MockedClass();

   @BeforeMethod
   public void recordExpectationsOnDynamicallyMockedClass()
   {
      assertTrue(anInstance.doSomething(56));
      assertFalse(anInstance.doSomething(-56));

      new Expectations(anInstance) {{
         anInstance.doSomething(anyInt); result = true; minTimes = 0;
      }};
   }

   @AfterMethod
   public void verifyThatDynamicallyMockedClassIsStillMocked()
   {
      new FullVerifications() {{
         anInstance.doSomething(anyInt); times = 1;
      }};
   }

   @Test
   public void testSomething()
   {
      assertTrue(anInstance.doSomething(-56));
   }

   @Test
   public void testSomethingElse()
   {
      assertTrue(anInstance.doSomething(-129));
   }
}
