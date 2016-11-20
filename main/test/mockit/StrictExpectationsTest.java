/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit;

import java.net.*;
import java.util.*;

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

      Collaborator() {}
      Collaborator(int value) { this.value = value; }

      static String doInternal() { return "123"; }

      void provideSomeService() {}
      protected native boolean nativeMethod();

      String doSomething(URL url) { return url.toString(); }

      int getValue() { return value; }
      void setValue(int value) { this.value = value; }
   }

   @Test
   public void expectOnlyOneInvocationButExerciseOthersDuringReplay(@Mocked final Collaborator mock)
   {
      thrown.expect(UnexpectedInvocation.class);

      new StrictExpectations() {{ mock.provideSomeService(); }};

      mock.provideSomeService();
      mock.setValue(1);
   }

   @Test
   public void recordNothingOnLocalMockedTypeAndExerciseItDuringReplay(@Mocked Collaborator mock)
   {
      mock.provideSomeService();
   }

   @Test
   public void recordEmptyBlockOnLocalMockedTypeAndExerciseItDuringReplay(@Mocked Collaborator mock)
   {
      new StrictExpectations() {};

      mock.provideSomeService();
   }

   @Test
   public void expectNothingOnMockedTypeButExerciseItDuringReplay(@Mocked final Collaborator mock)
   {
      thrown.expect(UnexpectedInvocation.class);

      new StrictExpectations() {{ mock.setValue(anyInt); times = 0; }};

      mock.setValue(2);
   }

   @Test
   public void mockInterface(@Mocked final Runnable mock)
   {
      new StrictExpectations() {{ mock.run(); }};

      mock.run();
   }

   public interface IA {}
   public interface IB extends IA {}
   public interface IC { boolean doSomething(IB b); }

   @Test
   public void mockInterfaceWhichExtendsAnother(@Mocked final IB b, @Mocked final IC c)
   {
      new StrictExpectations() {{
         c.doSomething(b); result = false;
         c.doSomething(b); result = true;
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
      new StrictExpectations() {{
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
      new StrictExpectations() {{
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
      new StrictExpectations() {{
         mock.getValue(); times = 2; returns(1, 2);
      }};

      SubCollaborator collaborator = new SubCollaborator();
      assertEquals(2, collaborator.getValue());
      assertEquals(3, collaborator.getValue(1));
   }

   @Test
   public void mockClassWithMethodsOfAllReturnTypesReturningDefaultValues(
      @Mocked final ClassWithMethodsOfEveryReturnType mock)
   {
      new StrictExpectations() {{
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
      ClassWithMethodsOfEveryReturnType returnSameType() { return null; }
   }

   @Test
   public void replayWithUnexpectedStaticMethodInvocation(@Mocked final Collaborator mock)
   {
      thrown.expect(UnexpectedInvocation.class);

      new StrictExpectations() {{ mock.getValue(); }};

      Collaborator.doInternal();
   }

   @Test
   public void replayWithMissingExpectedMethodInvocation(@Mocked final Collaborator mock)
   {
      thrown.expect(MissingInvocation.class);

      new StrictExpectations() {{ mock.setValue(123); }};
   }

   @Test
   public void defineTwoConsecutiveReturnValues(@Mocked final Collaborator mock)
   {
      new StrictExpectations() {{
         mock.getValue(); result = 1; result = 2;
      }};

      assertEquals(1, mock.getValue());
      assertEquals(2, mock.getValue());
   }

   @Test
   public void mockNativeMethodUsingFullMocking(@Mocked final Collaborator mock)
   {
      new StrictExpectations() {{
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
      new StrictExpectations() {{
         System.getenv("envVar"); result = ".";
      }};

      assertEquals(".", System.getenv("envVar"));
   }

   @Test
   public void failureFromUnexpectedInvocationInAnotherThread(@Mocked final Collaborator mock) throws Exception
   {
      thrown.expect(UnexpectedInvocation.class);

      Thread t = new Thread() {
         @Override
         public void run() { mock.provideSomeService(); }
      };

      new StrictExpectations() {{ mock.getValue(); }};

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
   public void recordingExpectationOnMethodWithOneArgumentButReplayingWithAnotherShouldProduceUsefulErrorMessage(
      @Mocked final Collaborator mock) throws Exception
   {
      final URL expectedURL = new URL("http://expected");

      new StrictExpectations() {{ mock.doSomething(expectedURL); }};

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
      new StrictExpectations() {
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

   @Test
   public void recordExpectationsOnMethodReturningOwnType(@Mocked final ClassWithMethodsOfEveryReturnType mock)
   {
      ClassWithMethodsOfEveryReturnType nonMock = new ClassWithMethodsOfEveryReturnType();

      new StrictExpectations() {{
         mock.returnSameType();
         mock.returnSameType();
      }};

      assertSame(mock, mock.returnSameType());
      assertSame(mock, nonMock.returnSameType());
   }
}
