/*
 * Copyright (c) 2006-2015 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit;

import org.junit.*;
import org.junit.rules.*;
import static org.junit.Assert.*;

import mockit.internal.*;

import static mockit.Deencapsulation.*;

public final class VerificationsTest
{
   @Rule public final ExpectedException thrown = ExpectedException.none();

   @SuppressWarnings("UnusedParameters")
   public static class Dependency
   {
      public Dependency() {}
      private Dependency(int i) {}

      public void setSomething(int value) {}
      public void setSomethingElse(String value) {}
      public void editABunchMoreStuff() {}
      public void notifyBeforeSave() {}
      public void prepare() {}
      public void save() {}

      private void privateMethod() {}
      private static void privateStaticMethod(String s, boolean b) {}
   }

   @Mocked Dependency mock;

   void exerciseCodeUnderTest()
   {
      mock.prepare();
      mock.setSomething(123);
      mock.setSomethingElse("anotherValue");
      mock.setSomething(45);
      mock.editABunchMoreStuff();
      mock.notifyBeforeSave();
      mock.save();
   }

   @Test
   public void verifySimpleInvocations()
   {
      exerciseCodeUnderTest();

      new Verifications() {{
         mock.prepare(); times = 1;
         mock.editABunchMoreStuff();
         mock.setSomething(45);
      }};
   }

   @Test
   public void verifyUnrecordedInvocationThatNeverHappens()
   {
      thrown.expect(MissingInvocation.class);
      thrown.expectMessage("with arguments: 45");

      mock.setSomething(123);
      mock.prepare();

      new Verifications() {{ mock.setSomething(45); }};
   }

   @Test
   public void verifyRecordedInvocationThatNeverHappens()
   {
      thrown.expect(MissingInvocation.class);

      new Expectations() {{ mock.editABunchMoreStuff(); }};

      mock.setSomething(123);
      mock.prepare();

      new Verifications() {{ mock.editABunchMoreStuff(); }};
   }

   @Test
   public void verifyInvocationThatIsAllowedToHappenAnyNumberOfTimesAndHappensOnce()
   {
      mock.prepare();
      mock.setSomething(123);
      mock.save();

      new Verifications() {{
         mock.setSomething(anyInt);
         mock.save(); minTimes = 0;
      }};
   }

   @Test
   public void verifyRecordedInvocationThatIsAllowedToOccurAnyNumberOfTimesAndDoesNotOccur()
   {
      new Expectations() {{ mock.save(); minTimes = 0; }};

      mock.prepare();
      mock.setSomething(123);

      new Verifications() {{
         mock.prepare();
         mock.save(); minTimes = 0;
      }};
   }

   @Test
   public void verifyRecordedInvocationWithExactInvocationCountUsingArgumentMatchers()
   {
      new Expectations() {{ mock.setSomething(anyInt); }};

      mock.setSomething(1);
      mock.setSomething(2);

      new Verifications() {{
         mock.setSomething(anyInt);
         times = 2;
      }};
   }

   @Test
   public void verifyUnrecordedInvocationThatIsAllowedToHappenAnyNoOfTimesAndDoesNotHappen()
   {
      mock.prepare();
      mock.setSomething(123);

      new Verifications() {{
         mock.prepare();
         mock.setSomething(anyInt);
         mock.save(); minTimes = 0;
      }};
   }

   @Test
   public void verifyUnrecordedInvocationThatShouldHappenButDoesNot()
   {
      thrown.expect(MissingInvocation.class);

      mock.setSomething(1);

      new Verifications() {{ mock.notifyBeforeSave(); }};
   }

   @Test
   public void verifyInvocationsWithInvocationCount()
   {
      mock.setSomething(3);
      mock.save();
      mock.setSomethingElse("test");
      mock.save();

      new Verifications() {{
         mock.save(); times = 2;
      }};
   }

   @Test
   public void verifyInvocationsWithInvocationCountLargerThanOccurred()
   {
      thrown.expect(MissingInvocation.class);
      thrown.expectMessage("Missing 2 invocations");
      thrown.expectMessage("with arguments: any int");

      mock.setSomethingElse("test");
      mock.setSomething(3);
      mock.save();

      new Verifications() {{
         mock.setSomething(anyInt);
         times = 3;
      }};
   }

   @Test
   public void verifyInvocationsWithInvocationCountSmallerThanOccurred()
   {
      thrown.expect(UnexpectedInvocation.class);
      thrown.expectMessage("1 unexpected invocation");
      thrown.expectMessage("with arguments: 5");

      mock.setSomethingElse("test");
      mock.setSomething(3);
      mock.save();
      mock.setSomething(5);

      new Verifications() {{
         mock.setSomething(anyInt);
         times = 1;
      }};
   }

   @Test
   public void verifyRecordedInvocationThatShouldNotOccurButDid()
   {
      thrown.expect(UnexpectedInvocation.class);
      thrown.expectMessage("2 unexpected invocations");
      thrown.expectMessage("with arguments: 123");

      new Expectations() {{ mock.setSomething(anyInt); }};

      mock.setSomething(5);
      mock.setSomething(123);

      new Verifications() {{
         mock.setSomething(anyInt);
         maxTimes = 0;
      }};
   }

   @Test
   public void verifySimpleInvocationsInIteratingBlock()
   {
      mock.setSomething(123);
      mock.save();
      mock.setSomething(45);
      mock.save();

      new Verifications(2) {{
         mock.setSomething(anyInt);
         mock.save();
      }};
   }

   @Test
   public void verifySingleInvocationInBlockWithLargerNumberOfIterations()
   {
      thrown.expect(MissingInvocation.class);
      thrown.expectMessage("Missing 2 invocations");
      thrown.expectMessage("with arguments: any int");

      mock.setSomething(123);

      new Verifications(3) {{ mock.setSomething(anyInt); }};
   }

   @Test
   public void verifyMultipleInvocationsInBlockWithSmallerNumberOfIterations()
   {
      thrown.expect(UnexpectedInvocation.class);
      thrown.expectMessage("with arguments: -1015");

      mock.setSomething(45);
      mock.setSomething(123);
      mock.setSomething(-1015);

      new Verifications(2) {{ mock.setSomething(anyInt); }};
   }

   @Test
   public void verifyWithArgumentMatcher()
   {
      exerciseCodeUnderTest();

      new Verifications() {{ mock.setSomething(anyInt); }};
   }

   @Test
   public void verifyWithArgumentMatcherAndIndividualInvocationCounts()
   {
      exerciseCodeUnderTest();

      new Verifications(1) {{
         mock.prepare(); maxTimes = 1;
         mock.setSomething(anyInt); minTimes = 2;
         mock.editABunchMoreStuff(); minTimes = 0; maxTimes = 5;
         mock.save(); times = 1;
      }};
   }

   @Test
   public void verifyWithCustomArgumentMatcherWithoutArgumentValue()
   {
      mock.setSomethingElse("not empty");

      new Verifications() {{
         mock.setSomethingElse(with(new Delegate<String>() {
            @Mock boolean isNotEmpty(String s) { return !s.isEmpty(); }
         }));
      }};
   }

   @Test
   public void verifyThroughCapturedArguments()
   {
      thrown.expect(AssertionError.class);
      thrown.expectMessage("not empty");

      mock.setSomethingElse("test");

      new Verifications() {{
         String value;
         mock.setSomethingElse(value = withCapture());
         assertEquals("not empty", 0, value.length());
      }};
   }

   @Test
   public void verifyWithCustomArgumentMatcher()
   {
      thrown.expect(MissingInvocation.class);
      thrown.expectMessage("with arguments: isEmpty(\"test\")");

      mock.setSomethingElse("test");

      new Verifications() {{
         mock.setSomethingElse(with(new Delegate<String>() {
            @Mock boolean isEmpty(String s) { return s.isEmpty(); }
         }));
      }};
   }

   @Test
   public void verifyWithArgumentMatcherAndIndividualInvocationCountsInIteratingBlock()
   {
      for (int i = 0; i < 2; i++) {
         exerciseCodeUnderTest();
      }

      new Verifications(2) {{
         mock.prepare(); maxTimes = 1;
         mock.setSomething(anyInt); minTimes = 2;
         mock.editABunchMoreStuff(); minTimes = 0; maxTimes = 5;
         mock.save(); times = 1;
      }};
   }

   @Test
   public void verifyInvocationsToPrivateMethodsAndConstructors()
   {
      new Dependency(9).privateMethod();
      Dependency.privateStaticMethod("test", true);

      new Verifications() {{
         newInstance(Dependency.class.getName(), 9);
         invoke(mock, "privateMethod");
         invoke(Dependency.class, "privateStaticMethod", "test", true);
      }};
   }

   @Test
   public void verifyInvocationThatMatchesExpectationRecordedWithAnyMatcherButWithArgumentValueWhichDidNotOccur()
   {
      thrown.expect(MissingInvocation.class);
      thrown.expectMessage("with arguments: 45");

      new Expectations() {{ mock.setSomething(anyInt); }};

      mock.setSomething(123);

      new Verifications() {{ mock.setSomething(45); }};
   }

   @Test
   public void verifyTwoInvocationsWithIteratingBlockHavingExpectationRecordedAndSecondInvocationUnverified()
   {
      thrown.expect(MissingInvocation.class);
      thrown.expectMessage("with arguments: 123");

      new Expectations() {{ mock.setSomething(anyInt); }};

      mock.setSomething(123);
      mock.setSomething(45);

      new Verifications(2) {{ mock.setSomething(123); }};
   }

   @Test
   public void verityTwoInvocationsToMethodMatchedOnSpecificInstanceWithNoArgumentMatchers(
      @Injectable final Dependency dep)
   {
      dep.editABunchMoreStuff();
      dep.editABunchMoreStuff();

      new Verifications() {{
         dep.editABunchMoreStuff();
         times = 2;
      }};
   }

   @Test
   public void verifyInvocationWhichWasReplayedBeforeAndAfterBeingRecorded()
   {
      // First "replay":
      mock.prepare();
      new Verifications() {{ mock.prepare(); times = 1; }};

      // Previous replay state is discarded, with a new empty one being created:
      new Expectations() {{ mock.prepare(); }};
      new Verifications() {{ mock.prepare(); times = 0; }};

      // Second replay, but counts again as first since the previous one was discarded:
      mock.prepare();
      new Verifications() {{ mock.prepare(); times = 1; }};
   }
}
