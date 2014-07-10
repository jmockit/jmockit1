/*
 * Copyright (c) 2006-2012 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit;

import org.junit.*;
import static org.junit.Assert.*;

public final class MultithreadedExpectationsTest
{
   static class Collaborator
   {
      int doSomething() { return -1; }
      void doSomethingElse() {}
   }

   @Mocked Collaborator mock;

   private void useMockedCollaboratorFromWorkerThread()
   {
      Thread worker = new Thread() {
         @Override public void run() { mock.doSomethingElse(); }
      };
      worker.start();
      try { worker.join(); } catch (InterruptedException ignore) {}
   }

   @Test
   public void useMockedObjectFromWorkerThreadWhileVerifyingExpectation()
   {
      mock.doSomething();
      mock.doSomething();

      new Verifications() {{
         mock.doSomething();
         useMockedCollaboratorFromWorkerThread();
         times = 2;
      }};
   }

   @Test
   public void useMockedObjectFromWorkerThreadWhileRecordingAndVerifyingExpectation()
   {
      new NonStrictExpectations() {{
         mock.doSomething();
         useMockedCollaboratorFromWorkerThread();
         result = 123;
      }};

      assertEquals(123, mock.doSomething());
      mock.doSomethingElse();

      new VerificationsInOrder() {{
         useMockedCollaboratorFromWorkerThread();
         mock.doSomething();
         mock.doSomethingElse();
      }};
   }

   @Test
   public void useMockedObjectFromWorkerThreadWhileRecordingStrictExpectation()
   {
      new Expectations() {{
         mock.doSomething();
         useMockedCollaboratorFromWorkerThread();
         result = 123;
      }};

      assertEquals(123, mock.doSomething());
   }

   @Test
   public void executeInvalidExpectationBlockThenReplayRecordedExpectationFromAnotherThread() throws Exception
   {
      try {
         new Expectations(Runnable.class) {};
         fail();
      }
      catch (IllegalArgumentException ignore) {}

      new Expectations() {{
         mock.doSomething();
      }};

      Thread task = new Thread() {
         @Override public void run() { mock.doSomething(); }
      };
      task.start();
      task.join();
   }
}
