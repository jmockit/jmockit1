/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit;

import org.junit.*;
import org.junit.rules.*;

import mockit.internal.expectations.invocation.*;

public final class FullVerificationsTest
{
   @Rule public final ExpectedException thrown = ExpectedException.none();

   public static class Dependency
   {
      public void setSomething(@SuppressWarnings("unused") int value) {}
      public void setSomethingElse(@SuppressWarnings("unused") char value) {}
      public boolean editABunchMoreStuff() { return false; }
      public void notifyBeforeSave() {}
      public void prepare() {}
      public void save() {}
   }

   @Mocked Dependency mock;

   void exerciseCodeUnderTest()
   {
      mock.prepare();
      mock.setSomething(123);
      mock.setSomethingElse('a');
      mock.setSomething(45);
      mock.editABunchMoreStuff();
      mock.notifyBeforeSave();
      mock.save();
   }

   @Test
   public void verifyAllInvocations()
   {
      exerciseCodeUnderTest();

      new FullVerifications() {{
         mock.prepare(); minTimes = 1;
         mock.editABunchMoreStuff();
         mock.notifyBeforeSave(); maxTimes = 1;
         mock.setSomething(anyInt); minTimes = 0; maxTimes = 2;
         mock.setSomethingElse(anyChar);
         mock.save(); times = 1;
      }};
   }

   @Test
   public void verifyAllInvocationsWithSomeOfThemRecorded()
   {
      new Expectations() {{
         mock.editABunchMoreStuff(); result = true;
         mock.setSomething(45);
      }};

      exerciseCodeUnderTest();

      new FullVerifications() {{
         mock.prepare();
         mock.setSomething(anyInt);
         mock.setSomethingElse(anyChar);
         mock.notifyBeforeSave();
         mock.save();
      }};
   }

   @Test
   public void verifyAllInvocationsWithThoseRecordedAsExpectedToOccurVerifiedImplicitly()
   {
      new Expectations() {{
         mock.setSomething(45); times = 1;
         mock.editABunchMoreStuff(); result = true; minTimes = 1;
      }};

      exerciseCodeUnderTest();

      new FullVerifications() {{
         mock.prepare();
         mock.setSomething(123);
         mock.setSomethingElse(anyChar);
         mock.notifyBeforeSave();
         mock.save();
      }};
   }

   @Test
   public void verifyAllInvocationsExceptThoseAlreadyVerifiedInAPreviousVerificationBlock()
   {
      exerciseCodeUnderTest();

      new Verifications() {{
         mock.setSomething(45);
         mock.editABunchMoreStuff();
      }};

      new FullVerifications() {{
         mock.prepare();
         mock.setSomething(123);
         mock.setSomethingElse(anyChar);
         mock.notifyBeforeSave();
         mock.save();
      }};
   }

   @Test
   public void verifyAllInvocationsWithOneMissing()
   {
      thrown.expect(UnexpectedInvocation.class);
      thrown.expectMessage("editABunchMoreStuff()");

      exerciseCodeUnderTest();

      new FullVerifications() {{
         mock.prepare();
         mock.notifyBeforeSave();
         mock.setSomething(anyInt);
         mock.setSomethingElse(anyChar);
         mock.save();
      }};
   }

   @Test
   public void verifyUnrecordedInvocationThatWasExpectedToNotHappen()
   {
      mock.prepare();
      mock.setSomething(123);
      mock.setSomething(45);

      new FullVerifications() {{
         mock.prepare();
         mock.setSomething(anyInt); times = 2;
         mock.notifyBeforeSave(); times = 0;
      }};
   }

   @Test
   public void verifyUnrecordedInvocationThatShouldNotHappenButDoes()
   {
      thrown.expect(UnexpectedInvocation.class);
      thrown.expectMessage("1 unexpected invocation");

      mock.setSomething(1);
      mock.notifyBeforeSave();

      new FullVerifications() {{
         mock.setSomething(1);
         mock.notifyBeforeSave(); times = 0;
      }};
   }

   @Test
   public void verifyInvocationThatIsAllowedToHappenAnyNumberOfTimesAndHappensOnce()
   {
      mock.prepare();
      mock.setSomething(123);
      mock.save();

      new FullVerifications() {{
         mock.prepare();
         mock.setSomething(anyInt);
         mock.save(); minTimes = 0;
      }};
   }

   @Test
   public void verifyRecordedInvocationThatIsAllowedToHappenAnyNoOfTimesAndDoesNotHappen()
   {
      mock.prepare();
      mock.setSomething(123);

      new FullVerifications() {{
         mock.prepare();
         mock.setSomething(anyInt);
         mock.save(); minTimes = 0;
      }};
   }

   @Test
   public void verifyUnrecordedInvocationThatIsAllowedToHappenAnyNoOfTimesAndDoesNotHappen()
   {
      mock.prepare();
      mock.setSomething(123);

      new FullVerifications() {{
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

      new FullVerifications() {{ mock.notifyBeforeSave(); }};
   }

   @Test
   public void verifyRecordedInvocationThatShouldHappenButDoesNot()
   {
      thrown.expect(MissingInvocation.class);

      new Expectations() {{ mock.notifyBeforeSave(); }};

      mock.setSomething(1);

      new FullVerifications() {{ mock.notifyBeforeSave(); }};
   }

   @Test
   public void verifyAllInvocationsWithExtraVerification()
   {
      thrown.expect(MissingInvocation.class);
      thrown.expectMessage("notifyBeforeSave()");

      mock.prepare();
      mock.setSomething(123);

      new FullVerifications() {{
         mock.prepare();
         mock.setSomething(123);
         mock.notifyBeforeSave();
      }};
   }

   @Test
   public void verifyAllInvocationsWithInvocationCountOneLessThanActual()
   {
      thrown.expect(UnexpectedInvocation.class);
      thrown.expectMessage("45");

      mock.setSomething(123);
      mock.setSomething(45);

      new FullVerifications() {{
         mock.setSomething(anyInt); times = 1;
      }};
   }

   @Test
   public void verifyAllInvocationsWithInvocationCountTwoLessThanActual()
   {
      thrown.expect(UnexpectedInvocation.class);
      thrown.expectMessage("2 unexpected invocations");
      thrown.expectMessage("1");

      mock.setSomething(123);
      mock.setSomething(45);
      mock.setSomething(1);

      new FullVerifications() {{
         mock.setSomething(anyInt); times = 1;
      }};
   }

   @Test
   public void verifyAllInvocationsWithInvocationCountMoreThanActual()
   {
      thrown.expect(MissingInvocation.class);
      thrown.expectMessage("Missing 2 invocations");
      thrown.expectMessage("any char");

      mock.setSomethingElse('f');

      new FullVerifications() {{
         mock.setSomethingElse(anyChar);
         times = 3;
      }};
   }

   @Test
   public void verifyNoInvocationsOccurredOnMockedDependencyWithOneHavingOccurred()
   {
      thrown.expect(UnexpectedInvocation.class);

      mock.editABunchMoreStuff();

      new FullVerifications() {};
   }

   @Test
   public void verifyNoInvocationsOnMockedDependencyBeyondThoseRecordedAsExpected()
   {
      new Expectations() {{
         mock.prepare(); times = 1;
      }};

      new Expectations() {{
         mock.setSomething(anyInt); minTimes = 1;
         mock.save(); times = 1;
      }};

      mock.prepare();
      mock.setSomething(1);
      mock.setSomething(2);
      mock.save();

      new FullVerifications() {};
   }

   @Test
   public void verifyNoInvocationsOnMockedDependencyBeyondThoseRecordedAsExpectedWithOneHavingOccurred()
   {
      thrown.expect(UnexpectedInvocation.class);
      thrown.expectMessage("editABunchMoreStuff()");

      new Expectations() {{
         mock.prepare(); times = 1;
         mock.save(); minTimes = 1;
      }};

      mock.prepare();
      mock.editABunchMoreStuff();
      mock.save();

      new FullVerifications() {};
   }
}
