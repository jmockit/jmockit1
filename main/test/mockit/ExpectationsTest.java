/*
 * Copyright (c) 2006-2014 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit;

import java.io.*;
import java.net.*;
import java.util.*;

import org.junit.*;
import static org.junit.Assert.*;

import mockit.internal.*;

import static mockit.Deencapsulation.*;

public final class ExpectationsTest
{
   static class Collaborator
   {
      private int value;

      Collaborator() {}
      Collaborator(int value) { this.value = value; }

      private static String doInternal() { return "123"; }

      void provideSomeService() {}
      protected native boolean nativeMethod();

      String doSomething(URL url) { return url.toString(); }

      int getValue() { return value; }
      void setValue(int value) { this.value = value; }
   }

   @Test(expected = UnexpectedInvocation.class)
   public void expectOnlyOneInvocationButExerciseOthersDuringReplay(@Mocked final Collaborator mock)
   {
      new Expectations() {{ mock.provideSomeService(); }};

      mock.provideSomeService();
      mock.setValue(1);
   }

   @Test
   public void recordNothingOnLocalMockedTypeAndExerciseItDuringReplay(@Mocked Collaborator mock)
   {
      mock.provideSomeService();
   }

   @Test
   public void recordNothingOnTestScopedMockedTypeAndExerciseItDuringReplay(@Mocked Collaborator mock)
   {
      new Expectations() {};

      mock.provideSomeService();
   }

   @Test(expected = UnexpectedInvocation.class)
   public void expectNothingOnMockedTypeButExerciseItDuringReplay(@Mocked final Collaborator mock)
   {
      new Expectations() {{
         mock.setValue(anyInt); times = 0;
      }};

      mock.setValue(2);
   }

   @Test
   public void mockInterface(@Mocked final Runnable mock)
   {
      new Expectations() {{ mock.run(); }};

      mock.run();
   }

   public interface IA {}
   public interface IB extends IA {}
   public interface IC { boolean doSomething(IB b); }

   @Test
   public void mockInterfaceWhichExtendsAnother(@Mocked final IB b, @Mocked final IC c)
   {
      new Expectations() {{
         c.doSomething(b); result = false;
         invoke(c, "doSomething", b); result = true;
      }};

      assertFalse(c.doSomething(b));
      assertTrue(c.doSomething(b));
   }

   public abstract static class AbstractCollaborator
   {
      String doSomethingConcrete() { return "test"; }
      protected abstract void doSomethingAbstract();
   }

   @Test
   public void mockAbstractClass(@Mocked final AbstractCollaborator mock)
   {
      new Expectations() {{
         mock.doSomethingConcrete();
         mock.doSomethingAbstract();
      }};

      mock.doSomethingConcrete();
      mock.doSomethingAbstract();
   }

   @Test
   public void mockClassWithoutDefaultConstructor(@Mocked Dummy mock)
   {
      assertNotNull(mock);
   }

   static class Dummy
   {
      @SuppressWarnings("unused")
      Dummy(int i) {}
   }

   static final class SubCollaborator extends Collaborator
   {
      @Override int getValue() { return 1 + super.getValue(); }
      int getValue(int i) { return i + super.getValue(); }
   }

   @Test
   public void mockSubclass(@Mocked final SubCollaborator mock)
   {
      new Expectations() {{
         new SubCollaborator();
         mock.provideSomeService();
         mock.getValue(); result = 1;
      }};

      SubCollaborator collaborator = new SubCollaborator();
      collaborator.provideSomeService();
      assertEquals(1, collaborator.getValue());
   }

   @Test
   public void mockSuperClass(@Mocked final Collaborator mock)
   {
      new Expectations() {{
         mock.getValue(); times = 2; returns(1, 2);
      }};

      SubCollaborator collaborator = new SubCollaborator();
      assertEquals(2, collaborator.getValue());
      assertEquals(3, collaborator.getValue(1));
   }

   @Test(expected = IllegalStateException.class)
   public void attemptToRecordExpectedReturnValueForNoCurrentInvocation(@Mocked Collaborator mock)
   {
      new Expectations() {{
         result = 42;
      }};
   }

   @Test(expected = IllegalStateException.class)
   public void attemptToAddArgumentMatcherWhenNotRecording(@Mocked Collaborator mock)
   {
      new Expectations() {}.withNotEqual(5);
   }

   @Test
   public void mockClassWithMethodsOfAllReturnTypesReturningDefaultValues(
      @Mocked final ClassWithMethodsOfEveryReturnType mock)
   {
      new Expectations() {{
         mock.getBoolean();
         mock.getChar();
         mock.getByte();
         mock.getShort();
         mock.getInt();
         mock.getLong();
         mock.getFloat();
         mock.getDouble();
         mock.getObject();
         mock.getElements();
      }};

      assertFalse(mock.getBoolean());
      assertEquals('\0', mock.getChar());
      assertEquals(0, mock.getByte());
      assertEquals(0, mock.getShort());
      assertEquals(0, mock.getInt());
      assertEquals(0L, mock.getLong());
      assertEquals(0.0, mock.getFloat(), 0.0);
      assertEquals(0.0, mock.getDouble(), 0.0);
      assertNull(mock.getObject());
      assertFalse(mock.getElements().hasMoreElements());
   }

   static class ClassWithMethodsOfEveryReturnType
   {
      boolean getBoolean() { return true; }
      char getChar() { return 'A' ; }
      byte getByte() { return 1; }
      short getShort() { return 1; }
      int getInt() { return 1; }
      long getLong() { return 1; }
      float getFloat() { return 1.0F; }
      double getDouble() { return 1.0; }
      Object getObject() { return new Object(); }
      Enumeration<?> getElements() { return null; }
   }

   @Test(expected = UnexpectedInvocation.class)
   public void replayWithUnexpectedStaticMethodInvocation(@Mocked final Collaborator mock)
   {
      new Expectations() {{
         mock.getValue();
      }};

      Collaborator.doInternal();
   }

   @Test(expected = MissingInvocation.class)
   public void replayWithMissingExpectedMethodInvocation(@Mocked final Collaborator mock)
   {
      new Expectations() {{
         mock.setValue(123);
      }};
   }

   @Test
   public void defineTwoConsecutiveReturnValues(@Mocked final Collaborator mock)
   {
      new Expectations() {{
         mock.getValue(); result = 1; result = 2;
      }};

      assertEquals(1, mock.getValue());
      assertEquals(2, mock.getValue());
   }

   @Test
   public void mockNativeMethodUsingFullMocking(@Mocked final Collaborator mock)
   {
      new Expectations() {{
         mock.nativeMethod(); result = true;
      }};

      assertTrue(mock.nativeMethod());
   }

   @Test
   public void mockNativeMethodUsingPartialMocking()
   {
      Collaborator col = new Collaborator();

      new MockUp<Collaborator>() {
         @Mock boolean nativeMethod() { return true; }
      };

      assertTrue(col.nativeMethod());
   }

   @Test
   public void mockSystemGetenvMethod(@Mocked System mockedSystem)
   {
      new Expectations() {{
         System.getenv("envVar"); result = ".";
      }};

      assertEquals(".", System.getenv("envVar"));
   }

   @Test
   public void mockConstructorsInJREClassHierarchies(@Mocked FileWriter fileWriter, @Mocked PrintWriter printWriter)
      throws Exception
   {
      new Expectations() {{
         new FileWriter("no.file");
      }};

      new FileWriter("no.file");
   }

   @Test(expected = UnexpectedInvocation.class)
   public void failureFromUnexpectedInvocationInAnotherThread(@Mocked final Collaborator mock) throws Exception
   {
      Thread t = new Thread() {
         @Override
         public void run() { mock.provideSomeService(); }
      };

      new Expectations() {{ mock.getValue(); }};

      mock.getValue();
      t.start();
      t.join();
   }

   public interface InterfaceWithStaticInitializer { Object X = "x"; }

   @Test
   public void mockInterfaceWithStaticInitializer(@Mocked InterfaceWithStaticInitializer mock)
   {
      assertNotNull(mock);
      assertEquals("x", InterfaceWithStaticInitializer.X);
   }

   @Test
   public void recordStrictExpectationsAllowingZeroInvocationsAndReplayNone(@Mocked final Collaborator mock)
   {
      new Expectations() {{
         mock.provideSomeService(); minTimes = 0;
         mock.setValue(1); minTimes = 0;
      }};

      // Don't exercise anything.
   }

   @Test
   public void recordingExpectationOnMethodWithOneArgumentButReplayingWithAnotherShouldProduceUsefulErrorMessage(
      @Mocked final Collaborator mock) throws Exception
   {
      final URL expectedURL = new URL("http://expected");

      new Expectations() {{ mock.doSomething(expectedURL); }};

      mock.doSomething(expectedURL);

      URL anotherURL = new URL("http://another");

      try {
         mock.doSomething(anotherURL);
         fail();
      }
      catch (UnexpectedInvocation e) {
         assertTrue(e.getMessage().contains(anotherURL.toString()));
      }
   }

   @Test
   public void recordExpectationInMethodOfExpectationBlockInsteadOfConstructor(@Mocked final Collaborator mock)
   {
      new Expectations() {
         {
            recordExpectation();
         }

         private void recordExpectation()
         {
            mock.getValue();
            result = 123;
         }
      };

      assertEquals(123, mock.getValue());
   }
}
