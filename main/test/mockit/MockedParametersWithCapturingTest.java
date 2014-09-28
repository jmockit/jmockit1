/*
 * Copyright (c) 2006-2014 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit;

import java.nio.*;

import org.junit.*;
import static org.junit.Assert.*;

import org.jetbrains.annotations.*;

public final class MockedParametersWithCapturingTest
{
   public interface Service
   {
      int doSomething();
      void doSomethingElse(int i);
   }

   static final class ServiceImpl implements Service
   {
      final String str;

      ServiceImpl() { str = ""; }
      ServiceImpl(String str) { this.str = str; }

      @Override public int doSomething() { return 1; }
      @Override public void doSomethingElse(int i) { throw new IllegalMonitorStateException(); }

      private boolean privateMethod() { return true; }
      static boolean staticMethod() { return true; }
   }

   public static final class TestedUnit
   {
      final Service service1 = new ServiceImpl("test");

      final Service service2 = new Service() {
         @Override public int doSomething() { return 2; }
         @Override public void doSomethingElse(int i) {}
      };

      public int businessOperation()
      {
         return service1.doSomething() + service2.doSomething();
      }
   }

   @Test
   public void captureInstancesWithoutMockingAnyMethods(@Capturing(maxInstances = 2) @Mocked("") Service service)
   {
      assertEquals(0, service.doSomething());

      TestedUnit unit = new TestedUnit();
      assertEquals(3, unit.businessOperation());

      assertTrue(ServiceImpl.staticMethod());

      ServiceImpl service1 = (ServiceImpl) unit.service1;
      assertTrue(service1.privateMethod());
      assertEquals("test", service1.str);
   }

   @Test(expected = IllegalMonitorStateException.class)
   public void mockStrictlyOnlySpecifiedMethod(@Capturing @Mocked("doSomething") final Service service)
   {
      new StrictExpectations() {{ service.doSomething(); returns(3, 4); }};

      assertEquals(7, new TestedUnit().businessOperation());

      // Not mocked, so it will throw an exception:
      new ServiceImpl().doSomethingElse(1);
   }

   @Test
   public void mockNonStrictlyOnlySpecifiedMethod(@Mocked("doSomethingElse") @Capturing final Service service)
   {
      ServiceImpl impl = new ServiceImpl();
      impl.doSomethingElse(5);
      impl.doSomethingElse(-1);

      assertEquals(1, impl.doSomething());
      assertEquals(1, new ServiceImpl().doSomething());
      assertEquals(3, new TestedUnit().businessOperation());

      new Verifications() {{ service.doSomethingElse(anyInt); times = 2; }};
   }

   static class BaseClass
   {
      final String str;
      BaseClass() { str = ""; }
      BaseClass(String str) { this.str = str; }
      String getStr() { return str; }
      void doSomething() { throw new IllegalStateException("Invalid state"); }
   }

   static class DerivedClass extends BaseClass
   {
      DerivedClass() {}
      DerivedClass(String str) { super(str); }
      @Override String getStr() { return super.getStr().toUpperCase(); }
   }

   @SuppressWarnings("UnusedDeclaration")
   Object[] valueForSuper(String s)
   {
      return new Object[] {"mock"};
   }

   @Test
   public void captureDerivedClass(@Capturing BaseClass service)
   {
      assertNull(new DerivedClass("test").str);
      assertNull(new DerivedClass() {}.str);
   }

   @Test
   public void captureDerivedClassButWithoutMockingAnything(@Capturing @Mocked("") BaseClass mock)
   {
      assertNull(mock.str);
      assertEquals("test", new DerivedClass("test").str);
   }

   @Test
   public void captureImplementationsOfDifferentInterfacesWithPartialMockingFiltersForEach(
      @Capturing @Mocked("run") Runnable mock1, @Capturing @Mocked("read") Readable mock2)
      throws Exception
   {
      Runnable runnable = new Runnable() {
         @Override
         public void run() { throw new RuntimeException("run"); }
      };
      runnable.run();

      Readable readable = new Readable() {
         @Override
         public int read(@NotNull CharBuffer cb) { throw new RuntimeException("read"); }
      };
      readable.read(CharBuffer.wrap("test"));
   }

   @Test
   public void captureAndPartiallyMockImplementationsOfAnInterface(@Capturing final Service service)
   {
      new Expectations(Service.class) {{
         service.doSomethingElse(1);
      }};

      Service impl1 = new ServiceImpl("test1");
      assertEquals(1, impl1.doSomething());
      impl1.doSomethingElse(1);

      Service impl2 = new Service() {
         @Override public int doSomething() { return 2; }
         @Override public void doSomethingElse(int i) { throw new IllegalStateException("2"); }
      };
      assertEquals(2, impl2.doSomething());
      impl2.doSomethingElse(1);

      try {
         impl2.doSomethingElse(2);
         fail();
      }
      catch (IllegalStateException ignore) {}
   }

   @Test
   public void captureAndPartiallyMockSubclassesOfABaseClass(@Capturing final BaseClass base)
   {
      new Expectations(BaseClass.class) {{
         base.doSomething();
      }};

      BaseClass impl1 = new DerivedClass("test1");
      assertEquals("TEST1", impl1.getStr());
      impl1.doSomething();

      BaseClass impl2 = new BaseClass("test2") {
         @Override void doSomething() { throw new IllegalStateException("2"); }
      };
      assertEquals("test2", impl2.getStr());
      impl2.doSomething();

      final class DerivedClass2 extends DerivedClass {
         DerivedClass2() { super("DeRiVed"); }
         @Override String getStr() { return super.getStr().toLowerCase(); }
      }
      DerivedClass2 impl3 = new DerivedClass2();
      impl3.doSomething();
      assertEquals("derived", impl3.getStr());
   }
}