/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit;

import java.util.*;

import org.junit.*;

import static org.junit.Assert.*;

import mockit.internal.expectations.invocation.*;

public final class ExpectationsWithInvocationCountsTest
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

   @Test
   public void expectOnce(@Mocked final Collaborator mock)
   {
      new Expectations() {{ mock.provideSomeService(); }};

      codeUnderTest.doSomething();
   }

   @Test(expected = UnexpectedInvocation.class)
   public void expectOnceButReplayTwice(@Mocked final Collaborator mock)
   {
      new Expectations() {{ mock.provideSomeService(); times = 1; }};

      codeUnderTest.doSomething();
      codeUnderTest.doSomething();

      fail("Should not get here");
   }

   @Test(expected = UnexpectedInvocation.class)
   public void expectOnceButReplayMoreTimes(@Mocked final Collaborator mock)
   {
      new Expectations() {{ mock.provideSomeService(); times = 1; }};

      codeUnderTest.doSomething();

      try {
         codeUnderTest.doSomething();
      }
      finally {
         codeUnderTest.doSomethingElse();
      }

      fail("Should not get here");
   }

   @Test
   public void catchUnexpectedInvocationAndContinue(@Mocked final Collaborator mock)
   {
      new Expectations() {{ mock.provideSomeService(); maxTimes = 0; }};

      try {
         mock.provideSomeService();
      }
      catch (UnexpectedInvocation ignore) {}

      mock.simpleOperation(1, "", null);
   }

   @Test
   public void expectTwiceByUsingInvocationCount(@Mocked final Collaborator mock)
   {
      new Expectations() {{
         mock.provideSomeService(); times = 2;
         mock.simpleOperation(1, "b", null);
      }};

      codeUnderTest.doSomething();
      codeUnderTest.doSomething();
      codeUnderTest.doSomethingElse();
   }

   @Test(expected = MissingInvocation.class)
   public void expectTwiceByUsingInvocationCountButReplayOnlyOnce(@Mocked final Collaborator mock)
   {
      new Expectations() {{
         mock.provideSomeService(); times = 2;
         mock.simpleOperation(1, "b", null);
      }};

      codeUnderTest.doSomething();
      codeUnderTest.doSomethingElse();
   }

   @Test(expected = UnexpectedInvocation.class)
   public void expectExactlyTwiceButReplayMoreTimes(@Mocked final Collaborator mock)
   {
      new Expectations() {{
         mock.provideSomeService(); times = 2;
      }};

      codeUnderTest.doSomething();
      codeUnderTest.doSomething();
      codeUnderTest.doSomething();
   }

   @Test
   public void expectAtLeastOnceAndReplayTwice(@Mocked final Collaborator mock)
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
   public void expectAtLeastTwiceButReplayOnceWithSingleExpectation(@Mocked final Collaborator mock)
   {
      new Expectations() {{
         mock.provideSomeService(); minTimes = 2;
      }};

      codeUnderTest.doSomething();
   }

   @Test(expected = MissingInvocation.class)
   public void expectAtLeastTwiceButReplayOnceWithTwoConsecutiveExpectations(@Mocked final Collaborator mock)
   {
      new Expectations() {{
         mock.provideSomeService(); minTimes = 2;
         mock.simpleOperation(1, "b", null);
      }};

      codeUnderTest.doSomething();
      codeUnderTest.doSomethingElse();
   }

   @Test
   public void repeatsAtLeastOverwritingUpperLimit(@Mocked final Collaborator mock)
   {
      new Expectations() {{
         mock.provideSomeService(); maxTimes = 2; minTimes = 1;
      }};

      codeUnderTest.doSomething();
      codeUnderTest.doSomething();
      codeUnderTest.doSomething();
   }

   @Test
   public void expectAtMostTwiceAndReplayOnce(@Mocked final Collaborator mock)
   {
      new Expectations() {{
         mock.provideSomeService(); maxTimes = 2;
         mock.simpleOperation(1, "b", null);
      }};

      codeUnderTest.doSomething();
      codeUnderTest.doSomethingElse();
   }

   @Test(expected = UnexpectedInvocation.class)
   public void expectAtMostOnceButReplayTwice(@Mocked final Collaborator mock)
   {
      new Expectations() {{
         mock.provideSomeService(); maxTimes = 1;
         mock.simpleOperation(1, "b", null);
      }};

      codeUnderTest.doSomething();
      codeUnderTest.doSomething();
      codeUnderTest.doSomethingElse();
   }

   @Test(expected = MissingInvocation.class)
   public void repeatsAtMostDoesNotOverwriteLowerLimit(@Mocked final Collaborator mock)
   {
      new Expectations() {{
         mock.provideSomeService(); minTimes = 2; maxTimes = 3;
      }};

      codeUnderTest.doSomething();
   }

   @Test
   public void expectSameMethodOnceOrTwiceThenOnceButReplayEachExpectationOnlyOnce(
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
   public void expectTwoOrThreeTimes(@Mocked final Collaborator mock)
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
   public void expectZeroOrMoreTimesAndReplayTwice(@Mocked final Collaborator mock)
   {
      new Expectations() {{
         mock.provideSomeService(); minTimes = 0; maxTimes = -1;
         mock.simpleOperation(1, "b", null);
      }};

      codeUnderTest.doSomething();
      codeUnderTest.doSomething();
      codeUnderTest.doSomethingElse();
   }

   @Test(expected = MissingInvocation.class)
   public void expectAtLeastOneInvocationMatchingStrictExpectationButInvokeNone(@Mocked final Collaborator a)
   {
      new Expectations() {{
         a.provideSomeService(); maxTimes = -1;
      }};

      // Do nothing at replay time.
   }

   @Test(expected = MissingInvocation.class)
   public void expectOneOrMoreInvocationsFollowedByAnotherWhichWontOccur_maxTimes(@Mocked final Collaborator mock)
   {
      new Expectations() {{
         mock.provideSomeService(); maxTimes = -1;
         mock.simpleOperation(1, null, null);
      }};

      codeUnderTest.doSomething();
   }

   @Test(expected = MissingInvocation.class)
   public void expectOneOrMoreInvocationsFollowedByAnotherWhichWontOccur_minTimes(@Mocked final Collaborator mock)
   {
      new Expectations() {{
         mock.simpleOperation(1, anyString, null); minTimes = 1;
         mock.provideSomeService();
      }};

      codeUnderTest.doSomethingElse();
   }
}
