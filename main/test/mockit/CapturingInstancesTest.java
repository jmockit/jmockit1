/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit;

import java.util.*;
import java.util.concurrent.*;

import org.junit.*;

import static org.junit.Assert.*;

public final class CapturingInstancesTest
{
   public interface Service1 { int doSomething(); }
   static final class Service1Impl implements Service1 { @Override public int doSomething() { return 1; } }

   public static final class TestedUnit
   {
      private final Service1 service1 = new Service1Impl();
      private final Service1 service2 = new Service1() { @Override public int doSomething() { return 2; } };
      Observable observable;

      public int businessOperation(final boolean b)
      {
         new Callable() {
            @Override public Object call() { throw new IllegalStateException(); }
         }.call();

         observable = new Observable() {{
            if (b) {
               throw new IllegalArgumentException();
            }
         }};

         return service1.doSomething() + service2.doSomething();
      }
   }

   @Capturing Service1 service;

   @Test
   public void captureServiceInstancesCreatedByTestedConstructor()
   {
      TestedUnit unit = new TestedUnit();

      assertEquals(0, unit.service1.doSomething());
      assertEquals(0, unit.service2.doSomething());
   }

   @Test
   public void captureAllInternallyCreatedInstances(
      @Capturing Observable observable, @Capturing final Callable<?> callable) throws Exception
   {
      new Expectations() {{
         service.doSomething(); returns(3, 4);
      }};

      TestedUnit unit = new TestedUnit();
      int result = unit.businessOperation(true);
      assertEquals(4, unit.service1.doSomething());
      assertEquals(4, unit.service2.doSomething());

      assertNotNull(unit.observable);
      assertEquals(7, result);

      new Verifications() {{ callable.call(); }};
   }

   public interface Service2 { int doSomething(); }
   static class Base { boolean doSomething() { return false; } }
   static final class Derived extends Base { Service2 doSomethingElse() { return null; } }

   @Test
   public void captureSubclassAndCascadeFromMethodExclusiveToSubclass(@Capturing Base capturingMock)
   {
      Derived d = new Derived();
      Service2 service2 = d.doSomethingElse();

      // Classes mocked only because they implement/extend a capturing base type do not cascade from methods
      // that exist only in them.
      //noinspection ConstantConditions
      assertNull(service2);
   }
}