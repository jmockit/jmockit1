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

import javax.swing.*;
import javax.swing.plaf.basic.*;

import org.junit.*;
import org.junit.rules.*;
import static org.junit.Assert.*;

public final class MockUpTest
{
   @Rule public final ExpectedException thrown = ExpectedException.none();

   @Test
   public void attemptToApplyMockUpWithoutTheTargetType()
   {
      thrown.expect(IllegalArgumentException.class);
      thrown.expectMessage("No target type");

      new MockUp() {};
   }

   // Mock-ups for classes ////////////////////////////////////////////////////////////////////////////////////////////

   @Test
   public void mockUpClass()
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
   public void mockUpGivenClass()
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
   public void attemptToMockGivenClassButPassNull()
   {
      thrown.expect(NullPointerException.class);

      new MockUp<Applet>((Class<?>) null) {};
   }

   @Test
   public void attemptToMockGivenInstanceButPassNull()
   {
      thrown.expect(NullPointerException.class);

      new MockUp<Applet>((Applet) null) {};
   }

   @SuppressWarnings("rawtypes")
   static class MockForGivenClass extends MockUp
   {
      @SuppressWarnings("unchecked")
      MockForGivenClass() { super(Applet.class); }

      @Mock
      String getParameter(String s) { return "mock"; }
   }

   @Test
   public void mockUpGivenClassUsingNamedMockUp()
   {
      new MockForGivenClass();

      String s = new Applet().getParameter("test");

      assertEquals("mock", s);
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
         @Mock
         public void run() {}
      }.getMockInstance();

      r.run();
   }

   @Test
   public <M extends Runnable & ResultSet> void mockUpTwoInterfacesAtOnce() throws Exception
   {
      M mock = new MockUp<M>() {
         @Mock
         void run() {}

         @Mock
         boolean next() { return true; }
      }.getMockInstance();

      mock.run();
      assertTrue(mock.next());
   }

   @SuppressWarnings("TypeParameterExtendsFinalClass")
   @Test
   public <M extends Applet & Runnable> void attemptToMockUpClassAndInterfaceAtOnce() throws Exception
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

   public static class PublicNamedMockUpWithNoInvocationParameters extends MockUp<Applet>
   {
      boolean executed;
      @Mock public void $init() { executed = true; }
      @Mock public String getParameter(String s) { return "45"; }
   }

   @Test
   public void publicNamedMockUpWithNoInvocationParameter()
   {
      PublicNamedMockUpWithNoInvocationParameters mockup = new PublicNamedMockUpWithNoInvocationParameters();

      Applet applet = new Applet();
      assertTrue(mockup.executed);

      String parameter = applet.getParameter("test");
      assertEquals("45", parameter);
   }

   @SuppressWarnings("deprecation")
   @Test
   public void mockingOfAnnotatedClass() throws Exception
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
   public void mockSameClassTwiceUsingSeparateMockups()
   {
      Applet a = new Applet();

      class MockUp1 extends MockUp<Applet> { @Mock void play(URL url) {} }
      new MockUp1();
      a.play(null);

      new MockUp<Applet>() { @Mock void showStatus(String s) {} };
      a.play(null); // still mocked
      a.showStatus("");
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

   @Test
   public void mockConstructorOfInnerClass()
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
   public void callMockMethodFromAWTEventDispatchingThread() throws Exception
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
}
