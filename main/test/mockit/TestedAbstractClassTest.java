/*
 * Copyright (c) 2006 Rogério Liesenfeld
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
      assertThatGeneratedSubclassIsAlwaysTheSame();
      assertEquals(123, tested.getValue());
      assertEquals("Test", tested.name);

      new Expectations() {{
         tested.doSomething(); result = 23; times = 1;
      }};

      assertTrue(tested.doSomeOperation());

      new Verifications() {{ tested.run(); }};
   }

   @Test
   public void exerciseDynamicallyMockedTestedObject()
   {
      assertThatGeneratedSubclassIsAlwaysTheSame();
      assertEquals(123, tested.getValue());

      new Expectations(tested) {{
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
      assertThatGeneratedSubclassIsAlwaysTheSame();
      assertEquals(123, tested.getValue());
      assertEquals("Another test", tested.name);

      assertFalse(tested.doSomeOperation());

      new FullVerificationsInOrder() {{
         tested.run();
         tested.doSomething();
      }};
   }

   Class<?> generatedSubclass;

   void assertThatGeneratedSubclassIsAlwaysTheSame()
   {
      Class<?> testedClass = tested.getClass();

      if (generatedSubclass == null) {
         generatedSubclass = testedClass;
      }
      else {
         assertSame(generatedSubclass, testedClass);
      }
   }
}
