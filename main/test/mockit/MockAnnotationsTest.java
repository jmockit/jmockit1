/*
 * Copyright (c) 2006-2014 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.*;
import javax.security.auth.callback.*;

import static mockit.Deencapsulation.*;
import static org.junit.Assert.*;
import org.junit.*;
import org.junit.rules.*;

import mockit.internal.*;

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

      int performComputation(int a, boolean b)
      {
         int i = dependency.getValue();
         List<?> results = dependency.complexOperation(a, i);

         if (b) {
            dependency.setValue(i + results.size());
         }

         return i;
      }
   }

   @SuppressWarnings("UnusedDeclaration")
   static class Collaborator
   {
      static Object xyz;
      protected int value;

      Collaborator() {}
      Collaborator(int value) { this.value = value; }

      @Deprecated private static String doInternal() { return "123"; }

      void provideSomeService() { throw new RuntimeException("Real provideSomeService() called"); }

      int getValue() { return value; }
      void setValue(int value) { this.value = value; }

      List<?> complexOperation(Object input1, Object... otherInputs)
      {
         return input1 == null ? Collections.emptyList() : Arrays.asList(otherInputs);
      }

      final void simpleOperation(int a, String b, Date c) {}

      long getThreadSpecificValue(int i) { return Thread.currentThread().getId() + i; }
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

   @Test
   public void attemptToSetUpMockForClassLackingAMatchingRealMethod()
   {
      thrown.expect(IllegalArgumentException.class);

      new MockForClassWithoutRealMethod();
   }

   static final class MockForClassWithoutRealMethod extends MockUp<Collaborator>
   {
      @Mock void noMatchingRealMethod() {}
   }

   public interface GenericInterface<T>
   {
      void method(T t);
      String method(int[] ii, T l, String[][] ss, T[] ll);
   }
   public interface NonGenericSubInterface extends GenericInterface<Long> {}

   public static final class MockForNonGenericSubInterface extends MockUp<NonGenericSubInterface>
   {
      @Mock(invocations = 1)
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
   public void setUpMocksFromInnerMockClassWithMockConstructor()
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

   // Mocks WITH expectations /////////////////////////////////////////////////////////////////////////////////////////

   @Test
   public void setUpMocksContainingExpectations()
   {
      new MockCollaboratorWithExpectations();

      int result = codeUnderTest.performComputation(2, true);

      assertEquals(0, result);
   }

   static class MockCollaboratorWithExpectations extends MockUp<Collaborator>
   {
      @Mock(minInvocations = 1)
      int getValue() { return 0; }

      @Mock(maxInvocations = 2)
      void setValue(int value)
      {
         assertEquals(1, value);
      }

      @Mock
      List<?> complexOperation(Object input1, Object... otherInputs)
      {
         int i = (Integer) otherInputs[0];
         assertEquals(0, i);

         List<Integer> values = new ArrayList<Integer>();
         values.add((Integer) input1);
         return values;
      }

      @Mock(invocations = 0)
      void provideSomeService() {}
   }

   @Test
   public void setUpMockWithMinInvocationsExpectationButFailIt()
   {
      thrown.expect(MissingInvocation.class);

      new MockCollaboratorWithMinInvocationsExpectation();
   }

   static class MockCollaboratorWithMinInvocationsExpectation extends MockUp<Collaborator>
   {
      @Mock(minInvocations = 2)
      int getValue() { return 1; }
   }

   @Test
   public void setUpMockWithMaxInvocationsExpectationButFailIt()
   {
      thrown.expect(UnexpectedInvocation.class);

      new MockCollaboratorWithMaxInvocationsExpectation();

      new Collaborator().setValue(23);
   }

   static class MockCollaboratorWithMaxInvocationsExpectation extends MockUp<Collaborator>
   {
      @Mock(maxInvocations = 0)
      void setValue(int v) { assertEquals(23, v); }
   }

   @Test
   public void setUpMockWithInvocationsExpectationButFailIt()
   {
      thrown.expect(UnexpectedInvocation.class);

      new MockCollaboratorWithInvocationsExpectation();

      codeUnderTest.doSomething();
      codeUnderTest.doSomething();
   }

   static class MockCollaboratorWithInvocationsExpectation extends MockUp<Collaborator>
   {
      @Mock(invocations = 1)
      void provideSomeService() {}
   }

   // Reentrant mocks /////////////////////////////////////////////////////////////////////////////////////////////////

   @Test
   public void setUpReentrantMock()
   {
      thrown.expect(RuntimeException.class);

      new MockCollaboratorWithReentrantMock();

      codeUnderTest.doSomething();
   }

   static class MockCollaboratorWithReentrantMock extends MockUp<Collaborator>
   {
      @Mock
      int getValue() { return 123; }

      @Mock(invocations = 1)
      void provideSomeService(Invocation inv) { inv.proceed(); }
   }

   // Mocks for constructors and static methods ///////////////////////////////////////////////////////////////////////

   @Test
   public void setUpMockForConstructor()
   {
      new MockCollaboratorWithConstructorMock();

      new Collaborator(5);
   }

   static class MockCollaboratorWithConstructorMock extends MockUp<Collaborator>
   {
      @Mock(invocations = 1)
      void $init(int value)
      {
         assertEquals(5, value);
      }
   }

   @Test
   public void setUpMockForStaticMethod()
   {
      new MockCollaboratorForStaticMethod();

      //noinspection deprecation
      Collaborator.doInternal();
   }

   static class MockCollaboratorForStaticMethod extends MockUp<Collaborator>
   {
      @Mock(invocations = 1)
      static String doInternal() { return ""; }
   }

   @Test
   public void setUpMockForSubclassConstructor()
   {
      new MockSubCollaborator();

      new SubCollaborator(31);
   }

   static class SubCollaborator extends Collaborator
   {
      SubCollaborator(int i) { throw new RuntimeException(String.valueOf(i)); }

      @Override
      void provideSomeService() { value = 123; }
   }

   static class MockSubCollaborator extends MockUp<SubCollaborator>
   {
      @Mock(invocations = 1)
      void $init(int i) { assertEquals(31, i); }

      @SuppressWarnings("UnusedDeclaration")
      native void doNothing();
   }

   @Test
   public void setUpMocksForClassHierarchy()
   {
      new MockUp<SubCollaborator>() {
         @Mock void $init(Invocation inv, int i)
         {
            assertNotNull(inv.getInvokedInstance());
            assertTrue(i > 0);
         }

         @Mock void provideSomeService(Invocation inv)
         {
            SubCollaborator it = inv.getInvokedInstance();
            it.value = 45;
         }

         @Mock int getValue(Invocation inv)
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
      MockSystem mockUp = new MockSystem();
      assertEquals(0, System.nanoTime());

      mockUp.tearDown();
      assertTrue(System.nanoTime() > 0);
   }

   static class MockSystem extends MockUp<System> {
      @Mock public static long nanoTime() { return 0; }
   }

   @Test
   public void mockNativeMethodInClassWithoutRegisterNatives() throws Exception
   {
      MockFloat mockUp = new MockFloat();
      assertEquals(0.0, Float.intBitsToFloat(2243019), 0.0);

      mockUp.tearDown();
      assertTrue(Float.intBitsToFloat(2243019) > 0);
   }

   static class MockFloat extends MockUp<Float>
   {
      @SuppressWarnings("UnusedDeclaration")
      @Mock
      public static float intBitsToFloat(int bits) { return 0; }
   }

   @Test
   public void setUpMockForJREClass()
   {
      MockThread mockThread = new MockThread();

      Thread.currentThread().interrupt();

      assertTrue(mockThread.interrupted);
   }

   public static class MockThread extends MockUp<Thread>
   {
      boolean interrupted;

      @Mock(invocations = 1)
      public void interrupt() { interrupted = true; }
   }

   // Stubbing of static class initializers ///////////////////////////////////////////////////////////////////////////

   static class ClassWithStaticInitializers
   {
      static String str = "initialized"; // if final it would be a compile-time constant
      static final Object obj = new Object(); // constant, but only at runtime

      static { System.exit(1); }

      static void doSomething() {}

      static
      {
         try {
            Class.forName("NonExistentClass");
         }
         catch (ClassNotFoundException e) {
            e.printStackTrace();
         }
      }
   }

   @Test
   public void mockStaticInitializer()
   {
      new MockUp<ClassWithStaticInitializers>() {
         @Mock(invocations = 1) void $clinit() {}
      };

      ClassWithStaticInitializers.doSomething();

      assertNull(ClassWithStaticInitializers.str);
      assertNull(ClassWithStaticInitializers.obj);
   }

   static class AnotherClassWithStaticInitializers
   {
      static { System.exit(1); }
      static void doSomething() { throw new RuntimeException(); }
   }

   @Test
   public void stubOutStaticInitializer() throws Exception
   {
      new MockForClassWithInitializer();

      AnotherClassWithStaticInitializers.doSomething();
   }

   static class MockForClassWithInitializer extends MockUp<AnotherClassWithStaticInitializers>
   {
      @Mock void $clinit() {}

      @Mock(minInvocations = 1, maxInvocations = 1)
      void doSomething() {}
   }

   static class YetAnotherClassWithStaticInitializer
   {
      static { System.loadLibrary("none.dll"); }
      static void doSomething() {}
   }

   static class MockForYetAnotherClassWithInitializer extends MockUp<YetAnotherClassWithStaticInitializer>
   {
      @Mock void $clinit() {}
   }

   // Other tests /////////////////////////////////////////////////////////////////////////////////////////////////////

   @Test
   public void stubOutStaticInitializerWithEmptyMockClass() throws Exception
   {
      new MockForYetAnotherClassWithInitializer();

      YetAnotherClassWithStaticInitializer.doSomething();
   }

   @Test
   public void mockJREInterface() throws Exception
   {
      CallbackHandler callbackHandler = new MockCallbackHandler().getMockInstance();

      callbackHandler.handle(new Callback[] {new NameCallback("Enter name:")});
   }

   public static class MockCallbackHandler extends MockUp<CallbackHandler>
   {
      @Mock(invocations = 1)
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
         @Mock(invocations = 1)
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

   abstract static class AnAbstractClass { abstract int doSomething(); }

   @Test
   public <A extends AnAbstractClass> void mockAbstractClassWithMockForAbstractMethodHavingInvocationParameter()
   {
      final AnAbstractClass obj = new AnAbstractClass() { @Override int doSomething() { return 0; } };

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
            AnotherInterface invokedInstance = inv.getInvokedInstance();
            assertSame(interfaceInstance, invokedInstance);

            int invocationCount = inv.getInvocationCount();
            assertTrue(invocationCount > 0);

            return invocationCount == 1 ? invokedInstance.doSomething() : 123;
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

   static class GenericClass<T> { T doSomething() { return null; } }

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

   @Test
   public void stubbedOutAnnotatedMethodInMockedClass() throws Exception
   {
      new MockCollaborator7();

      assertTrue(Collaborator.class.getDeclaredMethod("doInternal").isAnnotationPresent(Deprecated.class));
   }

   static class MockCollaborator7 extends MockUp<Collaborator>
   {
      @Mock String doInternal() { return null; }
      @Mock void provideSomeService() {}
   }

   @Test
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
