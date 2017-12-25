/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit;

import java.awt.*;
import java.util.concurrent.*;

import javax.swing.*;

import org.junit.*;
import org.junit.runners.*;
import static org.junit.Assert.*;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public final class MultiThreadedExpectationsTest
{
   static class Collaborator
   {
      int doSomething() { return -1; }
      void doSomethingElse() {}
   }

   @Mocked Collaborator mock;

   void useMockedCollaboratorFromWorkerThread()
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
      new Expectations() {{
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

   static class Dependency
   {
      void doSomething() {}
      static void doSomethingElse() {}
   }

   @Test
   public void verifyInvocationsReplayedInAnotherThreadWhoseClassIsNoLongerMocked_part1(
      @Mocked final Dependency dep, @Mocked final Graphics2D g2D, @Mocked final Runnable runnable)
   {
      new Thread() {
         @Override
         public void run()
         {
            dep.doSomething();
            g2D.dispose();
            runnable.run();
            Dependency.doSomethingElse();
         }
      }.start();
   }

   @Test
   public void verifyInvocationsReplayedInAnotherThreadWhoseClassIsNoLongerMocked_part2() throws Exception
   {
      Thread.sleep(10);
      new FullVerifications() {};
   }

   public interface APublicInterface { boolean doSomething(); }

   @Test
   public void invokeMethodOnMockedPublicInterfaceFromEDT(@Mocked final APublicInterface mock) throws Exception {
      new Expectations() {{ mock.doSomething(); result = true; }};

      SwingUtilities.invokeAndWait(new Runnable() {
         @Override
         public void run() { assertFalse(mock.doSomething()); }
      });

      assertTrue(mock.doSomething());
   }

   public abstract static class AnAbstractClass { public abstract boolean doSomething(); }

   @Test
   public void invokeMethodOnMockedAbstractClassFromEDT(@Mocked final AnAbstractClass mock) throws Exception {
      new Expectations() {{ mock.doSomething(); result = true; }};

      SwingUtilities.invokeAndWait(new Runnable() {
         @Override
         public void run() { assertFalse(mock.doSomething()); }
      });

      assertTrue(mock.doSomething());
   }

   @Test
   public void invokeMethodOnMockedGenericInterfaceFromEDT(@Mocked final Callable<Boolean> mock) throws Exception {
      new Expectations() {{ mock.call(); result = true; }};

      SwingUtilities.invokeAndWait(new Runnable() {
         @Override
         public void run() {
            try { assertFalse(mock.call()); } catch (Exception e) { throw new RuntimeException(e); }
         }
      });

      assertTrue(mock.call());
   }
}
