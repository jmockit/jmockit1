/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit;

import static org.junit.Assert.*;
import org.junit.*;

public final class DynamicMockingInBeforeMethodTest
{
   static final class MockedClass { boolean doSomething(int i) { return i > 0; } }

   final MockedClass anInstance = new MockedClass();

   @Before
   public void recordExpectationsOnDynamicallyMockedClass()
   {
      assertTrue(anInstance.doSomething(56));

      new Expectations(anInstance) {{
         anInstance.doSomething(anyInt); result = true; minTimes = 0;
      }};

      assertTrue(anInstance.doSomething(-56));
   }

   @After
   public void verifyThatDynamicallyMockedClassIsStillMocked()
   {
      new FullVerifications() {{
         anInstance.doSomething(anyInt); times = 2;
      }};
   }

   @Test
   public void testSomething()
   {
      assertTrue(anInstance.doSomething(56));
   }

   @Test
   public void testSomethingElse()
   {
      assertTrue(anInstance.doSomething(-129));
   }
}
