/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit;

import java.beans.*;
import java.util.*;

import javax.persistence.*;

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
      new Expectations() {{
         mock.value(); result = 1;
      }};

      new Expectations() {{
         mock.value(); result = 2; // overrides the previous expectation
      }};

      assertEquals(2, mock.value());
   }

   @Test
   public void recordSameInvocationInNotStrictExpectationBlockThenInStrictOne()
   {
      thrown.expect(AssertionError.class);

      new Expectations() {{
         mock.value(); result = 1;
      }};

      new StrictExpectations() {{
         // This expectation can never be replayed, so it will cause the test to fail:
         mock.value(); result = 2;
      }};

      assertEquals(1, mock.value());
      assertEquals(1, mock.value());
   }

   @Test
   public void recordNotStrictExpectationAfterInvokingSameMethodInReplayPhase()
   {
      assertEquals(0, mock.value());

      new Expectations() {{
         mock.value(); result = 1;
      }};

      assertEquals(1, mock.value());
   }

   @Test
   public void recordStrictExpectationAfterInvokingSameMethodInReplayPhase()
   {
      assertEquals(0, mock.value());

      new StrictExpectations() {{
         mock.value(); result = 1;
      }};

      assertEquals(1, mock.value());
   }

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

   @Test
   public void verifiedExpectationWithDuplicateRecordedExpectation()
   {
      thrown.expect(IllegalStateException.class);
      thrown.expectMessage("Identical expectation");

      new Expectations() {{
         mock.doSomething(anyBoolean); // not allowed unless recorded with minTimes = 0
      }};

      mock.doSomething(true);

      new Verifications() {{
         mock.doSomething(anyBoolean);
      }};
   }

   @BeforeClass
   public static void recordExpectationsInStaticContext()
   {
      final Blah blah = new Blah();

      try {
         new Expectations(blah) {{
            blah.doSomething(anyBoolean); result = "invalid";
         }};
         fail();
      }
      catch (IllegalStateException ignored) {
         // OK
      }
   }

   @AfterClass
   public static void verifyExpectationsInStaticContext()
   {
      try {
         new FullVerifications() {};
         fail();
      }
      catch (IllegalStateException ignored) {
         // OK
      }
   }

   @SuppressWarnings("UnusedParameters")
   static final class BlahBlah extends Blah
   {
      @Override String doSomething(boolean b) { return "overridden"; }
      void doSomethingElse(Object o) {}
   }

   @Test
   public void expectationBlockContainingATryCatchStatement()
   {
      thrown.expect(IllegalArgumentException.class);
      thrown.expectMessage("try/catch");

      new Expectations() {{
         try { mock.doSomething(anyBoolean); } catch (RuntimeException ignore) {}
      }};
   }

   @Test
   public void expectationBlockWithMethodContainingATryCatchStatement()
   {
      new Expectations() {
         {
            recordSomething();
         }

         void recordSomething()
         {
            // Allowed, although not recommended in Java code.
            try { mock.doSomething(anyBoolean); } catch (RuntimeException ignore) {}
         }
      };

      mock.doSomething(true);
   }

   @Test
   public void expectationBlockContainingATryFinallyStatement()
   {
      thrown.expect(IllegalArgumentException.class);
      thrown.expectMessage("try/finally");

      new StrictExpectations() {{
         try { mock.doSomething(anyBoolean); } finally { mock.setValue(1); }
      }};
   }

   @Test
   public void expectationBlockContainingATryCatchFinallyStatement()
   {
      thrown.expect(IllegalArgumentException.class);
      thrown.expectMessage("try/catch");

      new Expectations() {{
         try { mock.doSomething(anyBoolean); } catch (RuntimeException ignore) {}
         finally { System.out.println(); }
      }};
   }

   @Test
   public void expectationBlockContainingATableSwitchStatement()
   {
      thrown.expect(IllegalArgumentException.class);
      thrown.expectMessage("switch");

      final int s = 1;

      new Expectations() {{
         switch (s) {
            case 1: mock.doSomething(true); break;
            case 2: mock.doSomething(false); break;
            case 3: mock.doSomething(false); break;
            default: mock.setValue(3);
         }
      }};
   }

   @Test
   public void expectationBlockContainingALookupSwitchStatement()
   {
      thrown.expect(IllegalArgumentException.class);
      thrown.expectMessage("switch");

      new Verifications() {{
         switch (mock.value()) {
            case 1: mock.doSomething(true); break;
            default: mock.doSomething(false);
         }
      }};
   }

   @Test
   public void expectationBlockContainingAnIfStatement()
   {
      thrown.expect(IllegalArgumentException.class);
      thrown.expectMessage("conditional");

      new Expectations() {{
         if (mock.value() > 0) {
            mock.doSomething(false);
         }
      }};
   }

   @Test
   public void expectationBlockContainingAForStatement()
   {
      thrown.expect(IllegalArgumentException.class);
      thrown.expectMessage("conditional");

      new Expectations() {{
         for (int i = 0; i < 3; i++) {
            mock.setValue(i);
         }
      }};
   }

   @Test
   public void partiallyMockingAClassWhichIsAlreadyMocked()
   {
      thrown.expect(IllegalArgumentException.class);
      thrown.expectMessage("Already mocked");
      thrown.expectMessage("Blah");

      new Expectations(Blah.class) {};
   }

   @Test
   public void partiallyMockingASubclassOfAClassWhichIsAlreadyMocked()
   {
      thrown.expect(IllegalArgumentException.class);
      thrown.expectMessage("Already mocked");
      thrown.expectMessage("Blah");

      BlahBlah blahBlah = new BlahBlah();
      new Expectations(blahBlah) {};
   }

   @Test
   public void attemptToHaveDelegateMethodWithInvocationParameterNotAtFirstPosition()
   {
      thrown.expect(IllegalArgumentException.class);
      thrown.expectMessage("Delegate method");
      thrown.expectMessage("Invocation parameter");
      thrown.expectMessage("first");

      new Expectations() {{
         mock.doSomething(true);
         result = new Delegate() {
            @SuppressWarnings("unused")
            void delegate(boolean b, Invocation inv) {}
         };
      }};
   }

   @Test
   public void callMockedMethodDuringEvaluationOfExpressionAssignedToResultField()
   {
      class CUT {
         final int value;
         CUT() { value = mock.value(); }
         @Override public String toString() { return "cut=" + value; }
      }

      new Expectations() {{
         mock.getName();
         result = "test " + new CUT();
      }};

      String name = mock.getName();

      assertEquals("test cut=0", name);
   }

   @Test(expected = IllegalArgumentException.class)
   public void attemptToApplyCapturingOnFinalClass(@Capturing BlahBlah mock)
   {
      fail("Should fail before entering the test");
   }

   @Test(expected = IllegalArgumentException.class)
   public void attemptToApplyBothMockedAndCapturing(@Mocked @Capturing Blah mock)
   {
      fail("Should fail before entering the test");
   }

   @Test
   public void allowMockedWithAttributePlusCapturing(@Mocked(stubOutClassInitialization = true) @Capturing Blah mock)
   {
      assertNotNull(mock);
   }

   @Test(expected = IllegalArgumentException.class)
   public void attemptToApplyBothMockedAndInjectable(@Mocked @Injectable Blah mock)
   {
      fail("Should fail before entering the test");
   }

   @Test(expected = IllegalArgumentException.class)
   public void attemptToApplyBothInjectableAndCapturingWithoutMaxInstances(@Capturing @Injectable Blah mock)
   {
      fail("Should fail before entering the test");
   }

   @Test
   public void recordExpectationReturningSameMockedInstanceWhichWouldBeAutomaticallyCascaded()
   {
      new Expectations() {{ mock.same(); result = mock; }};

      assertSame(mock, mock.same());
   }

   @Test
   public void ambiguousCascadingWhenMultipleValidCandidatesAreAvailable(
      @Injectable Runnable r1, @Injectable Runnable r2)
   {
      Runnable cascaded = mock.getSomethingElse(); // which one to return: r1 or r2?

      assertSame(r2, cascaded); // currently, last mock to be declared wins
   }

   @Test
   public void recordExpectationWithMinTimesSetToZero()
   {
      thrown.expect(IllegalArgumentException.class);
      thrown.expectMessage("Invalid");
      thrown.expectMessage("minTimes");
      thrown.expectMessage("test method");
      thrown.expectMessage("named Expectations");

      new Expectations() {{
         mock.doSomething(true);
         minTimes = 0; // invalid if set to 0 (or negative) in a test method
      }};
   }

   @Test
   public void recordDynamicExpectationWithMinTimesSetToZero()
   {
      thrown.expect(IllegalArgumentException.class);

      new Expectations(ProcessBuilder.class) {{
         new ProcessBuilder();
         minTimes = 0; // invalid
      }};
   }

   @Test
   public void verifyExpectationWithMinTimesSetToZero()
   {
      thrown.expect(IllegalArgumentException.class);
      thrown.expectMessage("Invalid minTimes");
      thrown.expectMessage("test method");

      new Verifications() {{
         mock.doSomething(true);
         minTimes = 0; // invalid
      }};
   }

   @Test
   public void recordReplacementMockInstanceExpectingItWouldReplaceAFutureInstanceCreatedWithMatchingConstructorCall()
   {
      thrown.expect(IllegalStateException.class);
      thrown.expectMessage("single replacement");
      thrown.expectMessage("Blah");

      new Expectations() {{
         new Blah(); result = mock;
      }};
   }

   @Test
   public void recordMultipleReplacementsForSameMockedClassUsingSingleReplacementInstance()
   {
      thrown.expect(IllegalStateException.class);
      thrown.expectMessage("single replacement");
      thrown.expectMessage("Blah");

      new Expectations() {{
         new Blah(); result = mock;
         new Blah(1); result = mock;
      }};
   }

   @Test
   public void callMockedMethodFromExpressionAssignedToResultField()
   {
      thrown.expect(IllegalArgumentException.class);
      thrown.expectMessage("Invalid invocation to another mocked method during unfinished recording");

      new Expectations() {{
         Blah.doSomething();
         result = Blah.someValue();
      }};
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

   // Mocking/faking of JPA-annotated classes /////////////////////////////////////////////////////////////////////////

   @Entity static class Person { public int getId() { return 1; } }
   static class Manager { Person findPerson() { return null; } }

   @Test
   public void mockClassWithMethodsReturningEntityClasses(@Mocked final Manager mock) throws IntrospectionException
   {
      final Person p = new Person();
      new Expectations() {{ mock.findPerson(); result = p; }};

      Person found = mock.findPerson();

      assertSame(p, found);
   }

   @Test(expected = IllegalArgumentException.class)
   public void attemptToMockAnEntityClass(@Injectable Person person) {}

   @Test
   public void attemptToFakeAnEntityClass()
   {
      thrown.expect(IllegalArgumentException.class);
      thrown.expectMessage("@Entity-annotated class");

      new MockUp<Person>() {
         @Mock int getId() { return 2; }
      };
   }
}
