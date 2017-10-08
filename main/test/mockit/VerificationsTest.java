/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit;

import org.junit.*;
import org.junit.rules.*;
import static org.junit.Assert.*;

import mockit.internal.expectations.invocation.*;

public final class VerificationsTest
{
   @Rule public final ExpectedException thrown = ExpectedException.none();

   @SuppressWarnings("UnusedParameters")
   public static class Dependency
   {
      public void setSomething(int value) {}
      public void setSomethingElse(String value) {}
      public void editABunchMoreStuff() {}
      public void notifyBeforeSave() {}
      public void prepare() {}
      public void save() {}
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
      thrown.expectMessage("45");

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
   public void verifyInvocationThatIsAllowedToHappenOnceOrMoreAndHappensOnce()
   {
      mock.prepare();
      mock.setSomething(123);
      mock.save();

      new Verifications() {{
         mock.setSomething(anyInt);
         mock.save();
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
      thrown.expectMessage("any int");

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
      thrown.expectMessage("5");

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
   public void verifyInvocationThatShouldNotOccurButDid()
   {
      thrown.expect(UnexpectedInvocation.class);
      thrown.expectMessage("2 unexpected invocations");
      thrown.expectMessage("123");

      mock.setSomething(5);
      mock.setSomething(123);

      new Verifications() {{
         mock.setSomething(anyInt);
         maxTimes = 0;
      }};
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

      new Verifications() {{
         mock.prepare(); maxTimes = 1;
         mock.setSomething(anyInt); minTimes = 2;
         mock.editABunchMoreStuff(); maxTimes = 5;
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
         //noinspection ConstantConditions
         assertEquals("not empty", 0, value.length());
      }};
   }

   @Test
   public void verifyWithCustomArgumentMatcher()
   {
      thrown.expect(MissingInvocation.class);
      thrown.expectMessage("isEmpty(\"test\")");

      mock.setSomethingElse("test");

      new Verifications() {{
         mock.setSomethingElse(with(new Delegate<String>() {
            @Mock boolean isEmpty(String s) { return s.isEmpty(); }
         }));
      }};
   }

   @Test
   public void verifyInvocationThatMatchesExpectationRecordedWithAnyMatcherButWithArgumentValueWhichDidNotOccur()
   {
      thrown.expect(MissingInvocation.class);
      thrown.expectMessage("45");

      new Expectations() {{ mock.setSomething(anyInt); }};

      mock.setSomething(123);

      new Verifications() {{ mock.setSomething(45); }};
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
}
