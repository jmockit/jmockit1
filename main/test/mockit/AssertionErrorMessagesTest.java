/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit;

import org.junit.*;
import org.junit.rules.*;

import mockit.internal.expectations.invocation.*;

public final class AssertionErrorMessagesTest
{
   @Rule public final ExpectedException thrown = ExpectedException.none();

   static class Collaborator
   {
      void doSomething() {}
      void doSomething(@SuppressWarnings("unused") int i, @SuppressWarnings("unused") String s) {}
      void doSomethingElse(@SuppressWarnings("unused") String s) {}
   }

   @Mocked Collaborator mock;

   @Test
   public void unexpectedInvocationForRecordedExpectation()
   {
      new Expectations() {{ mock.doSomething(anyInt, anyString); times = 1; }};

      thrown.expect(UnexpectedInvocation.class);
      thrown.expectMessage("Unexpected invocation to");
      thrown.expectMessage("doSomething(2, \"xyz\")");

      mock.doSomething(1, "Abc");
      mock.doSomething(2, "xyz");
   }

   @Test
   public void unexpectedInvocationWhereExpectingAnotherForRecordedExpectations()
   {
      mock.doSomething(1, "Abc");
      mock.doSomething(2, "xyz");
      mock.doSomethingElse("test");

      thrown.expect(UnexpectedInvocation.class);
      thrown.expectMessage("doSomething(2, \"xyz\"");

      new VerificationsInOrder() {{
         mock.doSomething(anyInt, anyString); times = 1;
         mock.doSomethingElse(anyString);
      }};
   }

   @Test
   public void unexpectedInvocationForRecordedExpectationWithMaximumInvocationCountOfZero()
   {
      new Expectations() {{ mock.doSomething(anyInt, anyString); times = 0; }};

      thrown.expect(UnexpectedInvocation.class);
      thrown.expectMessage("1, \"Abc\"");

      mock.doSomething(1, "Abc");
   }

   @Test
   public void unexpectedInvocationForVerifiedExpectation()
   {
      mock.doSomething(123, "Test");
      mock.doSomethingElse("abc");

      thrown.expect(UnexpectedInvocation.class);
      thrown.expectMessage("123, \"Test\"");

      new Verifications() {{
         mock.doSomething(123, anyString);
         times = 0;
      }};
   }

   @Test
   public void unexpectedInvocationForExpectationsVerifiedInOrder()
   {
      mock.doSomethingElse("test");
      mock.doSomething(123, "Test");

      thrown.expect(UnexpectedInvocation.class);
      thrown.expectMessage("123, \"Test\"");

      new VerificationsInOrder() {{
         mock.doSomethingElse(anyString);
         mock.doSomething(anyInt, anyString); times = 0;
      }};
   }

   @Test
   public void unexpectedInvocationOnMethodWithNoParameters()
   {
      new Expectations() {{ mock.doSomethingElse(anyString); }};

      mock.doSomething();

      thrown.expect(UnexpectedInvocation.class);
      thrown.expectMessage("doSomething()\n   on mock instance");
      new FullVerifications(mock) {};
   }

   @Test
   public void missingInvocationForRecordedExpectation()
   {
      new Expectations() {{ mock.doSomething(anyInt, anyString); times = 2; }};

      thrown.expect(MissingInvocation.class);
      thrown.expectMessage("any int, any String");

      mock.doSomething(123, "Abc");
   }

   @Test
   public void missingInvocationForRecordedExpectationWhichGetsNonMatchingInvocationsAtReplayTime()
   {
      new Expectations() {{ mock.doSomethingElse("test"); }};

      thrown.expect(MissingInvocation.class);
      thrown.expectMessage("doSomethingElse(\"test\")");
      thrown.expectMessage("instead got:");
      thrown.expectMessage("doSomethingElse(\"Abc\")");
      thrown.expectMessage("doSomethingElse(\"\")");

      mock.doSomethingElse("Abc");
      mock.doSomething(1, "xy");
      mock.doSomethingElse("");
   }

   @Test
   public void missingInvocationForVerifiedExpectation()
   {
      thrown.expect(MissingInvocation.class);
      thrown.expectMessage("123, any String");

      new Verifications() {{ mock.doSomething(123, anyString); }};
   }

   @Test
   public void missingInvocationForVerifiedExpectationWhichGetsNonMatchingInvocationsAtReplayTime()
   {
      mock.doSomethingElse("Abc");
      mock.doSomething(1, "xy");
      mock.doSomethingElse("");

      thrown.expect(MissingInvocation.class);
      thrown.expectMessage("doSomethingElse(\"test\")");
      thrown.expectMessage("instead got:");
      thrown.expectMessage("doSomethingElse(\"Abc\")");
      thrown.expectMessage("doSomethingElse(\"\")");

      new Verifications() {{ mock.doSomethingElse("test"); }};
   }

   @Test
   public void missingInvocationForExpectationVerifiedInOrder()
   {
      mock.doSomething(123, "Test");

      thrown.expect(MissingInvocation.class);
      thrown.expectMessage("any int, any String");

      new FullVerificationsInOrder() {{
         mock.doSomething(anyInt, anyString);
         minTimes = 3;
      }};
   }

   @Test
   public void missingInvocationForFullyVerifiedExpectations()
   {
      mock.doSomething(123, "Abc");

      thrown.expect(MissingInvocation.class);
      thrown.expectMessage("any int, any String");

      new FullVerifications() {{
         mock.doSomething(anyInt, anyString);
         times = 2;
      }};
   }

   @Test
   public void missingInvocationForExpectationUsingMatcherForDifferentParameterType()
   {
      mock.doSomething(5, "");

      thrown.expect(MissingInvocation.class);
      thrown.expectMessage("any char");

      new Verifications() {{ mock.doSomething(anyChar, ""); }};
   }
}
