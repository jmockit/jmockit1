/*
 * Copyright (c) 2006-2015 Rogério Liesenfeld
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
   public void recordStrictExpectationAfterInvokingSameMethodInReplayPhase() throws Exception
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
   static class BlahBlah extends Blah
   {
      @Override String doSomething(boolean b) { return "overridden"; }
      void doSomethingElse(Object o) {}
      Blah getBlah() { return null; }
   }

   @SuppressWarnings("UnnecessarySuperQualifier")
   @Test
   public void accessSpecialFieldsInExpectationBlockThroughSuper(@Mocked final BlahBlah mock2)
   {
      new Expectations() {{
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

      new NonStrictExpectations() {{
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

   @Test @Ignore("because Blah is mocked, the result gets confused")
   public void recordResultByInstantiatingMockedClass(@Mocked final BlahBlah mock2)
   {
      final Blah[] blahs = new Blah[1];

      new NonStrictExpectations() {{
         mock2.getBlah();
         result = blahs[0] = new Blah();
      }};

      Blah blah = mock2.getBlah();

      assertSame(blahs[0], blah);
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
