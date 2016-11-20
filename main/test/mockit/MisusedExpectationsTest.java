/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit;

import java.util.*;

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
   public void multipleReplayPhasesWithFirstSetOfExpectationsFullyReplayed()
   {
      // First record phase:
      new StrictExpectations() {{
         new Blah().value(); result = 5;
      }};

      // First replay phase:
      assertEquals(5, new Blah().value());

      // Second record phase:
      new StrictExpectations() {{
         mock.value(); result = 6;
         mock.value(); result = 3;
      }};

      // Second replay phase:
      assertEquals(6, mock.value());
      assertEquals(3, mock.value());
   }

   @Test
   public void multipleReplayPhasesWithFirstSetOfExpectationsPartiallyReplayed()
   {
      // First record phase:
      new StrictExpectations() {{
         mock.value(); returns(1, 2);
      }};

      // First replay phase:
      assertEquals(1, mock.value());

      // Second record phase:
      new StrictExpectations() {{
         mock.value(); returns(3, 4);
      }};

      // Second replay phase:
      assertEquals(2, mock.value());
      assertEquals(3, mock.value());
      assertEquals(4, mock.value());
   }

   @Test
   public void recordNotStrictExpectationAfterInvokingSameMethodInReplayPhase()
   {
      assertEquals(0, mock.value());

      new Expectations() {{ mock.value(); result = 1; }};

      assertEquals(1, mock.value());
   }

   @Test
   public void recordStrictExpectationAfterInvokingSameMethodInReplayPhase()
   {
      assertEquals(0, mock.value());

      new StrictExpectations() {{ mock.value(); result = 1; }};

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

   @Test
   public void recordSameInvocationInNotStrictExpectationBlockThenInStrictOne()
   {
      new Expectations() {{ mock.value(); result = 1; }};
      new StrictExpectations() {{ mock.value(); result = 2; }}; // overrides previous one

      assertEquals(2, mock.value());
   }

   // Order-related recordings ////////////////////////////////////////////////////////////////////////////////////////

   @Test
   public void recordOrderedInstantiationOfClassMockedTwice(@Mocked Blah mock2)
   {
      new StrictExpectations() {{
         // OK because of the strictly ordered matching (will match the *first* invocation with this constructor).
         new Blah();
      }};

      new Blah();
   }

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

   // Attempts to mock JRE classes that should never be mocked ////////////////////////////////////////////////////////

   @Test
   public void attemptToMockString(@Mocked String s) { assertNotNull(s); }

   @Test
   public void attemptToMockStringBuilder(@Mocked StringBuilder s) { assertNotNull(s); }

   @Test
   public void attemptToMockStringBuffer(@Mocked StringBuffer s) { assertNotNull(s); }

   @Test
   public void attemptToMockArrayList(@Mocked ArrayList<?> list) { assertNotNull(list); }

   @Test
   public void attemptToMockLinkedList(@Mocked LinkedList<?> list) { assertNotNull(list); }

   @Test
   public void attemptToMockHashMap(@Mocked HashMap<?, ?> map) { assertNotNull(map); }
}
