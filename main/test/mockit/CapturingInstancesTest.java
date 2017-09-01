/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit;

import java.nio.*;
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

   @Capturing(maxInstances = 2) Service1 service;

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
   static final class Service2Impl implements Service2 { @Override public int doSomething() { return 2; } }

   @Test
   public void recordExpectationsForNextTwoInstancesToBeCreated(
      @Capturing(maxInstances = 1) final Service2 mock1, @Capturing(maxInstances = 1) final Service2 mock2)
   {
      new Expectations() {{
         mock1.doSomething(); result = 11;
         mock2.doSomething(); result = 22;
      }};

      Service2Impl s1 = new Service2Impl();
      Service2Impl s2 = new Service2Impl();
      assertEquals(22, s2.doSomething());
      assertEquals(11, s1.doSomething());
      assertEquals(11, s1.doSomething());
      assertEquals(22, s2.doSomething());
      assertEquals(11, s1.doSomething());
   }

   @Test
   public void recordExpectationsForNextTwoInstancesOfTwoDifferentImplementingClasses(
      @Capturing(maxInstances = 1) final Service2 mock1, @Capturing(maxInstances = 1) final Service2 mock2)
   {
      new Expectations() {{
         mock1.doSomething(); result = 1;
         mock2.doSomething(); result = 2;
      }};

      Service2 s1 = new Service2() { @Override public int doSomething() { return -1; } };
      assertEquals(1, s1.doSomething());

      Service2 s2 = new Service2() { @Override public int doSomething() { return -2; } };
      assertEquals(2, s2.doSomething());
   }

   @Test
   public void recordExpectationsForTwoConsecutiveSetsOfFutureInstances(
      @Capturing(maxInstances = 2) final Service2 set1, @Capturing(maxInstances = 3) final Service2 set2)
   {
      new Expectations() {{
         set1.doSomething(); result = 1;
         set2.doSomething(); result = 2;
      }};

      // First set of instances, matching the expectation on "s1":
      Service2 s1 = new Service2Impl();
      Service2 s2 = new Service2Impl();
      assertEquals(1, s1.doSomething());
      assertEquals(1, s2.doSomething());

      // Second set of instances, matching the expectation on "s2":
      Service2 s3 = new Service2Impl();
      Service2 s4 = new Service2Impl();
      Service2 s5 = new Service2Impl();
      assertEquals(2, s3.doSomething());
      assertEquals(2, s4.doSomething());
      assertEquals(2, s5.doSomething());

      // Third set of instances, not matching any expectation:
      Service2 s6 = new Service2Impl();
      assertEquals(0, s6.doSomething());
   }

   @Test
   public void recordExpectationsForNextTwoInstancesToBeCreatedUsingMockParameters(
      @Capturing(maxInstances = 1) final Service2 s1, @Capturing(maxInstances = 1) final Service2 s2)
   {
      new Expectations() {{
         s2.doSomething(); result = 22;
         s1.doSomething(); result = 11;
      }};

      Service2Impl cs1 = new Service2Impl();
      assertEquals(11, cs1.doSomething());

      Service2Impl cs2 = new Service2Impl();
      assertEquals(22, cs2.doSomething());
      assertEquals(11, cs1.doSomething());
      assertEquals(22, cs2.doSomething());
   }

   static class Base { boolean doSomething() { return false; } }
   static final class Derived1 extends Base {}
   static final class Derived2 extends Base { Service2 doSomethingElse() { return null; } }

   @Test
   public void verifyExpectationsOnlyOnFirstOfTwoCapturedInstances(@Capturing(maxInstances = 1) final Base b)
   {
      new Expectations() {{
         b.doSomething(); result = true; times = 1;
      }};

      assertTrue(new Derived1().doSomething());
      assertFalse(new Derived2().doSomething());
   }

   @Test
   public void verifyExpectationsOnlyOnOneOfTwoSubclassesForTwoCapturedInstances(
      @Capturing(maxInstances = 1) final Derived1 firstCapture,
      @Capturing(maxInstances = 1) final Derived1 secondCapture)
   {
      new Expectations() {{
         new Derived1(); times = 2;
         firstCapture.doSomething(); result = true; times = 1;
         secondCapture.doSomething(); result = true; times = 1;
      }};

      assertTrue(new Derived1().doSomething());
      assertFalse(new Derived2().doSomething());
      assertTrue(new Derived1().doSomething());
   }

   @Test
   public void captureSubclassAndCascadeFromMethodExclusiveToSubclass(@Capturing Base capturingMock)
   {
      Derived2 d = new Derived2();
      Service2 service2 = d.doSomethingElse();

      // Classes mocked only because they implement/extend a capturing base type do not cascade from methods
      // that exist only in them.
      //noinspection ConstantConditions
      assertNull(service2);
   }

   @Test
   public void specifyDifferentBehaviorForFirstNewInstanceAndForRemainingNewInstances(
      @Capturing(maxInstances = 1) final Buffer firstNewBuffer, @Capturing final Buffer remainingNewBuffers)
   {
      new Expectations() {{
         firstNewBuffer.position(); result = 10;
         remainingNewBuffers.position(); result = 20;
      }};

      ByteBuffer buffer1 = ByteBuffer.allocate(100);
      IntBuffer  buffer2 = IntBuffer.wrap(new int[] {1, 2, 3});
      CharBuffer buffer3 = CharBuffer.wrap("                ");

      assertEquals(10, buffer1.position());
      assertEquals(20, buffer2.position());
      assertEquals(20, buffer3.position());
   }
}