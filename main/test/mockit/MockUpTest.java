/*
 * Copyright (c) 2006-2015 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit;

import java.lang.reflect.*;
import java.sql.*;
import java.util.concurrent.atomic.*;

import org.junit.*;
import org.junit.rules.*;
import static org.junit.Assert.*;

@SuppressWarnings("deprecation")
public final class MockUpTest
{
   @Rule public final ExpectedException thrown = ExpectedException.none();

   @Test
   public void attemptToCreateMockUpWithoutTheTypeToBeMocked()
   {
      thrown.expect(IllegalArgumentException.class);
      thrown.expectMessage("No type to be mocked");

      new MockUp() {};
   }

   // Mock-ups for classes ////////////////////////////////////////////////////////////////////////////////////////////

   @Deprecated
   static final class Collaborator
   {
      @Deprecated final boolean b;

      @Deprecated Collaborator() { b = true; }
      Collaborator(boolean b) { this.b = b; }

      @Ignore("test") int doSomething(@Deprecated String s) { return s.length(); }

      <N extends Number> N genericMethod(@SuppressWarnings("unused") N n) { return null; }

      @Deprecated static boolean doSomethingElse() { return false; }
   }

   @Test
   public void attemptToCreateMockUpWithMockMethodLackingCorrespondingRealMethod()
   {
      thrown.expect(IllegalArgumentException.class);
      thrown.expectMessage("$init(int");

      new MockUp<Collaborator>() { @Mock void $init(int i) { System.out.println(i); } };
   }

   @Test
   public void mockUpClass() throws Exception
   {
      new MockUp<Collaborator>() {
         @Mock(invocations = 1)
         void $init(boolean b)
         {
            assertTrue(b);
         }

         @Mock(minInvocations = 1)
         int doSomething(String s)
         {
            assertEquals("test", s);
            return 123;
         }
      };

      assertEquals(123, new Collaborator(true).doSomething("test"));
   }

   static final class Main
   {
      static final AtomicIntegerFieldUpdater<Main> atomicCount =
         AtomicIntegerFieldUpdater.newUpdater(Main.class, "count");

      volatile int count;
      int max = 2;

      boolean increment()
      {
         while (true) {
            int currentCount = count;

            if (currentCount >= max) {
               return false;
            }

            if (atomicCount.compareAndSet(this, currentCount, currentCount + 1)) {
               return true;
            }
         }
      }
   }

   @Test
   public void mockUpGivenClass()
   {
      final Main main = new Main();
      AtomicIntegerFieldUpdater<?> atomicCount = Deencapsulation.getField(Main.class, AtomicIntegerFieldUpdater.class);

      new MockUp<AtomicIntegerFieldUpdater<?>>(atomicCount.getClass()) {
         boolean second;

         @Mock(invocations = 2)
         public boolean compareAndSet(Object obj, int expect, int update)
         {
            assertSame(main, obj);
            assertEquals(0, expect);
            assertEquals(1, update);

            if (second) {
               return true;
            }

            second = true;
            return false;
         }
      };

      assertTrue(main.increment());
   }

   @Test
   public void attemptToMockGivenClassButPassNull()
   {
      thrown.expect(IllegalArgumentException.class);
      thrown.expectMessage("Null reference");

      new MockUp<Collaborator>((Class<?>) null) {};
   }

   @Test
   public void attemptToMockGivenInstanceButPassNull()
   {
      thrown.expect(IllegalArgumentException.class);
      thrown.expectMessage("Null reference");

      new MockUp<Collaborator>((Collaborator) null) {};
   }

   @SuppressWarnings("rawtypes")
   static class MockForGivenClass extends MockUp
   {
      @SuppressWarnings("unchecked")
      MockForGivenClass() { super(Collaborator.class); }

      @Mock
      int doSomething(String s) { return s.length() + 1; }
   }

   @Test
   public void mockUpGivenClassUsingNamedMockUp()
   {
      new MockForGivenClass();

      int i = new Collaborator().doSomething("test");
      assertEquals(5, i);
   }

   // Mock-ups for interfaces /////////////////////////////////////////////////////////////////////////////////////////

   @Test
   public void mockUpInterface() throws Exception
   {
      ResultSet mock = new MockUp<ResultSet>() {
         @Mock
         boolean next() { return true; }
      }.getMockInstance();

      assertTrue(mock.next());
   }

   @Test
   public void mockUpGivenInterface()
   {
      Runnable r = new MockUp<Runnable>(Runnable.class) {
         @Mock(minInvocations = 1)
         public void run() {}
      }.getMockInstance();

      r.run();
   }

   @Test
   public <M extends Runnable & ResultSet> void mockUpTwoInterfacesAtOnce() throws Exception
   {
      M mock = new MockUp<M>() {
         @Mock(invocations = 1)
         void run() {}

         @Mock
         boolean next() { return true; }
      }.getMockInstance();

      mock.run();
      assertTrue(mock.next());
   }

   @SuppressWarnings("TypeParameterExtendsFinalClass")
   @Test
   public <M extends Collaborator & Runnable> void attemptToMockUpClassAndInterfaceAtOnce() throws Exception
   {
      thrown.expect(IllegalArgumentException.class);
      thrown.expectMessage("Collaborator is not an interface");

      new MockUp<M>() {
         @Mock int doSomething(String s) { return s.length() + 1; }
         @Mock void run() {}
      };
   }

   public interface SomeInterface { int doSomething(); }

   @Test
   public void callEqualsMethodOnMockedUpInterface()
   {
      SomeInterface proxy1 = new MockUp<SomeInterface>(){}.getMockInstance();
      SomeInterface proxy2 = new MockUp<SomeInterface>(){}.getMockInstance();

      //noinspection SimplifiableJUnitAssertion,EqualsWithItself
      assertTrue(proxy1.equals(proxy1));
      assertFalse(proxy1.equals(proxy2));
      assertFalse(proxy2.equals(proxy1));
      //noinspection ObjectEqualsNull
      assertFalse(proxy1.equals(null));
   }

   @Test
   public void callHashCodeMethodOnMockedUpInterface()
   {
      SomeInterface proxy = new MockUp<SomeInterface>(){}.getMockInstance();

      assertEquals(System.identityHashCode(proxy), proxy.hashCode());
   }

   @Test
   public void callToStringMethodOnMockedUpInterface()
   {
      SomeInterface proxy = new MockUp<SomeInterface>(){}.getMockInstance();

      assertEquals(proxy.getClass().getName() + '@' + Integer.toHexString(proxy.hashCode()), proxy.toString());
   }

   // Mock-ups for other situations ///////////////////////////////////////////////////////////////////////////////////

   @Test
   public void mockUpUsingInvocationParameters()
   {
      new MockUp<Collaborator>() {
         @Mock(invocations = 1)
         void $init(Invocation inv, boolean b)
         {
            Collaborator it = inv.getInvokedInstance();
            assertFalse(it.b);
            assertTrue(b);
         }

         @Mock
         int doSomething(Invocation inv, String s)
         {
            return inv.proceed(s + ": mocked");
         }
      };

      int i = new Collaborator(true).doSomething("test");

      assertEquals(12, i);
   }

   public static class PublicNamedMockUpWithNoInvocationParameters extends MockUp<Collaborator>
   {
      boolean executed;
      @Mock public void $init() { executed = true; }
      @Mock public int doSomething(String s) { return 45; }
   }

   @Test
   public void publicNamedMockUpWithNoInvocationParameter()
   {
      PublicNamedMockUpWithNoInvocationParameters mockUp = new PublicNamedMockUpWithNoInvocationParameters();

      Collaborator col = new Collaborator();
      assertTrue(mockUp.executed);
      assertFalse(col.b);

      int i = col.doSomething("test");
      assertEquals(45, i);
   }

   public static final class NamedMockUp extends MockUp<Collaborator>
   {
      @Mock
      public void $init(Invocation inv, boolean b)
      {
         assertMockMethodCalledDirectlyFromMockedClass();
         assertTrue(inv.getInvokedInstance() instanceof Collaborator);

         Object[] arguments = inv.getInvokedArguments();
         assertEquals(1, arguments.length);
         assertEquals(b, arguments[0]);

         if (inv.getInvocationCount() == 1) {
            inv.proceed();
         }
      }

      @Mock
      public static int doSomething(Invocation inv, String s)
      {
         assertMockMethodCalledDirectlyFromMockedClass();
         assertTrue(inv.getInvokedInstance() instanceof Collaborator);

         Object[] arguments = inv.getInvokedArguments();
         assertEquals(1, arguments.length);
         assertSame(s, arguments[0]);

         if (inv.getInvocationIndex() == 0) return 123;
         return inv.proceed();
      }

      private static void assertMockMethodCalledDirectlyFromMockedClass()
      {
         StackTraceElement[] callStack = new Throwable().getStackTrace();
         assertEquals(Collaborator.class.getName(), callStack[2].getClassName());
      }

      @Mock
      public static boolean doSomethingElse(Invocation inv)
      {
         assertMockMethodCalledDirectlyFromMockedClass();
         assertNull(inv.getInvokedInstance());
         assertEquals(0, inv.getInvokedArguments().length);
         return true;
      }

      @Mock
      public <N extends Number> N genericMethod(Invocation inv, N n)
      {
         assertMockMethodCalledDirectlyFromMockedClass();
         if (n == null) return inv.proceed();
         return n;
      }
   }

   @Test
   public void namedMockUpWithPublicMockMethodsUsingInvocationParameter()
   {
      Collaborator col = new Collaborator();
      assertTrue(col.b);

      new NamedMockUp();

      assertTrue(Collaborator.doSomethingElse());
      assertEquals(123, col.doSomething("test"));
      assertEquals(5, col.doSomething("test2"));
      assertTrue(new Collaborator(true).b);
      assertFalse(new Collaborator(true).b);
      assertEquals(45L, col.genericMethod(45L).longValue());
      assertEquals(4.5F, col.genericMethod(4.5F).floatValue(), 0);
      assertNull(col.genericMethod(null));
   }

   @Test
   public void cannotReenterConstructorsWithReplacementArguments()
   {
      thrown.expect(UnsupportedOperationException.class);
      thrown.expectMessage("Cannot replace arguments when proceeding into constructor");

      new MockUp<Collaborator>() {
         @Mock void $init(Invocation inv, boolean b) { inv.proceed(true); }
      };

      new Collaborator(false);
   }

   @Test
   public void mockingOfAnnotatedClass() throws Exception
   {
      new MockUp<Collaborator>() {
         @Mock void $init() {}
         @Mock int doSomething(String s) { assertNotNull(s); return 123; }
         @Mock boolean doSomethingElse(Invocation inv) { return true; }
      };

      assertEquals(123, new Collaborator().doSomething(""));

      assertTrue(Collaborator.class.isAnnotationPresent(Deprecated.class));
      assertTrue(Collaborator.class.getDeclaredField("b").isAnnotationPresent(Deprecated.class));
      assertTrue(Collaborator.class.getDeclaredConstructor().isAnnotationPresent(Deprecated.class));

      Method mockedMethod = Collaborator.class.getDeclaredMethod("doSomething", String.class);
      Ignore ignore = mockedMethod.getAnnotation(Ignore.class);
      assertNotNull(ignore);
      assertEquals("test", ignore.value());
      assertTrue(mockedMethod.getParameterAnnotations()[0][0] instanceof Deprecated);

      assertTrue(Collaborator.doSomethingElse());
      assertTrue(Collaborator.class.getDeclaredMethod("doSomethingElse").isAnnotationPresent(Deprecated.class));
   }

   static class A
   {
      void method1() { throw new RuntimeException("1"); }
      void method2() { throw new RuntimeException("2"); }
   }

   @Test
   public void mockSameClassTwiceUsingSeparateMockups()
   {
      A a = new A();

      class MockUp1 extends MockUp<A> { @Mock void method1() {} }
      new MockUp1();
      a.method1();

      new MockUp<A>() { @Mock void method2() {} };
      a.method1(); // still mocked
      a.method2();
   }

   interface B
   {
      int aMethod();
      Integer anotherMethod();
   }

   @Test
   public void mockNonPublicInterface()
   {
      B b = new MockUp<B>() {
         @Mock int aMethod() { return 1; }
      }.getMockInstance();

      assertEquals(1, b.aMethod());
      assertEquals(0, b.anotherMethod().intValue());
   }

   public interface C
   {
      int method1();
      int method2();
   }

   @Test
   public void mockSameInterfaceTwiceUsingSeparateMockups()
   {
      class MockUp1 extends MockUp<C> { @Mock int method1() { return 1; } }
      C c1 = new MockUp1().getMockInstance();
      assertEquals(1, c1.method1());
      assertEquals(0, c1.method2());

      C c2 = new MockUp<C>() { @Mock int method2() { return 2; } }.getMockInstance();
      assertEquals(0, c2.method1()); // not mocked because c2 belongs to a second implementation class for C
      assertEquals(2, c2.method2());

      // Instances c1 and c2 belong to different mocked classes, so c1 is unaffected:
      assertEquals(1, c1.method1());
      assertEquals(0, c1.method2());
   }

   static class Outer
   {
      class Inner
      {
         final int value;
         Inner(int value) { this.value = value; }
      }
   }

   @Test
   public void mockConstructorOfInnerClass()
   {
      final Outer outer = new Outer();

      new MockUp<Outer.Inner>() {
         @Mock void $init(Outer o, int i)
         {
            assertSame(outer, o);
            assertEquals(123, i);
         }
      };

      Outer.Inner inner = outer.new Inner(123);
      assertEquals(0, inner.value);
   }

   static class Base { Base(int i) { assert i > 0; } }
   static class Derived extends Base { Derived() { super(123); } }

   @Test
   public void attemptToMockConstructorNotFoundInMockedClassButFoundInASuperClass()
   {
      thrown.expect(IllegalArgumentException.class);
      thrown.expectMessage("$init(int");

      new MockUp<Derived>() {
         @Mock void $init(int i) {}
      };
   }

   static class ClassWithConstructorCallingAnother
   {
      final Number value;
      ClassWithConstructorCallingAnother() { this(123); }
      ClassWithConstructorCallingAnother(long l) { value = l; }
   }

   @Test
   public void mockConstructorWhichCallsAnotherWhoseLastParameterHasDoubleWordSize()
   {
      new MockUp<ClassWithConstructorCallingAnother>() {
         @Mock void $init() {}
      };

      ClassWithConstructorCallingAnother a = new ClassWithConstructorCallingAnother();

      assertNull(a.value);
   }
}
