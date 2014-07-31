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
   public void mockOnlySpecifiedMethod(@Capturing @Mocked("doSomething") final Service service)
   {
      new Expectations() {{ service.doSomething(); returns(3, 4); }};

      assertEquals(7, new TestedUnit().businessOperation());

      // Not mocked, so it will throw an exception:
      new ServiceImpl().doSomethingElse(1);
   }

   @Test
   public void mockAllMethodsExceptOne(@Mocked("doSomethingElse") @Capturing final Service service)
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
   }

   static class DerivedClass extends BaseClass
   {
      DerivedClass() {}
      DerivedClass(String str) { super(str); }
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
}