/*
 * Copyright (c) 2006-2014 Rogério Liesenfeld
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
      @SuppressWarnings("RedundantStringConstructorCall") final String name = new String("Blah");
      int value() { return -1; }
      void setValue(int value) {}
      String doSomething(boolean b) { return ""; }
      String getName() { return name.toUpperCase(); }
   }

   @Mocked Blah mock;

   @Test
   public void multipleReplayPhasesWithFirstSetOfExpectationsFullyReplayed()
   {
      // First record phase:
      new Expectations() {{
         new Blah().value(); result = 5;
      }};

      // First replay phase:
      assertEquals(5, new Blah().value());

      // Second record phase:
      new Expectations() {{
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
      new Expectations() {{
         mock.value(); returns(1, 2);
      }};

      // First replay phase:
      assertEquals(1, mock.value());

      // Second record phase:
      new Expectations() {{
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
      new NonStrictExpectations() {{
         mock.value(); result = 1;
         mock.value(); result = 2; // second recording overrides the first
      }};

      assertEquals(2, mock.value());
      assertEquals(2, mock.value());
   }

   @Test
   public void recordDuplicateInvocationWithArgumentMatcher()
   {
      new NonStrictExpectations() {{
         mock.setValue(anyInt); result = new UnknownError();
         mock.setValue(anyInt); // overrides the previous one
      }};

      mock.setValue(3);
   }

   @Test
   public void recordDuplicateInvocationInSeparateNonStrictExpectationBlocks()
   {
      new NonStrictExpectations() {{
         mock.value(); result = 1;
      }};

      new NonStrictExpectations() {{
         mock.value(); result = 2; // overrides the previous expectation
      }};

      assertEquals(2, mock.value());
   }

   @Test
   public void recordSameInvocationInNonStrictExpectationBlockThenInStrictOne()
   {
      thrown.handleAssertionErrors();
      thrown.expect(AssertionError.class);

      new NonStrictExpectations() {{
         mock.value(); result = 1;
      }};

      new Expectations() {{
         // This expectation can never be replayed, so it will cause the test to fail:
         mock.value(); result = 2;
      }};

      assertEquals(1, mock.value());
      assertEquals(1, mock.value());
   }

   @Test
   public void recordNonStrictExpectationAfterInvokingSameMethodInReplayPhase()
   {
      assertEquals(0, mock.value());

      new NonStrictExpectations() {{
         mock.value(); result = 1;
      }};

      assertEquals(1, mock.value());
   }

   @Test
   public void recordStrictExpectationAfterInvokingSameMethodInReplayPhase() throws Exception
   {
      assertEquals(0, mock.value());

      new Expectations() {{
         mock.value(); result = 1;
      }};

      assertEquals(1, mock.value());
   }

   @Test
   public void recordOrderedInstantiationOfClassMockedTwice(@Mocked Blah mock2)
   {
      new Expectations() {{
         // OK because of the strictly ordered matching (will match the *first* invocation with this constructor).
         new Blah();
      }};

      new Blah();
   }

   @Test
   public void recordUnorderedInstantiationOfClassMockedTwice(@Mocked final Blah mock2)
   {
      new NonStrictExpectations() {{
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

   @BeforeClass
   public static void recordExpectationsInStaticContext()
   {
      final Blah blah = new Blah();

      try {
         new NonStrictExpectations(blah) {{
            blah.doSomething(anyBoolean); result = "invalid";
         }};
         fail();
      }
      catch (IllegalStateException ignored) {
         // OK
      }
   }

   @SuppressWarnings("UnusedParameters")
   static class BlahBlah extends Blah
   {
      @Override String doSomething(boolean b) { return "overridden"; }
      void doSomethingElse(Object o) {}
      static void doSomethingStatic() {}
   }

   @SuppressWarnings("UnnecessarySuperQualifier")
   @Test
   public void accessSpecialFieldsInExpectationBlockThroughSuper(@Mocked final BlahBlah mock2)
   {
      new NonStrictExpectations() {{
         mock2.value(); super.result = 123; super.minTimes = 1; super.maxTimes = 2;

         mock2.doSomething(super.anyBoolean); super.result = "test";
         super.times = 1;

         mock2.setValue(withNotEqual(0));
      }};

      assertEquals(123, mock2.value());
      assertEquals("test", mock2.doSomething(true));
      mock2.setValue(1);
   }

   @SuppressWarnings("UnnecessarySuperQualifier")
   @Test
   public void accessSpecialFieldsInVerificationBlockThroughSuper(@Mocked final BlahBlah mock2)
   {
      assertNull(mock2.doSomething(true));

      new Verifications() {{
         mock2.doSomething(false); super.times = 0;
         mock2.doSomethingElse(super.any); super.maxTimes = 0;
      }};
   }

   @Test // with Java 7 only: "java.lang.VerifyError: Expecting a stackmap frame ..."
   public void expectationBlockContainingATryBlock()
   {
      new Expectations() {{
         try { mock.doSomething(anyBoolean); } finally { mock.setValue(1); }
      }};

      mock.doSomething(true);
      mock.setValue(1);
   }

   @Test
   public void mixingStrictAndNonStrictExpectationsForSameDynamicallyMockedObject()
   {
      final BlahBlah tested = new BlahBlah();

      new Expectations(tested) {{ tested.value(); }};

      try {
         new NonStrictExpectations(tested) {{ tested.doSomething(anyBoolean); }};
         fail();
      }
      catch (IllegalArgumentException ignore) {}
   }

   @Test
   public void mixingStrictAndNonStrictExpectationsForSameDynamicallyMockedClass()
   {
      new Expectations(BlahBlah.class) {{ BlahBlah.doSomethingStatic(); }};

      try {
         new NonStrictExpectations(BlahBlah.class) {};
         fail();
      }
      catch (IllegalArgumentException ignore) {}
   }

   @Test
   public void mixingStrictAndNonStrictExpectationsForSameDynamicallyMockedClass_forNonStrictBaseClass()
   {
      new Expectations(BlahBlah.class) {{ new Blah(); BlahBlah.doSomethingStatic(); }};

      try {
         new NonStrictExpectations(Blah.class) {};
         fail();
      }
      catch (IllegalArgumentException ignore) {}
   }

   @Test
   public void mixingStrictAndNonStrictExpectationsForSameDynamicallyMockedClass_forStrictBaseClass()
   {
      new Expectations(BlahBlah.class) {{ new BlahBlah(); }};

      try {
         new NonStrictExpectations(BlahBlah.class) {};
         fail();
      }
      catch (IllegalArgumentException ignore) {}
   }

   @Test
   public void partiallyMockingAClassWhichIsAlreadyMocked()
   {
      thrown.expect(IllegalArgumentException.class);
      thrown.expectMessage("already mocked");
      thrown.expectMessage("Blah");

      new NonStrictExpectations(Blah.class) {};
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

      new NonStrictExpectations() {{
         mock.getName();
         result = "test " + new CUT();
      }};

      String name = mock.getName();

      assertEquals("test cut=0", name);
   }
}
