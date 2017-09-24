/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit;

import java.applet.*;
import java.awt.*;
import java.lang.reflect.*;
import java.net.*;
import java.rmi.*;
import java.sql.*;
import java.util.concurrent.atomic.*;

import javax.sound.midi.*;
import javax.swing.*;
import javax.swing.plaf.basic.*;

import org.junit.*;
import org.junit.rules.*;
import static org.junit.Assert.*;

public final class MockUpTest
{
   @Rule public final ExpectedException thrown = ExpectedException.none();

   @Test
   public void attemptToApplyFakeWithoutTheTargetType()
   {
      thrown.expect(IllegalArgumentException.class);
      thrown.expectMessage("No target type");

      new MockUp() {};
   }

   // Fakes for classes ///////////////////////////////////////////////////////////////////////////////////////////////

   @Test
   public void fakeAClass()
   {
      new MockUp<Applet>() {
         @Mock
         int getComponentCount() { return 123; }
      };

      assertEquals(123, new Applet().getComponentCount());
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
   public void fakeAGivenClass()
   {
      final Main main = new Main();
      AtomicIntegerFieldUpdater<?> atomicCount = Deencapsulation.getField(Main.class, AtomicIntegerFieldUpdater.class);

      new MockUp<AtomicIntegerFieldUpdater<?>>(atomicCount.getClass()) {
         boolean second;

         @Mock
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
   public void attemptToFakeGivenClassButPassNull()
   {
      thrown.expect(NullPointerException.class);

      new MockUp<Applet>(null) {};
   }

   @SuppressWarnings("rawtypes")
   static class FakeForGivenClass extends MockUp
   {
      @SuppressWarnings("unchecked")
      FakeForGivenClass() { super(Applet.class); }

      @Mock
      String getParameter(String s) { return "mock"; }
   }

   @Test
   public void fakeGivenClassUsingNamedFake()
   {
      new FakeForGivenClass();

      String s = new Applet().getParameter("test");

      assertEquals("mock", s);
   }

   // Fakes for interfaces ////////////////////////////////////////////////////////////////////////////////////////////

   @Test
   public void fakeAnInterface() throws Exception
   {
      ResultSet fake = new MockUp<ResultSet>() {
         @Mock
         boolean next() { return true; }
      }.getMockInstance();

      assertTrue(fake.next());
   }

   @Test
   public void fakeAGivenInterface()
   {
      Runnable r = new MockUp<Runnable>(Runnable.class) {
         @Mock
         public void run() {}
      }.getMockInstance();

      r.run();
   }

   @Test
   public <M extends Runnable & ResultSet> void fakeTwoInterfacesAtOnce() throws Exception
   {
      M fake = new MockUp<M>() {
         @Mock
         void run() {}

         @Mock
         boolean next() { return true; }
      }.getMockInstance();

      fake.run();
      assertTrue(fake.next());
   }

   @SuppressWarnings("TypeParameterExtendsFinalClass")
   @Test
   public <M extends Applet & Runnable> void attemptToFAkeClassAndInterfaceAtOnce() throws Exception
   {
      thrown.expect(IllegalArgumentException.class);
      thrown.expectMessage("java.applet.Applet is not an interface");

      new MockUp<M>() {
         @Mock String getParameter(String s) { return s.toLowerCase(); }
         @Mock void run() {}
      };
   }

   public interface SomeInterface { int doSomething(); }

   @Test
   public void callEqualsMethodOnFakedInterface()
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
   public void callHashCodeMethodOnFakedInterface()
   {
      SomeInterface proxy = new MockUp<SomeInterface>(){}.getMockInstance();

      assertEquals(System.identityHashCode(proxy), proxy.hashCode());
   }

   @Test
   public void callToStringMethodOnFakedInterface()
   {
      SomeInterface proxy = new MockUp<SomeInterface>(){}.getMockInstance();

      assertEquals(proxy.getClass().getName() + '@' + Integer.toHexString(proxy.hashCode()), proxy.toString());
   }

   // Fakes for other situations //////////////////////////////////////////////////////////////////////////////////////

   @Test
   public void fakeUsingInvocationParameters()
   {
      new MockUp<Applet>() {
         @Mock
         void $init(Invocation inv)
         {
            Applet it = inv.getInvokedInstance();
            assertNotNull(it);
         }

         @Mock
         int getBaseline(Invocation inv, int w, int h)
         {
            return inv.proceed();
         }
      };

      int i = new Applet().getBaseline(20, 15);

      assertEquals(-1, i);
   }

   public static class PublicNamedFakeWithNoInvocationParameters extends MockUp<Applet>
   {
      boolean executed;
      @Mock public void $init() { executed = true; }
      @Mock public String getParameter(String s) { return "45"; }
   }

   @Test
   public void publicNamedFakeWithNoInvocationParameter()
   {
      PublicNamedFakeWithNoInvocationParameters fake = new PublicNamedFakeWithNoInvocationParameters();

      Applet applet = new Applet();
      assertTrue(fake.executed);

      String parameter = applet.getParameter("test");
      assertEquals("45", parameter);
   }

   @SuppressWarnings("deprecation")
   @Test
   public void fakingOfAnnotatedClass() throws Exception
   {
      new MockUp<RMISecurityException>() {
         @Mock void $init(String s) { assertNotNull(s); }
      };

      assertTrue(RMISecurityException.class.isAnnotationPresent(Deprecated.class));

      Constructor<RMISecurityException> aConstructor = RMISecurityException.class.getDeclaredConstructor(String.class);
      assertTrue(aConstructor.isAnnotationPresent(Deprecated.class));

      Deprecated deprecated = aConstructor.getAnnotation(Deprecated.class);
      assertNotNull(deprecated);
   }

   @Test
   public void fakeSameClassTwiceUsingSeparateFakes()
   {
      Applet a = new Applet();

      class Fake1 extends MockUp<Applet> { @Mock void play(URL url) {} }
      new Fake1();
      a.play(null);

      new MockUp<Applet>() { @Mock void showStatus(String s) {} };
      a.play(null); // still faked
      a.showStatus("");
   }

   interface B
   {
      int aMethod();
      Integer anotherMethod();
   }

   @Test
   public void fakeANonPublicInterface()
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
   public void fakeSameInterfaceTwiceUsingSeparateFakes()
   {
      class Fake1 extends MockUp<C> { @Mock int method1() { return 1; } }
      C c1 = new Fake1().getMockInstance();
      assertEquals(1, c1.method1());
      assertEquals(0, c1.method2());

      C c2 = new MockUp<C>() { @Mock int method2() { return 2; } }.getMockInstance();
      assertEquals(0, c2.method1()); // not faked because c2 belongs to a second implementation class for C
      assertEquals(2, c2.method2());

      // Instances c1 and c2 belong to different faked classes, so c1 is unaffected:
      assertEquals(1, c1.method1());
      assertEquals(0, c1.method2());
   }

   @Test
   public void fakeConstructorOfInnerClass()
   {
      final BasicColorChooserUI outer = new BasicColorChooserUI();
      final boolean[] constructed = {false};

      new MockUp<BasicColorChooserUI.PropertyHandler>() {
         @Mock
         void $init(BasicColorChooserUI o)
         {
            assertSame(outer, o);
            constructed[0] = true;
         }
      };

      outer.new PropertyHandler();
      assertTrue(constructed[0]);
   }

   @Test
   public void callFakMethodFromAWTEventDispatchingThread() throws Exception
   {
      new MockUp<Panel>() {
         @Mock int getComponentCount() { return 10; }
      };

      SwingUtilities.invokeAndWait(new Runnable() {
         @Override
         public void run()
         {
            int i = new Panel().getComponentCount();
            assertEquals(10, i);
         }
      });
   }

   static final class JRESubclass extends Patch { JRESubclass(int i, int j) { super(i, j); } }

   @Test
   public void anonymousFakeForJRESubclassHavingFakeMethodForJREMethod()
   {
      new MockUp<JRESubclass>() { @Mock int getBank() { return 123; } };

      Patch t = new JRESubclass(1, 2);
      int i = t.getBank();

      assertEquals(123, i);
   }

   static Boolean fakeTornDown;

   static final class FakeWithActionOnTearDown extends MockUp<Applet>
   {
      @Override
      protected void onTearDown() { fakeTornDown = true; }
   }

   @Test
   public void performActionOnFakeTearDown()
   {
      fakeTornDown = false;
      new FakeWithActionOnTearDown();
      assertFalse(fakeTornDown);
   }

   @AfterClass
   public static void verifyFakeAppliedInTestWasTornDown()
   {
      assertTrue(fakeTornDown == null || fakeTornDown);
   }
}
