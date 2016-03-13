/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.integration.junit4;

import static org.junit.Assert.*;
import org.junit.*;

import mockit.*;
import mockit.integration.*;

public final class JUnit4ExpectationsTest
{
   @Tested TestedClass tested;
   @Injectable MockedClass dependency;
   @Injectable MockedClass mock2;

   @Before
   public void setUp1()
   {
      new Expectations() {{ mock2.doSomethingElse(anyInt); result = true; minTimes = 0; }};
   }

   @Before
   public void setUp2()
   {
      new Expectations() {{ dependency.getValue(); result = "mocked"; minTimes = 0; }};
   }

   @After
   public void tearDown()
   {
      new Verifications() {{ mock2.doSomethingElse(6); times = 1; }};
   }

   @Test
   public void testSomething()
   {
      new Expectations() {{
         dependency.doSomething(anyInt); result = true; times = 2;
      }};

      assertTrue(dependency.doSomething(5));
      assertEquals("mocked", dependency.getValue());
      assertTrue(tested.doSomething(-5));
      assertTrue(mock2.doSomethingElse(6));

      new FullVerifications(dependency) {{
         dependency.getValue();
      }};
   }

   @Test
   public void testSomethingElse()
   {
      assertEquals("mocked", dependency.getValue());
      assertFalse(tested.doSomething(41));
      assertTrue(mock2.doSomethingElse(6));

      new FullVerificationsInOrder(dependency) {{
         dependency.getValue();
         dependency.doSomething(anyInt);
      }};
   }
}
