/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit;

import org.junit.*;
import org.junit.rules.*;

import static org.junit.Assert.*;

public final class MisusedExpectationsTest
{
   @Rule public final ExpectedException thrown = ExpectedException.none();

   @SuppressWarnings("unused")
   static class Blah
   {
      Blah() {}
      Blah(int i) {}

      @SuppressWarnings("RedundantStringConstructorCall") final String name = new String("Blah");

      int value() { return -1; }
      void setValue(int value) {}
      String doSomething(boolean b) { return ""; }
      String getName() { return name.toUpperCase(); }
      Blah same() { return this; }
      Runnable getSomethingElse() { return null; }

      static Object doSomething() { return null; }
      static Object someValue() { return null; }
   }

   @Mocked Blah mock;

   // Arrange-Act-Assert non-conformance //////////////////////////////////////////////////////////////////////////////

   @Test
   public void recordExpectationAfterInvokingSameMethodInReplayPhase()
   {
      assertEquals(0, mock.value());

      new Expectations() {{ mock.value(); result = 1; }};

      assertEquals(1, mock.value());
   }

   // Duplicate recordings ////////////////////////////////////////////////////////////////////////////////////////////

   @Test
   public void recordDuplicateInvocationWithNoArguments()
   {
      new Expectations() {{
         mock.value(); result = 1;
         mock.value(); result = 2; // second recording overrides the first
      }};

      assertEquals(2, mock.value());
      assertEquals(2, mock.value());
   }

   @Test
   public void recordDuplicateInvocationWithArgumentMatcher()
   {
      new Expectations() {{
         mock.setValue(anyInt); result = new UnknownError();
         mock.setValue(anyInt); // overrides the previous one
      }};

      mock.setValue(3);
   }

   @Test
   public void recordDuplicateInvocationInSeparateExpectationBlocks()
   {
      new Expectations() {{ mock.value(); result = 1; }};
      new Expectations() {{ mock.value(); result = 2; }}; // overrides the previous expectation

      assertEquals(2, mock.value());
   }

   // Order-related recordings ////////////////////////////////////////////////////////////////////////////////////////

   @Test
   public void recordUnorderedInstantiationOfClassMockedTwice(@Mocked final Blah mock2)
   {
      new Expectations() {{
         new Blah(); times = 1;
         mock.value(); result = 123;
         mock2.value(); result = 45;
      }};

      assertEquals(45, mock2.value());
      assertEquals(123, mock.value());
      new Blah();
   }

   @Test
   public void verifyOrderedInstantiationOfClassMockedTwice(@Mocked final Blah mock2)
   {
      new Blah();
      mock2.doSomething(true);

      new VerificationsInOrder() {{
         new Blah();
         mock2.doSomething(anyBoolean);
      }};
   }

   @Test
   public void verifyUnorderedInstantiationOfClassMockedTwice(@Mocked final Blah mock2)
   {
      mock.doSomething(false);
      mock2.doSomething(true);
      new Blah();

      new Verifications() {{
         mock2.doSomething(true);
         new Blah();
         mock.doSomething(false);
      }};
   }

   // Cascading ///////////////////////////////////////////////////////////////////////////////////////////////////////

   @Test
   public void ambiguousCascadingWhenMultipleValidCandidatesAreAvailable(
      @Injectable Runnable r1, @Injectable Runnable r2)
   {
      Runnable cascaded = mock.getSomethingElse(); // which one to return: r1 or r2?

      assertSame(r2, cascaded); // currently, last mock to be declared wins
   }

   // @Tested/@Injectable usage ///////////////////////////////////////////////////////////////////////////////////////

   static class TestedClass { Runnable action; }
   @Injectable static Runnable action = new Runnable() { @Override public void run() {} };
   @Tested TestedClass tested;

   @Test
   public void checkStaticInjectableIsNotUsed()
   {
      assertNull(tested.action);
   }

   // Other cases /////////////////////////////////////////////////////////////////////////////////////////////////////

   static final class CustomException extends Exception {}

   @Test(expected = IllegalArgumentException.class)
   public void attemptingToMockAllInstancesOfExceptionSubclass(@Mocked CustomException anyCustomException)
   {
      fail("Shouldn't get here");
   }

   @Test
   public void attemptingToPartiallyMockExceptionSubclass()
   {
      thrown.expect(IllegalArgumentException.class);
      thrown.expectMessage("CustomException");

      new Expectations(CustomException.class) {};
   }
}
