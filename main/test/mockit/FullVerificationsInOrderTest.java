/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit;

import org.junit.*;
import org.junit.rules.*;

import mockit.internal.*;

public final class FullVerificationsInOrderTest
{
   @Rule public final ExpectedException thrown = ExpectedException.none();

   @SuppressWarnings("UnusedParameters")
   public static class Dependency
   {
      public void setSomething(int value) {}
      public void setSomethingElse(char value) {}
      public int editABunchMoreStuff() { return 0; }
      public void notifyBeforeSave() {}
      public boolean prepare() { return false; }
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

      new FullVerificationsInOrder() {{
         mock.prepare(); minTimes = 1;
         mock.setSomething(anyInt); minTimes = 0; maxTimes = 2;
         mock.setSomethingElse(anyChar);
         mock.setSomething(anyInt); minTimes = 1; maxTimes = 2;
         mock.editABunchMoreStuff();
         mock.notifyBeforeSave(); maxTimes = 1;
         mock.save(); times = 1;
      }};
   }

   @Test
   public void verifyAllInvocationsWithSomeOfThemRecorded()
   {
      new Expectations() {{
         mock.prepare(); result = true;
         mock.editABunchMoreStuff(); result = 5;
      }};

      exerciseCodeUnderTest();

      new FullVerificationsInOrder() {{
         mock.prepare(); minTimes = 1;
         mock.setSomething(anyInt); minTimes = 0; maxTimes = 2;
         mock.setSomethingElse(anyChar);
         mock.setSomething(anyInt); minTimes = 1; maxTimes = 2;
         mock.editABunchMoreStuff();
         mock.notifyBeforeSave(); maxTimes = 1;
         mock.save(); times = 1;
      }};
   }

   @Test
   public void verifyAllInvocationsWithThoseRecordedAsExpectedToOccurVerifiedImplicitly()
   {
      new Expectations() {{
         mock.setSomething(45); times = 1;
         mock.editABunchMoreStuff(); result = 5; minTimes = 1;
      }};

      exerciseCodeUnderTest();

      new FullVerificationsInOrder() {{
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

      new VerificationsInOrder() {{
         mock.setSomething(45);
         mock.editABunchMoreStuff();
      }};

      new FullVerificationsInOrder() {{
         mock.prepare();
         mock.setSomething(123);
         mock.setSomethingElse(anyChar);
         mock.notifyBeforeSave();
         mock.save();
      }};
   }

   @Test
   public void verifyAllInvocationsWhenOutOfOrder()
   {
      thrown.expect(MissingInvocation.class);
      thrown.expectMessage("123");

      mock.setSomething(123);
      mock.prepare();

      new FullVerificationsInOrder() {{
         mock.prepare();
         mock.setSomething(123);
      }};
   }

   @Test
   public void verifyAllInvocationsWithSomeMissing()
   {
      thrown.expect(UnexpectedInvocation.class);
      thrown.expectMessage("45");

      exerciseCodeUnderTest();

      new FullVerificationsInOrder() {{
         mock.prepare();
         mock.setSomething(anyInt);
         mock.setSomethingElse(anyChar);
         mock.notifyBeforeSave();
         mock.save();
      }};
   }

   @Test
   public void verifyTwoInvocationsWithIteratingBlockHavingExpectationRecordedAndSecondInvocationUnverified()
   {
      thrown.expect(MissingInvocation.class);
      thrown.expectMessage("123");

      new Expectations() {{ mock.setSomething(anyInt); }};

      mock.setSomething(123);
      mock.setSomething(45);

      new FullVerificationsInOrder(2) {{
         mock.setSomething(123);
      }};
   }

   @Test
   public void verifyInvocationThatNeverHappens()
   {
      mock.prepare();
      mock.setSomething(123);

      new FullVerificationsInOrder() {{
         mock.prepare();
         mock.setSomething(123);
         mock.notifyBeforeSave(); times = 0;
      }};
   }

   @Test
   public void verifyInvocationThatShouldNeverHappenButDoes()
   {
      thrown.expect(UnexpectedInvocation.class);

      mock.setSomething(1);
      mock.notifyBeforeSave();

      new FullVerificationsInOrder() {{
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

      new FullVerificationsInOrder() {{
         mock.prepare();
         mock.setSomething(anyInt);
         mock.save(); minTimes = 0;
      }};
   }

   @Test
   public void verifyInvocationThatDoesNotHappen()
   {
      mock.prepare();
      mock.setSomething(123);

      new FullVerificationsInOrder() {{
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

      new FullVerificationsInOrder() {{
         mock.prepare();
         mock.setSomething(anyInt);
         mock.save(); minTimes = 0;
      }};
   }

   @Test
   public void verifyIntermediateUnrecordedInvocationThatDoesNotHappenButCould()
   {
      mock.prepare();
      mock.setSomething(123);

      new FullVerificationsInOrder() {{
         mock.prepare();
         mock.editABunchMoreStuff(); minTimes = 0;
         mock.setSomething(anyInt);
      }};
   }

   @Test
   public void verifyAllInvocationsWithExtraVerification()
   {
      thrown.expect(MissingInvocation.class);

      mock.prepare();
      mock.setSomething(123);

      new FullVerificationsInOrder() {{
         mock.prepare();
         mock.setSomething(123);
         mock.notifyBeforeSave();
      }};
   }

   @Test
   public void verifyAllInvocationsWithInvocationCountLessThanActual()
   {
      thrown.expect(UnexpectedInvocation.class);
      thrown.expectMessage("45");

      mock.setSomething(123);
      mock.setSomething(45);

      new FullVerificationsInOrder() {{
         mock.setSomething(anyInt); times = 1;
      }};
   }

   @Test
   public void verifyAllInvocationsWithInvocationCountMoreThanActual()
   {
      thrown.expect(MissingInvocation.class);
      thrown.expectMessage("any int");

      mock.setSomething(-67);

      new FullVerificationsInOrder() {{
         mock.setSomething(anyInt); minTimes = 3; maxTimes = 6;
      }};
   }

   @Test
   public void verifyAllInvocationsInIteratingBlock()
   {
      mock.setSomething(123);
      mock.save();
      mock.setSomething(45);
      mock.save();

      new FullVerificationsInOrder(2) {{
         mock.setSomething(anyInt);
         mock.save();
      }};
   }

   @Test
   public void verifySingleInvocationInBlockWithLargerNumberOfIterations()
   {
      thrown.expect(MissingInvocation.class);
      thrown.expectMessage("any int");

      mock.setSomething(123);

      new FullVerificationsInOrder(3) {{
         mock.setSomething(anyInt);
      }};
   }

   @Test
   public void verifyMultipleInvocationsInBlockWithSmallerNumberOfIterations()
   {
      thrown.expect(UnexpectedInvocation.class);
      thrown.expectMessage("45");

      mock.setSomething(123);
      mock.setSomething(45);

      new FullVerificationsInOrder(1) {{
         mock.setSomething(anyInt);
      }};
   }

   @Test
   public void verifyWithArgumentMatcherAndIndividualInvocationCountsInIteratingBlock()
   {
      for (int i = 0; i < 2; i++) {
         exerciseCodeUnderTest();
      }

      new FullVerificationsInOrder(2) {{
         mock.prepare(); maxTimes = 1;
         mock.setSomething(anyInt); minTimes = 1;
         mock.setSomethingElse('a');
         mock.setSomething(anyInt); maxTimes = 1;
         mock.editABunchMoreStuff(); minTimes = 0; maxTimes = 5;
         mock.notifyBeforeSave();
         mock.save(); times = 1;
      }};
   }
}
