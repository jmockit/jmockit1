/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit;

import static org.junit.Assert.*;
import org.junit.*;

public final class TestedClassWithNoDITest
{
   public static final class TestedClass
   {
      private final Dependency dependency = new Dependency();

      public boolean doSomeOperation() { return dependency.doSomething() > 0; }
   }

   static class Dependency { int doSomething() { return -1; } }

   @Tested TestedClass tested1;
   @Tested final TestedClass tested2 = new TestedClass();
   @Tested TestedClass tested3;
   @Tested NonPublicTestedClass tested4;
   @Tested final TestedClass tested5 = null;
   @Mocked Dependency mock;
   TestedClass tested;

   @Before
   public void setUp()
   {
      assertNotNull(mock);
      assertNull(tested);
      tested = new TestedClass();
      assertNull(tested3);
      tested3 = tested;
      assertNull(tested1);
      assertNotNull(tested2);
      assertNull(tested4);
      assertNull(tested5);
   }

   @Test
   public void verifyTestedFields()
   {
      assertNull(tested5);
      assertNotNull(tested4);
      assertNotNull(tested3);
      assertSame(tested, tested3);
      assertNotNull(tested2);
      assertNotNull(tested1);
   }

   @Test
   public void exerciseAutomaticallyInstantiatedTestedObject()
   {
      new Expectations() {{ mock.doSomething(); result = 1; }};

      assertTrue(tested1.doSomeOperation());
   }

   @Test
   public void exerciseManuallyInstantiatedTestedObject()
   {
      new Expectations() {{ mock.doSomething(); result = 1; }};

      assertTrue(tested2.doSomeOperation());

      new FullVerifications() {};
   }

   @Test
   public void exerciseAnotherManuallyInstantiatedTestedObject()
   {
      assertFalse(tested3.doSomeOperation());

      new Verifications() {{ mock.doSomething(); times = 1; }};
   }
}

class NonPublicTestedClass
{
   @SuppressWarnings("RedundantNoArgConstructor")
   NonPublicTestedClass() {}
}