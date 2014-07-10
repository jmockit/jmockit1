/*
 * Copyright (c) 2006-2013 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit;

import java.util.*;

import org.junit.*;

import mockit.internal.*;

public final class InvocationBlocksWithTimesFieldsTest
{
   private final CodeUnderTest codeUnderTest = new CodeUnderTest();

   static class CodeUnderTest
   {
      private final Collaborator dependency = new Collaborator();

      void doSomething()
      {
         dependency.provideSomeService();
      }

      void doSomethingElse()
      {
         dependency.simpleOperation(1, "b", null);
      }
   }

   static class Collaborator
   {
      Collaborator() {}

      @SuppressWarnings("UnusedDeclaration")
      Collaborator(int value) {}

      void provideSomeService() {}

      @SuppressWarnings("UnusedDeclaration")
      final void simpleOperation(int a, String b, Date c) {}
   }

   // Tests with recorded strict expectations /////////////////////////////////////////////////////////////////////////

   @Test
   public void strict_expectTwiceByUsingInvocationCount(@Mocked final Collaborator mock)
   {
      new Expectations() {{
         mock.provideSomeService(); times = 2;
         mock.simpleOperation(1, "b", null);
      }};

      codeUnderTest.doSomething();
      codeUnderTest.doSomething();
      codeUnderTest.doSomethingElse();
   }

   @Test(expected = UnexpectedInvocation.class)
   public void strict_expectTwiceByUsingInvocationCountButReplayOnlyOnce(@Mocked final Collaborator mock)
   {
      new Expectations() {{
         mock.provideSomeService(); times = 2;
         mock.simpleOperation(1, "b", null);
      }};

      codeUnderTest.doSomething();
      codeUnderTest.doSomethingElse();
   }

   @Test
   public void strict_expectAtLeastOnceAndReplayTwice(@Mocked final Collaborator mock)
   {
      new Expectations() {{
         mock.provideSomeService(); minTimes = 1;
         mock.simpleOperation(1, "b", null);
      }};

      codeUnderTest.doSomething();
      codeUnderTest.doSomething();
      codeUnderTest.doSomethingElse();
   }

   @Test(expected = MissingInvocation.class)
   public void strict_expectAtLeastTwiceButReplayOnceWithSingleExpectation(@Mocked final Collaborator mock)
   {
      new Expectations() {{
         mock.provideSomeService(); minTimes = 2;
      }};

      codeUnderTest.doSomething();
   }

   @Test(expected = UnexpectedInvocation.class)
   public void strict_expectAtLeastTwiceButReplayOnceWithTwoConsecutiveExpectations(@Mocked final Collaborator mock)
   {
      new Expectations() {{
         mock.provideSomeService(); minTimes = 2;
         mock.simpleOperation(1, "b", null);
      }};

      codeUnderTest.doSomething();
      codeUnderTest.doSomethingElse();
   }

   @Test
   public void strict_minTimesAndMaxTimesOutOfOrder(@Mocked final Collaborator mock)
   {
      new Expectations() {{
         mock.provideSomeService(); maxTimes = 2; minTimes = 1;
      }};

      codeUnderTest.doSomething();
      codeUnderTest.doSomething();
      codeUnderTest.doSomething();
   }

   @Test
   public void strict_expectAtMostTwiceAndReplayOnce(@Mocked final Collaborator mock)
   {
      new Expectations() {{
         mock.provideSomeService(); maxTimes = 2;
         mock.simpleOperation(1, "b", null);
      }};

      codeUnderTest.doSomething();
      codeUnderTest.doSomethingElse();
   }

   @Test(expected = UnexpectedInvocation.class)
   public void strict_expectAtMostOnceButReplayTwice(@Mocked final Collaborator mock)
   {
      new Expectations() {{
         mock.provideSomeService(); maxTimes = 1;
         mock.simpleOperation(1, "b", null);
      }};

      codeUnderTest.doSomething();
      codeUnderTest.doSomething();
      codeUnderTest.doSomethingElse();
   }

   @Test
   public void strict_expectAtMostZero(@Mocked final Collaborator mock)
   {
      new Expectations() {{
         mock.provideSomeService(); maxTimes = 0;
      }};
   }

   @Test(expected = UnexpectedInvocation.class)
   public void strict_expectAtMostZeroButReplayOnce(@Mocked final Collaborator mock)
   {
      new Expectations() {{
         mock.provideSomeService();
         maxTimes = 0;
      }};

      codeUnderTest.doSomething();
   }

   @Test(expected = MissingInvocation.class)
   public void strict_maxTimesDoesNotOverwriteMinTimes(@Mocked final Collaborator mock)
   {
      new Expectations() {{
         mock.provideSomeService(); minTimes = 2; maxTimes = 3;
      }};

      codeUnderTest.doSomething();
   }

   @Test
   public void strict_expectSameMethodOnceOrTwiceThenOnceButReplayEachExpectationOnlyOnce(
      @Mocked final Collaborator mock)
   {
      new Expectations() {{
         mock.simpleOperation(1, "", null); minTimes = 1; maxTimes = 2;
         mock.simpleOperation(2, "", null);
      }};

      mock.simpleOperation(1, "", null);
      mock.simpleOperation(2, "", null);
   }

   @Test
   public void strict_expectTwoOrThreeTimes(@Mocked final Collaborator mock)
   {
      new Expectations() {{
         mock.provideSomeService(); minTimes = 2; maxTimes = 3;
         mock.simpleOperation(1, "b", null);
      }};

      codeUnderTest.doSomething();
      codeUnderTest.doSomething();
      codeUnderTest.doSomethingElse();
   }

   @Test
   public void strict_expectZeroOrMoreTimesAndReplayTwice(@Mocked final Collaborator mock)
   {
      new Expectations() {{
         mock.provideSomeService(); minTimes = 0; maxTimes = -1;
         mock.simpleOperation(1, "b", null);
      }};

      codeUnderTest.doSomething();
      codeUnderTest.doSomething();
      codeUnderTest.doSomethingElse();
   }

   @Test
   public void strict_expectZeroOrMoreTimesAndReplayNone(@Mocked final Collaborator mock)
   {
      new Expectations() {{
         mock.provideSomeService(); minTimes = 0; maxTimes = -1;
         mock.simpleOperation(1, "b", null);
      }};

      codeUnderTest.doSomethingElse();
   }

   // Tests with recorded non-strict expectations /////////////////////////////////////////////////////////////////////

   @Test
   public void nonStrict_expectTwiceByUsingInvocationCount(@Mocked final Collaborator mock)
   {
      new NonStrictExpectations() {{
         mock.provideSomeService(); times = 2;
         mock.simpleOperation(1, "b", null);
      }};

      codeUnderTest.doSomething();
      codeUnderTest.doSomethingElse();
      codeUnderTest.doSomething();
   }

   @Test(expected = MissingInvocation.class)
   public void nonStrict_expectTwiceByUsingInvocationCountButReplayOnlyOnce(@Mocked final Collaborator mock)
   {
      new NonStrictExpectations() {{
         mock.simpleOperation(1, "b", null);
         mock.provideSomeService(); times = 2;
      }};

      codeUnderTest.doSomething();
      codeUnderTest.doSomethingElse();
   }

   @Test
   public void nonStrict_expectAtLeastOnceAndReplayTwice(@Mocked final Collaborator mock)
   {
      new NonStrictExpectations() {{
         mock.provideSomeService(); minTimes = 1;
         mock.simpleOperation(1, "b", null);
      }};

      codeUnderTest.doSomethingElse();
      codeUnderTest.doSomething();
      codeUnderTest.doSomething();
   }

   @Test
   public void nonStrict_minTimesAndMaxTimesOutOfOrder(@Mocked final Collaborator mock)
   {
      new NonStrictExpectations() {{
         mock.provideSomeService(); maxTimes = 2; minTimes = 1;
      }};

      codeUnderTest.doSomething();
      codeUnderTest.doSomething();
      codeUnderTest.doSomething();
   }

   @Test
   public void nonStrict_expectAtMostTwiceAndReplayOnce(@Mocked final Collaborator mock)
   {
      new NonStrictExpectations() {{
         mock.provideSomeService(); maxTimes = 2;
         mock.simpleOperation(1, "b", null);
      }};

      codeUnderTest.doSomethingElse();
      codeUnderTest.doSomething();
   }

   @Test(expected = UnexpectedInvocation.class)
   public void nonStrict_expectAtMostOnceButReplayTwice(@Mocked final Collaborator mock)
   {
      new NonStrictExpectations() {{
         mock.simpleOperation(1, "b", null);
         mock.provideSomeService(); maxTimes = 1;
      }};

      codeUnderTest.doSomething();
      codeUnderTest.doSomething();
      codeUnderTest.doSomethingElse();
   }

   @Test
   public void nonStrict_expectAtMostZero(@Mocked final Collaborator mock)
   {
      new NonStrictExpectations() {{
         mock.provideSomeService(); maxTimes = 0;
      }};
   }

   @Test(expected = UnexpectedInvocation.class)
   public void nonStrict_expectAtMostZeroButReplayOnce(@Mocked final Collaborator mock)
   {
      new Expectations() {{
         mock.provideSomeService(); maxTimes = 0;
      }};

      codeUnderTest.doSomething();
   }

   @Test(expected = MissingInvocation.class)
   public void nonStrict_maxTimesDoesNotOverwriteMinTimes(@Mocked final Collaborator mock)
   {
      new NonStrictExpectations() {{
         mock.provideSomeService(); minTimes = 2; maxTimes = 3;
      }};

      codeUnderTest.doSomething();
   }

   @Test
   public void nonStrict_expectSameMethodOnceOrTwiceThenOnceButReplayEachExpectationOnlyOnce(
      @Mocked final Collaborator mock)
   {
      new NonStrictExpectations() {{
         mock.simpleOperation(1, "", null); minTimes = 1; maxTimes = 2;
         mock.simpleOperation(2, "", null);
      }};

      mock.simpleOperation(2, "", null);
      mock.simpleOperation(1, "", null);
   }

   @Test
   public void nonStrict_expectTwoOrThreeTimes(@Mocked final Collaborator mock)
   {
      new NonStrictExpectations() {{
         mock.provideSomeService(); minTimes = 2; maxTimes = 3;
         mock.simpleOperation(1, "b", null);
      }};

      codeUnderTest.doSomething();
      codeUnderTest.doSomethingElse();
      codeUnderTest.doSomething();
   }

   @Test
   public void nonStrict_expectZeroOrMoreTimesAndReplayTwice()
   {
      final Collaborator collaborator = new Collaborator();

      new NonStrictExpectations(collaborator) {{
         collaborator.simpleOperation(1, "b", null);
         collaborator.provideSomeService(); minTimes = 0; maxTimes = -1;
      }};

      codeUnderTest.doSomething();
      codeUnderTest.doSomethingElse();
      codeUnderTest.doSomething();
   }

   @Test
   public void nonStrict_expectZeroOrMoreTimesAndReplayNone(@Mocked final Collaborator mock)
   {
      new NonStrictExpectations() {{
         mock.provideSomeService(); minTimes = 0; maxTimes = -1;
         mock.simpleOperation(1, "b", null);
      }};

      codeUnderTest.doSomethingElse();
   }

   // Tests with ordered verifications ////////////////////////////////////////////////////////////////////////////////

   @Test
   public void ordered_verifyTwiceByUsingInvocationCount(@Mocked final Collaborator mock)
   {
      codeUnderTest.doSomething();
      codeUnderTest.doSomething();
      codeUnderTest.doSomethingElse();

      new VerificationsInOrder() {{
         mock.provideSomeService(); times = 2;
         mock.simpleOperation(1, "b", null);
      }};
   }

   @Test(expected = MissingInvocation.class)
   public void ordered_verifyTwiceByUsingInvocationCountButReplayOnlyOnce(@Mocked final Collaborator mock)
   {
      codeUnderTest.doSomethingElse();
      codeUnderTest.doSomething();

      new FullVerificationsInOrder() {{
         mock.simpleOperation(1, "b", null);
         mock.provideSomeService(); times = 2;
      }};
   }

   @Test
   public void ordered_verifyAtLeastOnceAndReplayTwice(@Mocked final Collaborator mock)
   {
      codeUnderTest.doSomething();
      codeUnderTest.doSomething();
      codeUnderTest.doSomethingElse();

      new VerificationsInOrder() {{
         mock.provideSomeService(); minTimes = 1;
         mock.simpleOperation(1, "b", null);
      }};
   }

   @Test(expected = UnexpectedInvocation.class)
   public void ordered_minTimesAndMaxTimesOutOfOrder(@Mocked final Collaborator mock)
   {
      codeUnderTest.doSomething();
      codeUnderTest.doSomething();
      codeUnderTest.doSomething();

      new VerificationsInOrder() {{
         mock.provideSomeService(); maxTimes = 2; minTimes = 1;
      }};
   }

   @Test
   public void ordered_verifyAtMostTwiceAndReplayOnce(@Mocked final Collaborator mock)
   {
      codeUnderTest.doSomething();
      codeUnderTest.doSomethingElse();

      new FullVerificationsInOrder() {{
         mock.provideSomeService(); maxTimes = 2;
         mock.simpleOperation(1, "b", null);
      }};
   }

   @Test(expected = UnexpectedInvocation.class)
   public void ordered_verifyAtMostOnceButReplayTwice(@Mocked final Collaborator mock)
   {
      codeUnderTest.doSomething();
      codeUnderTest.doSomething();
      codeUnderTest.doSomethingElse();

      new VerificationsInOrder() {{
         mock.provideSomeService(); maxTimes = 1;
         mock.simpleOperation(1, "b", null);
      }};
   }

   @Test
   public void ordered_verifyAtMostZero(@Mocked final Collaborator mock)
   {
      new VerificationsInOrder() {{
         mock.provideSomeService(); maxTimes = 0;
      }};
   }

   @Test(expected = UnexpectedInvocation.class)
   public void ordered_verifyAtMostZeroButReplayOnce(@Mocked final Collaborator mock)
   {
      codeUnderTest.doSomething();

      new VerificationsInOrder() {{
         mock.provideSomeService(); maxTimes = 0;
      }};
   }

   @Test(expected = MissingInvocation.class)
   public void ordered_maxTimesDoesNotOverwriteMinTimes(@Mocked final Collaborator mock)
   {
      codeUnderTest.doSomething();

      new FullVerificationsInOrder() {{
         mock.provideSomeService(); minTimes = 2; maxTimes = 3;
      }};
   }

   @Test
   public void ordered_verifySameMethodOnceOrTwiceThenOnceButReplayEachExpectationOnlyOnce(
      @Mocked final Collaborator mock)
   {
      mock.simpleOperation(1, "", null);
      mock.simpleOperation(2, "", null);

      new VerificationsInOrder() {{
         mock.simpleOperation(1, "", null); minTimes = 1; maxTimes = 2;
         mock.simpleOperation(2, "", null);
      }};
   }

   @Test
   public void ordered_verifyTwoOrThreeTimes(@Mocked final Collaborator mock)
   {
      codeUnderTest.doSomething();
      codeUnderTest.doSomething();
      codeUnderTest.doSomethingElse();

      new VerificationsInOrder() {{
         mock.provideSomeService(); minTimes = 2; maxTimes = 3;
         mock.simpleOperation(1, "b", null);
      }};
   }

   @SuppressWarnings("UnusedDeclaration")
   @Test
   public void ordered_verifyZeroOrMoreTimesAndReplayTwice(@Mocked Collaborator mock)
   {
      codeUnderTest.doSomethingElse();
      codeUnderTest.doSomething();
      codeUnderTest.doSomething();

      final Collaborator collaborator = new Collaborator();

      new VerificationsInOrder() {{
         collaborator.simpleOperation(1, "b", null);
         collaborator.provideSomeService(); minTimes = 0; maxTimes = -1;
      }};
   }

   @Test
   public void ordered_verifyZeroOrMoreTimesAndReplayNone(@Mocked final Collaborator mock)
   {
      codeUnderTest.doSomethingElse();

      new FullVerificationsInOrder() {{
         mock.provideSomeService(); minTimes = 0; maxTimes = -1;
         mock.simpleOperation(1, "b", null);
      }};
   }

   // Tests with unordered verifications //////////////////////////////////////////////////////////////////////////////

   @Test
   public void unordered_verifyTwiceByUsingInvocationCount(@Mocked final Collaborator mock)
   {
      codeUnderTest.doSomething();
      codeUnderTest.doSomethingElse();
      codeUnderTest.doSomething();

      new Verifications() {{
         mock.provideSomeService(); times = 2;
         mock.simpleOperation(1, "b", null);
      }};
   }

   @Test(expected = MissingInvocation.class)
   public void unordered_verifyTwiceByUsingInvocationCountButReplayOnlyOnce(@Mocked final Collaborator mock)
   {
      codeUnderTest.doSomethingElse();
      codeUnderTest.doSomething();

      new FullVerifications() {{
         mock.provideSomeService(); times = 2;
         mock.simpleOperation(1, "b", null);
      }};
   }

   @Test
   public void unordered_verifyAtLeastOnceAndReplayTwice(@Mocked final Collaborator mock)
   {
      codeUnderTest.doSomethingElse();
      codeUnderTest.doSomething();
      codeUnderTest.doSomething();

      new Verifications() {{
         mock.provideSomeService(); minTimes = 1;
         mock.simpleOperation(1, "b", null);
      }};
   }

   @Test
   public void unordered_minTimesAndMaxTimesOutOfOrder(@Mocked final Collaborator mock)
   {
      codeUnderTest.doSomething();
      codeUnderTest.doSomething();
      codeUnderTest.doSomething();

      new Verifications() {{
         mock.provideSomeService(); maxTimes = 2; minTimes = 1;
      }};
   }

   @Test
   public void unordered_verifyAtMostTwiceAndReplayOnce(@Mocked final Collaborator mock)
   {
      codeUnderTest.doSomething();
      codeUnderTest.doSomethingElse();

      new FullVerifications() {{
         mock.simpleOperation(1, "b", null);
         mock.provideSomeService(); maxTimes = 2;
      }};
   }

   @Test(expected = UnexpectedInvocation.class)
   public void unordered_verifyAtMostOnceButReplayTwice(@Mocked final Collaborator mock)
   {
      codeUnderTest.doSomething();
      codeUnderTest.doSomethingElse();
      codeUnderTest.doSomething();

      new Verifications() {{
         mock.provideSomeService(); maxTimes = 1;
         mock.simpleOperation(1, "b", null);
      }};
   }

   @Test
   public void unordered_verifyAtMostZero(@Mocked final Collaborator mock)
   {
      new Verifications() {{
         mock.provideSomeService(); maxTimes = 0;
      }};
   }

   @Test(expected = UnexpectedInvocation.class)
   public void unordered_verifyAtMostZeroButReplayOnce(@Mocked final Collaborator mock)
   {
      codeUnderTest.doSomething();

      new Verifications() {{
         mock.provideSomeService(); maxTimes = 0;
      }};
   }

   @Test(expected = MissingInvocation.class)
   public void unordered_maxTimesDoesNotOverwriteMinTimes(@Mocked final Collaborator mock)
   {
      codeUnderTest.doSomething();

      new FullVerifications() {{
         mock.provideSomeService(); minTimes = 2; maxTimes = 3;
      }};
   }

   @Test
   public void unordered_verifySameMethodOnceOrTwiceThenOnceButReplayEachExpectationOnlyOnce(
      @Mocked final Collaborator mock)
   {
      mock.simpleOperation(2, "", null);
      mock.simpleOperation(1, "", null);

      new Verifications() {{
         mock.simpleOperation(1, "", null); minTimes = 1; maxTimes = 2;
         mock.simpleOperation(2, "", null);
      }};
   }

   @Test
   public void unordered_verifyTwoOrThreeTimes(@Mocked final Collaborator mock)
   {
      codeUnderTest.doSomething();
      codeUnderTest.doSomethingElse();
      codeUnderTest.doSomething();

      new FullVerifications() {{
         mock.simpleOperation(1, "b", null);
         mock.provideSomeService(); minTimes = 2; maxTimes = 3;
      }};
   }

   @SuppressWarnings("UnusedDeclaration")
   @Test
   public void unordered_verifyZeroOrMoreTimesAndReplayTwice(@Mocked Collaborator mock)
   {
      codeUnderTest.doSomething();
      codeUnderTest.doSomethingElse();
      codeUnderTest.doSomething();

      final Collaborator collaborator = new Collaborator();

      new Verifications() {{
         collaborator.simpleOperation(1, "b", null);
         collaborator.provideSomeService(); minTimes = 0; maxTimes = -1;
      }};
   }

   @Test
   public void unordered_verifyZeroOrMoreTimesAndReplayNone(@Mocked final Collaborator mock)
   {
      codeUnderTest.doSomethingElse();

      new Verifications() {{
         mock.simpleOperation(1, "b", null);
         mock.provideSomeService(); minTimes = 0; maxTimes = -1;
      }};
   }
}