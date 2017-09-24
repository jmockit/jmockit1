/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit;

import java.io.*;
import java.lang.reflect.*;
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

      @SuppressWarnings("DeprecatedIsStillUsed")
      @Deprecated protected static String doInternal() { return "123"; }

      public void provideSomeService() { throw new RuntimeException("Real provideSomeService() called"); }

      public int getValue() { return value; }
      void setValue(int value) { this.value = value; }

      protected long getThreadSpecificValue(int i) { return Thread.currentThread().getId() + i; }
   }

   static class FakeCollaborator1 extends MockUp<Collaborator>
   {
      @Mock void provideSomeService() {}
   }

   @Test
   public void fakeDoingNothing()
   {
      new FakeCollaborator1();

      codeUnderTest.doSomething();
   }

   public interface GenericInterface<T>
   {
      void method(T t);
      String method(int[] ii, T l, String[][] ss, T[] ll);
   }
   public interface NonGenericSubInterface extends GenericInterface<Long> {}

   public static final class FakeForNonGenericSubInterface extends MockUp<NonGenericSubInterface>
   {
      @Mock
      public void method(Long l) { assertTrue(l > 0); }

      @Mock
      public String method(int[] ii, Long l, String[][] ss, Long[] ll)
      {
         assertTrue(ii.length > 0 && l > 0);
         return "faked";
      }
   }

   @Test
   public void fakeMethodOfSubInterfaceWithGenericTypeArgument()
   {
      NonGenericSubInterface faked = new FakeForNonGenericSubInterface().getMockInstance();

      faked.method(123L);
      assertEquals("faked", faked.method(new int[] {1}, 45L, null, null));
   }

   @Test
   public void fakeMethodOfGenericInterfaceWithArrayAndGenericTypeArgument()
   {
      GenericInterface<Long> faked = new MockUp<GenericInterface<Long>>() {
         @Mock
         String method(int[] ii, Long l, String[][] ss, Long[] tt)
         {
            assertTrue(ii.length > 0 && l > 0);
            return "faked";
         }
      }.getMockInstance();

      assertEquals("faked", faked.method(new int[] {1}, 45L, null, null));
   }

   @Test
   public void applyFakesFromInnerFakeClassWithFakeConstructor()
   {
      new FakeCollaborator4();
      assertFalse(mockExecuted);

      new CodeUnderTest().doSomething();

      assertTrue(mockExecuted);
   }

   class FakeCollaborator4 extends MockUp<Collaborator>
   {
      @Mock void $init() { mockExecuted = true; }
      @Mock void provideSomeService() {}
   }

   @Test
   public void applyReentrantFake()
   {
      thrown.expect(RuntimeException.class);

      new FakeCollaboratorWithReentrantFakeMethod();

      codeUnderTest.doSomething();
   }

   static class FakeCollaboratorWithReentrantFakeMethod extends MockUp<Collaborator>
   {
      @Mock int getValue() { return 123; }
      @Mock void provideSomeService(Invocation inv) { inv.proceed(); }
   }

   @Test
   public void applyFakeForConstructor()
   {
      new FakeCollaboratorWithConstructorFake();

      new FacesMessage("test");
   }

   static class FakeCollaboratorWithConstructorFake extends MockUp<FacesMessage>
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
   public void applyFakeForClassHierarchy()
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
            // fake class instance, or the last instance of the faked subclass if a previous invocation of a fake
            // method whose faked method is defined in the subclass occurred on this fake class instance.
            return 123;
         }
      };

      SubCollaborator collaborator = new SubCollaborator(123);
      collaborator.provideSomeService();
      assertEquals(45, collaborator.value);
      assertEquals(123, collaborator.getValue());
   }

   @Test
   public void fakeNativeMethodInClassWithRegisterNatives()
   {
      new FakeSystem();

      assertEquals(0, System.nanoTime());
   }

   static class FakeSystem extends MockUp<System> {
      @Mock public static long nanoTime() { return 0; }
   }

   @Test
   public void fakeNativeMethodInClassWithoutRegisterNatives() throws Exception
   {
      // For some reason, the native method doesn't get mocked when running on Java 9.
      if (!JAVA9) {
         new FakeFloat();

         assertEquals(0.0, Float.intBitsToFloat(2243019), 0.0);
      }
   }

   static class FakeFloat extends MockUp<Float>
   {
      @Mock
      public static float intBitsToFloat(int bits) { return 0; }
   }

   @After
   public void checkThatLocalFakesHaveBeenTornDown()
   {
      assertTrue(System.nanoTime() > 0);
      assertTrue(Float.intBitsToFloat(2243019) > 0);
   }

   @Test
   public void applyFakeForJREClass()
   {
      FakeThread fakeThread = new FakeThread();

      Thread.currentThread().interrupt();

      assertTrue(fakeThread.interrupted);
   }

   public static class FakeThread extends MockUp<Thread>
   {
      boolean interrupted;

      @Mock
      public void interrupt() { interrupted = true; }
   }

   @Test
   public void fakeStaticInitializer()
   {
      new MockUp<AccessibleState>() {
         @Mock void $clinit() {}
      };

      assertNull(AccessibleState.ACTIVE);
   }

   @Test
   public void fakeJREInterface() throws Exception
   {
      CallbackHandler callbackHandler = new FakeCallbackHandler().getMockInstance();

      callbackHandler.handle(new Callback[] {new NameCallback("Enter name:")});
   }

   public static class FakeCallbackHandler extends MockUp<CallbackHandler>
   {
      @Mock
      public void handle(Callback[] callbacks)
      {
         assertEquals(1, callbacks.length);
         assertTrue(callbacks[0] instanceof NameCallback);
      }
   }

   @Test
   public void fakeJREInterfaceWithFakeClass() throws Exception
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
   public void fakePublicInterfaceWithFakeHavingInvocationParameter()
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
   public <A extends AnAbstractClass> void fakeAbstractClassWithFakeForAbstractMethodHavingInvocationParameter()
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
   public void fakeNonPublicInterfaceWithFakeHavingInvocationParameter()
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
   public void fakeGenericInterfaceWithFakeHavingInvocationParameter() throws Exception
   {
      Callable<String> faked = new MockUp<Callable<String>>() {
         @Mock String call(Invocation inv) { return "faked"; }
      }.getMockInstance();

      assertEquals("faked", faked.call());
   }

   static class GenericClass<T> { protected T doSomething() { return null; } }

   @Test
   public void fakeGenericClassWithFakeHavingInvocationParameter()
   {
      new MockUp<GenericClass<String>>() {
         @Mock String doSomething(Invocation inv) { return "faked"; }
      };

      GenericClass<String> faked = new GenericClass<String>();
      assertEquals("faked", faked.doSomething());
   }

   @Test
   public void fakeFileConstructor()
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
   public void concurrentFake() throws Exception
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
   public void fakeAffectsInstancesOfSpecifiedSubclassAndNotOfBaseClass()
   {
      new FakeForSubclass();

      // Faking applies to instance methods executed on instances of the subclass:
      assertEquals(123, new SubCollaborator(5).getValue());

      // And to static methods from any class in the hierarchy:
      //noinspection deprecation
      assertEquals("mocked", Collaborator.doInternal());

      // But not to instance methods executed on instances of the base class:
      assertEquals(62, new Collaborator(62).getValue());
   }

   static class FakeForSubclass extends MockUp<SubCollaborator>
   {
      @Mock void $init(int i) {}
      @Mock String doInternal() { return "mocked"; }
      @Mock int getValue() { return 123; }
   }
}
