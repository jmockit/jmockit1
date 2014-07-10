/*
 * Copyright (c) 2006-2012 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit;

import org.junit.*;
import static org.junit.Assert.*;

public final class TestedAbstractClassTest
{
   public abstract static class AbstractClass implements Runnable
   {
      private final int value;
      protected String name;

      protected AbstractClass(int value) { this.value = value; }

      public final boolean doSomeOperation()
      {
         run();
         return doSomething() > 0;
      }

      protected abstract int doSomething();

      public int getValue() { return value; }
   }

   // A subclass is generated with the *same* constructors as the tested class, and with *mocked* implementations
   // for all abstract methods in the tested base class, its super-classes and its implemented interfaces.
   @Tested AbstractClass tested;

   @Injectable("123") int value;

   @Test
   public void exerciseTestedObject(@Injectable("Test") String name)
   {
      assertEquals(123, tested.getValue());
      assertEquals("Test", tested.name);

      new NonStrictExpectations() {{
         tested.doSomething(); result = 23; times = 1;
      }};

      assertTrue(tested.doSomeOperation());

      new Verifications() {{ tested.run(); }};
   }

   @Test
   public void exerciseDynamicallyMockedTestedObject()
   {
      assertEquals(123, tested.getValue());

      new NonStrictExpectations(tested) {{
         tested.getValue(); result = 45;
         tested.doSomething(); result = 7;
      }};

      assertEquals(45, tested.getValue());
      assertTrue(tested.doSomeOperation());

      new Verifications() {{ tested.run(); times = 1; }};
   }

   @Test
   public void exerciseTestedObjectAgain(@Injectable("Another test") String text)
   {
      assertEquals(123, tested.getValue());
      assertEquals("Another test", tested.name);

      assertFalse(tested.doSomeOperation());

      new FullVerificationsInOrder() {{
         tested.run();
         tested.doSomething();
      }};
   }

   static Class<?> generatedSubclass;

   @After
   public void assertThatGeneratedSubclassIsAlwaysTheSame()
   {
      if (generatedSubclass == null) {
         generatedSubclass = tested.getClass();
      }
      else {
         assertSame(generatedSubclass, tested.getClass());
      }
   }
}
