/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.*;
import javax.accessibility.*;
import javax.faces.application.*;
import javax.security.auth.callback.*;

import static mockit.Deencapsulation.*;
import static mockit.internal.util.Utilities.*;

import static org.junit.Assert.*;
import org.junit.*;
import org.junit.rules.*;

@SuppressWarnings("JUnitTestMethodWithNoAssertions")
public final class MockAnnotationsTest
{
   @Rule public final ExpectedException thrown = ExpectedException.none();

   final CodeUnderTest codeUnderTest = new CodeUnderTest();
   boolean mockExecuted;

   static class CodeUnderTest
   {
      private final Collaborator dependency = new Collaborator();

      void doSomething() { dependency.provideSomeService(); }
      long doSomethingElse(int i) { return dependency.getThreadSpecificValue(i); }
   }

   public static class Collaborator
   {
      static Object xyz;
      protected int value;

      public Collaborator() {}
      Collaborator(int value) { this.value = value; }

      @Deprecated protected static String doInternal() { return "123"; }

      public void provideSomeService() { throw new RuntimeException("Real provideSomeService() called"); }

      public int getValue() { return value; }
      void setValue(int value) { this.value = value; }

      @SuppressWarnings("unused")
      List<?> complexOperation(Object input1, Object... otherInputs)
      {
         return input1 == null ? Collections.emptyList() : Arrays.asList(otherInputs);
      }

      @SuppressWarnings("unused")
      final void simpleOperation(int a, String b, Date c) {}

      protected long getThreadSpecificValue(int i) { return Thread.currentThread().getId() + i; }
   }

   // Mocks without expectations //////////////////////////////////////////////////////////////////////////////////////

   static class MockCollaborator1 extends MockUp<Collaborator>
   {
      @Mock void provideSomeService() {}
   }

   @Test
   public void mockWithNoExpectationsPassingMockInstance()
   {
      new MockCollaborator1();

      codeUnderTest.doSomething();
   }

   public interface GenericInterface<T>
   {
      void method(T t);
      String method(int[] ii, T l, String[][] ss, T[] ll);
   }
   public interface NonGenericSubInterface extends GenericInterface<Long> {}

   public static final class MockForNonGenericSubInterface extends MockUp<NonGenericSubInterface>
   {
      @Mock
      public void method(Long l) { assertTrue(l > 0); }

      @Mock
      public String method(int[] ii, Long l, String[][] ss, Long[] ll)
      {
         assertTrue(ii.length > 0 && l > 0);
         return "mocked";
      }
   }

   @Test
   public void mockMethodOfSubInterfaceWithGenericTypeArgument()
   {
      NonGenericSubInterface mock = new MockForNonGenericSubInterface().getMockInstance();

      mock.method(123L);
      assertEquals("mocked", mock.method(new int[] {1}, 45L, null, null));
   }

   @Test
   public void mockMethodOfGenericInterfaceWithArrayAndGenericTypeArgument()
   {
      GenericInterface<Long> mock = new MockUp<GenericInterface<Long>>() {
         @Mock
         String method(int[] ii, Long l, String[][] ss, Long[] tt)
         {
            assertTrue(ii.length > 0 && l > 0);
            return "mocked";
         }
      }.getMockInstance();

      assertEquals("mocked", mock.method(new int[] {1}, 45L, null, null));
   }

   @Test
   public void applyMockupsFromInnerMockClassWithMockConstructor()
   {
      new MockCollaborator4();
      assertFalse(mockExecuted);

      new CodeUnderTest().doSomething();

      assertTrue(mockExecuted);
   }

   class MockCollaborator4 extends MockUp<Collaborator>
   {
      @Mock void $init() { mockExecuted = true; }
      @Mock void provideSomeService() {}
   }

   // Reentrant mocks /////////////////////////////////////////////////////////////////////////////////////////////////

   @Test
   public void applyReentrantMockup()
   {
      thrown.expect(RuntimeException.class);

      new MockCollaboratorWithReentrantMock();

      codeUnderTest.doSomething();
   }

   static class MockCollaboratorWithReentrantMock extends MockUp<Collaborator>
   {
      @Mock int getValue() { return 123; }
      @Mock void provideSomeService(Invocation inv) { inv.proceed(); }
   }

   // Mocks for constructors and static methods ///////////////////////////////////////////////////////////////////////

   @Test
   public void applyMockupForConstructor()
   {
      new MockCollaboratorWithConstructorMock();

      new FacesMessage("test");
   }

   static class MockCollaboratorWithConstructorMock extends MockUp<FacesMessage>
   {
      @Mock
      void $init(String value)
      {
         assertEquals("test", value);
      }
   }

   public static class SubCollaborator extends Collaborator
   {
      public SubCollaborator(int i) { throw new RuntimeException(String.valueOf(i)); }

      @Override
      public void provideSomeService() { value = 123; }
   }

   @Test
   public void applyMockupForClassHierarchy()
   {
      new MockUp<SubCollaborator>() {
         @Mock
         void $init(Invocation inv, int i)
         {
            assertNotNull(inv.getInvokedInstance());
            assertTrue(i > 0);
         }

         @Mock
         void provideSomeService(Invocation inv)
         {
            SubCollaborator it = inv.getInvokedInstance();
            it.value = 45;
         }

         @Mock
         int getValue(Invocation inv)
         {
            assertNotNull(inv.getInvokedInstance());
            // The value of "it" is undefined here; it will be null if this is the first mock invocation reaching this
            // mock class instance, or the last instance of the mocked subclass if a previous invocation of a mock
            // method whose mocked method is defined in the subclass occurred on this mock class instance.
            return 123;
         }
      };

      SubCollaborator collaborator = new SubCollaborator(123);
      collaborator.provideSomeService();
      assertEquals(45, collaborator.value);
      assertEquals(123, collaborator.getValue());
   }

   @Test
   public void mockNativeMethodInClassWithRegisterNatives()
   {
      new MockSystem();

      assertEquals(0, System.nanoTime());
   }

   static class MockSystem extends MockUp<System> {
      @Mock public static long nanoTime() { return 0; }
   }

   @Test
   public void mockNativeMethodInClassWithoutRegisterNatives() throws Exception
   {
      // For some reason, the native method doesn't get mocked when running on Java 9.
      if (!JAVA9) {
         new MockFloat();

         assertEquals(0.0, Float.intBitsToFloat(2243019), 0.0);
      }
   }

   static class MockFloat extends MockUp<Float>
   {
      @SuppressWarnings("UnusedDeclaration")
      @Mock
      public static float intBitsToFloat(int bits) { return 0; }
   }

   @After
   public void checkThatLocalMockUpsHaveBeenTornDown()
   {
      assertTrue(System.nanoTime() > 0);
      assertTrue(Float.intBitsToFloat(2243019) > 0);
   }

   @Test
   public void applyMockupForJREClass()
   {
      MockThread mockThread = new MockThread();

      Thread.currentThread().interrupt();

      assertTrue(mockThread.interrupted);
   }

   public static class MockThread extends MockUp<Thread>
   {
      boolean interrupted;

      @Mock
      public void interrupt() { interrupted = true; }
   }

   // Stubbing of static class initializers ///////////////////////////////////////////////////////////////////////////

   @Test
   public void fakeStaticInitializer()
   {
      new MockUp<AccessibleState>() {
         @Mock void $clinit() {}
      };

      assertNull(AccessibleState.ACTIVE);
   }

   // Other tests /////////////////////////////////////////////////////////////////////////////////////////////////////

   @Test
   public void mockJREInterface() throws Exception
   {
      CallbackHandler callbackHandler = new MockCallbackHandler().getMockInstance();

      callbackHandler.handle(new Callback[] {new NameCallback("Enter name:")});
   }

   public static class MockCallbackHandler extends MockUp<CallbackHandler>
   {
      @Mock
      public void handle(Callback[] callbacks)
      {
         assertEquals(1, callbacks.length);
         assertTrue(callbacks[0] instanceof NameCallback);
      }
   }

   @Test
   public void mockJREInterfaceWithMockUp() throws Exception
   {
      CallbackHandler callbackHandler = new MockUp<CallbackHandler>() {
         @Mock
         void handle(Callback[] callbacks)
         {
            assertEquals(1, callbacks.length);
            assertTrue(callbacks[0] instanceof NameCallback);
         }
      }.getMockInstance();

      callbackHandler.handle(new Callback[] {new NameCallback("Enter name:")});
   }

   public interface AnInterface { int doSomething(); }

   @Test
   public void mockPublicInterfaceWithMockUpHavingInvocationParameter()
   {
      AnInterface obj = new MockUp<AnInterface>() {
         @Mock
         int doSomething(Invocation inv)
         {
            assertNotNull(inv.getInvokedInstance());
            return 122 + inv.getInvocationCount();
         }
      }.getMockInstance();

      assertEquals(123, obj.doSomething());
   }

   abstract static class AnAbstractClass { protected abstract int doSomething(); }

   @Test
   public <A extends AnAbstractClass> void mockAbstractClassWithMockForAbstractMethodHavingInvocationParameter()
   {
      final AnAbstractClass obj = new AnAbstractClass() { @Override protected int doSomething() { return 0; } };

      new MockUp<A>() {
         @Mock
         int doSomething(Invocation inv)
         {
            assertSame(obj, inv.getInvokedInstance());
            Method invokedMethod = inv.getInvokedMember();
            assertTrue(AnAbstractClass.class.isAssignableFrom(invokedMethod.getDeclaringClass()));
            return 123;
         }
      };

      assertEquals(123, obj.doSomething());
   }

   interface AnotherInterface { int doSomething(); }
   AnotherInterface interfaceInstance;

   @Test
   public void attemptToProceedIntoInterfaceImplementation()
   {
      thrown.expect(UnsupportedOperationException.class);
      thrown.expectMessage("abstract/interface method");

      interfaceInstance = new MockUp<AnotherInterface>() {
         @Mock
         int doSomething(Invocation inv) { return inv.proceed(); }
      }.getMockInstance();

      interfaceInstance.doSomething();
   }

   @Test
   public void mockNonPublicInterfaceWithMockUpHavingInvocationParameter()
   {
      interfaceInstance = new MockUp<AnotherInterface>() {
         @Mock
         int doSomething(Invocation inv)
         {
            AnotherInterface instanceThatWasInvoked = inv.getInvokedInstance();
            assertSame(interfaceInstance, instanceThatWasInvoked);

            int invocationCount = inv.getInvocationCount();
            assertTrue(invocationCount > 0);

            return invocationCount == 1 ? instanceThatWasInvoked.doSomething() : 123;
         }
      }.getMockInstance();

      assertEquals(123, interfaceInstance.doSomething());
   }

   @Test
   public void mockGenericInterfaceWithMockUpHavingInvocationParameter() throws Exception
   {
      Callable<String> mock = new MockUp<Callable<String>>() {
         @Mock String call(Invocation inv) { return "mocked"; }
      }.getMockInstance();

      assertEquals("mocked", mock.call());
   }

   static class GenericClass<T> { protected T doSomething() { return null; } }

   @Test
   public void mockGenericClassWithMockUpHavingInvocationParameter()
   {
      new MockUp<GenericClass<String>>() {
         @Mock String doSomething(Invocation inv) { return "mocked"; }
      };

      GenericClass<String> mock = new GenericClass<String>();
      assertEquals("mocked", mock.doSomething());
   }

   @Test
   public void mockFileConstructor()
   {
      new MockUp<File>() {
         @Mock
         void $init(Invocation inv, String pathName)
         {
            File it = inv.getInvokedInstance();
            setField(it, "path", "fixedPrefix/" + pathName);
         }
      };

      File f = new File("test");
      assertEquals("fixedPrefix/test", f.getPath());
   }

   @Test @SuppressWarnings("MethodWithMultipleLoops")
   public void concurrentMock() throws Exception
   {
      new MockUp<Collaborator>() {
         @Mock long getThreadSpecificValue(int i) { return Thread.currentThread().getId() + 123; }
      };

      Thread[] threads = new Thread[5];

      for (int i = 0; i < threads.length; i++) {
         threads[i] = new Thread() {
            @Override
            public void run()
            {
               long threadSpecificValue = Thread.currentThread().getId() + 123;
               long actualValue = new CodeUnderTest().doSomethingElse(0);
               assertEquals(threadSpecificValue, actualValue);
            }
         };
      }

      for (Thread thread : threads) { thread.start(); }
      for (Thread thread : threads) { thread.join(); }
   }

   @Test
   public void mockUpAffectsInstancesOfSpecifiedSubclassAndNotOfBaseClass()
   {
      new MockUpForSubclass();

      // Mocking applies to instance methods executed on instances of the subclass:
      assertEquals(123, new SubCollaborator(5).getValue());

      // And to static methods from any class in the hierarchy:
      //noinspection deprecation
      assertEquals("mocked", Collaborator.doInternal());

      // But not to instance methods executed on instances of the base class:
      assertEquals(62, new Collaborator(62).getValue());
   }

   static class MockUpForSubclass extends MockUp<SubCollaborator>
   {
      @Mock void $init(int i) {}
      @Mock String doInternal() { return "mocked"; }
      @Mock int getValue() { return 123; }
   }
}
