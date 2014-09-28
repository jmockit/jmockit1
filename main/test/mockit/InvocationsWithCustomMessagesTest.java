/*
 * Copyright (c) 2006-2014 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit;

import org.junit.*;
import org.junit.rules.*;
import static org.junit.Assert.*;

import mockit.internal.*;

public final class InvocationsWithCustomMessagesTest
{
   @Rule public final ExpectedException thrown = ExpectedException.none();

   static final String message = "custom message";
   @Mocked Collaborator mock;

   static class Collaborator
   {
      private int value;

      @SuppressWarnings("RedundantNoArgConstructor")
      Collaborator() {}

      private static String doInternal() { return "123"; }

      void provideSomeService() {}

      int getValue() { return value; }
      void setValue(int value) { this.value = value; }
   }

   @Test
   public void attemptToSpecifyErrorMessageWithNoExpectationRecorded()
   {
      thrown.expect(IllegalStateException.class);

      new Expectations() {{ $ = "error"; }};
   }

   @Test
   public void replayWithUnexpectedInvocation()
   {
      thrown.expect(MissingInvocation.class);

      new StrictExpectations() {{ mock.getValue(); $ = message; }};

      try {
         mock.provideSomeService();
      }
      catch (UnexpectedInvocation e) {
         if (e.getMessage().startsWith(message)) {
            return;
         }
      }

      throw new IllegalStateException("should not get here");
   }

   @Test
   public void replayStrictExpectationOnceMoreThanExpected()
   {
      thrown.expect(MissingInvocation.class);

      new StrictExpectations() {{
         Collaborator.doInternal();
         mock.provideSomeService(); minTimes = 1; $ = message; maxTimes = 2;
      }};

      Collaborator.doInternal();

      try {
         Collaborator.doInternal();
      }
      catch (UnexpectedInvocation e) {
         if (e.getMessage().startsWith(message)) {
            return;
         }
      }

      throw new IllegalStateException("should not get here");
   }

   @Test
   public void replayNonStrictExpectationOnceMoreThanExpected()
   {
      new Expectations() {{
         new Collaborator(); times = 1; $ = message;
      }};

      new Collaborator();

      try {
         new Collaborator();
      }
      catch (UnexpectedInvocation e) {
         assertTrue(e.getMessage().startsWith(message));
         return;
      }

      fail();
   }

   @Test
   public void replayWithMissingNonStrictExpectation()
   {
      thrown.expect(MissingInvocation.class);
      thrown.expectMessage(message);

      new Expectations() {{
         new Collaborator(); minTimes = 2; maxTimes = 3; $ = message;
      }};

      new Collaborator();
   }

   @Test
   public void replayWithMissingExpectedInvocation()
   {
      thrown.expect(MissingInvocation.class);
      thrown.expectMessage(message);

      new Expectations() {{
         mock.setValue(123); $ = message;
      }};
   }

   @Test
   public void replayUnexpectedStrictExpectation_showCustomMessageOfExpectedInvocation()
   {
      thrown.expect(UnexpectedInvocation.class);
      thrown.expectMessage(message);

      new StrictExpectations() {{
         mock.setValue(1); $ = message;
         mock.setValue(2);
      }};

      mock.setValue(2);
   }

   @Test
   public void attemptToSpecifyErrorMessageWithNoExpectationVerified()
   {
      thrown.expect(IllegalStateException.class);

      new Verifications() {{ $ = "error"; }};
   }

   @Test
   public void verifyInvocationThatDidNotOccur()
   {
      try {
         new Verifications() {{
            mock.provideSomeService();
            $ = message;
            times = 1;
         }};
      }
      catch (MissingInvocation e) {
         if (!e.getMessage().startsWith(message)) {
            throw new IllegalStateException("Missing custom message prefix", e);
         }
      }
   }

   @Test
   public void verifyMissingInvocationAfterOneThatDidOccur()
   {
      Collaborator.doInternal();
      Collaborator.doInternal();

      try {
         new VerificationsInOrder() {{
            Collaborator.doInternal();
            mock.provideSomeService(); $ = message; minTimes = 1; maxTimes = 2;
         }};
      }
      catch (MissingInvocation e) {
         if (!e.getMessage().startsWith(message)) {
            throw new IllegalStateException("Missing custom message prefix", e);
         }
      }
   }

   @Test
   public void verifyInvocationThatOccurredOnceMoreThanExpected()
   {
      new Collaborator();
      new Collaborator();

      try {
         new FullVerifications() {{
            new Collaborator();
            $ = message;
            times = 1;
         }};
      }
      catch (UnexpectedInvocation e) {
         assertTrue(e.getMessage().startsWith(message));
      }
   }

   @Test
   public void verifyUnexpectedInvocation()
   {
      mock.provideSomeService();
      mock.setValue(123);

      try {
         new FullVerificationsInOrder() {{
            mock.provideSomeService();
            mock.setValue(anyInt); $ = message; times = 0;
         }};
      }
      catch (UnexpectedInvocation e) {
         if (!e.getMessage().startsWith(message)) {
            throw new IllegalStateException("Missing custom message prefix", e);
         }
      }
   }
}