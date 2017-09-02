/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit;

import org.junit.*;
import org.junit.rules.*;
import static org.junit.Assert.*;

import mockit.internal.*;

public final class StrictExpectationsTest
{
   @Rule public final ExpectedException thrown = ExpectedException.none();

   static class Collaborator
   {
      private int value;

      int getValue() { return value; }
      void setValue(int value) { this.value = value; }

      void provideSomeService() {}
      String doSomething(String s) { return s.toLowerCase(); }
      static String doInternal() { return "123"; }
   }

   @Test
   public void expectOnlyOneInvocationButExerciseOthersDuringReplay(@Mocked final Collaborator mock)
   {
      thrown.expect(UnexpectedInvocation.class);
      new Expectations() {{ mock.provideSomeService(); }};

      mock.provideSomeService();
      mock.setValue(1);

      new FullVerifications() {};
   }

   @Test
   public void expectNothingOnMockedTypeButExerciseItDuringReplay(@Mocked final Collaborator mock)
   {
      thrown.expect(UnexpectedInvocation.class);
      new Expectations() {{ mock.setValue(anyInt); times = 0; }};

      mock.setValue(2);

      new FullVerifications() {};
   }

   @Test
   public void replayWithUnexpectedStaticMethodInvocation(@Mocked final Collaborator mock)
   {
      thrown.expect(UnexpectedInvocation.class);
      new Expectations() {{ mock.getValue(); }};

      Collaborator.doInternal();

      new FullVerifications() {};
   }

   @Test
   public void failureFromUnexpectedInvocationInAnotherThread(@Mocked final Collaborator mock) throws Exception
   {
      Thread t = new Thread() {
         @Override
         public void run() { mock.provideSomeService(); }
      };

      new Expectations() {{ mock.getValue(); }};
      thrown.expect(UnexpectedInvocation.class);

      mock.getValue();
      t.start();
      t.join();

      new FullVerifications() {};
   }

   @Test
   public void recordingExpectationOnMethodWithOneArgumentButReplayingWithAnotherShouldProduceUsefulErrorMessage(
      @Mocked final Collaborator mock) throws Exception
   {
      final String expected = "expected";
      new Expectations() {{ mock.doSomething(expected); }};

      mock.doSomething(expected);

      String another = "another";
      mock.doSomething(another);

      thrown.expect(UnexpectedInvocation.class);
      thrown.expectMessage(another);
      new FullVerifications() {};
   }

   public interface BaseItf { int base(); }
   public interface SubItf extends BaseItf {}
   static final class Impl implements SubItf { @Override public int base() { return 1; } }
   final BaseItf impl = new Impl();

   @Test
   public void recordStrictCallToBaseInterfaceMethodOnCapturedImplementationOfSubInterface(@Capturing final SubItf sub)
   {
      new StrictExpectations() {{ sub.base(); result = 123; }};

      int actual = impl.base();

      assertEquals(123, actual);
   }
}
