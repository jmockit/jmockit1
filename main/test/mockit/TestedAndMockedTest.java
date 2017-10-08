/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit;

import org.junit.*;
import static org.junit.Assert.*;

import mockit.internal.expectations.invocation.*;

public final class TestedAndMockedTest
{
   public static class ClassToBeTested
   {
      private final String someData;
      String outputData;
      AnotherClassToBeTested collaborator;

      public ClassToBeTested(String someData) { this.someData = someData; }

      public boolean doSomeOperation(int i, String s)
      {
         validateInput(i, s);
         int j = i + doSomething();
         doSomethingElse(s);
         return j > 0;
      }

      static void validateInput(int i, String s) { if (i <= 0 || s == null) throw new IllegalArgumentException(); }
      int doSomething() { return -1; }
      void doSomethingElse(String s) { outputData = "output data: " + s; }

      int doAnotherOperation()
      {
         return collaborator.doSomething() - 23;
      }
   }

   static final class AnotherClassToBeTested
   {
      int doSomething() { return 123; }
   }

   @Tested AnotherClassToBeTested testedAndInjected;
   @Tested(fullyInitialized = true) @Mocked ClassToBeTested tested;
   @Injectable final String testData = "test data";

   @Test
   public void exercisePublicMethodWhileHavingHelperMethodsMocked()
   {
      assertEquals(testData, tested.someData);

      new Expectations() {{ tested.doSomething(); result = 123; }};
      new Expectations() {{ ClassToBeTested.validateInput(anyInt, anyString); }};

      boolean result = tested.doSomeOperation(0, "testing");

      assertTrue(result);
      assertEquals("output data: testing", tested.outputData);

      new Verifications() {{ tested.doSomethingElse(anyString); times = 1; }};
   }

   @Test
   public void exerciseTopLevelTestedObjectTogetherWithInjectedSecondLevelTestedObject()
   {
      assertEquals(123, testedAndInjected.doSomething());
      assertEquals(100, tested.doAnotherOperation());
   }

   @Test(expected = MissingInvocation.class)
   public void mockTestedClass(@Mocked final ClassToBeTested mock)
   {
      new Expectations() {{
         mock.doSomethingElse("");
      }};
   }
}
