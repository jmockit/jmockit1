/*
 * Copyright (c) 2006-2014 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package integrationTests.serviceA;

import java.util.*;
import static java.util.Arrays.*;

import org.junit.*;
import static org.junit.Assert.*;

import mockit.*;

import integrationTests.serviceB.*;

public final class ServiceATest
{
   @Before
   public void setUp()
   {
      new MockServiceBThatAvoidsStaticInitialization();
   }

   public static final class MockServiceBThatAvoidsStaticInitialization extends MockUp<ServiceB>
   {
      @Mock
      public static void $clinit()
      {
         // Do nothing.
      }
   }

   @Test
   public void serviceBCalledExactlyOnce()
   {
      new MockServiceBForOneInvocation();

      boolean result = new ServiceA().doSomethingThatUsesServiceB(2, "test");

      assertTrue(result);
   }

   public static class MockServiceBForOneInvocation extends MockUp<ServiceB>
   {
      @Mock(invocations = 1)
      public int computeX(int a, int b)
      {
         // Asserts that the received arguments meets the expected values.
         // Equivalent jMock2 expectations: one(mockOfServiceB).computeX(2, 5);
         assertEquals(2, a);
         assertEquals(5, b);

         // Returns the expected result.
         // Equivalent jMock2 expectation: will(returnValue(7));
         return 7;
      }
   }

   @Test
   public void serviceBCalledAtLeastTwoTimes()
   {
      new MockServiceBForTwoInvocations();

      new ServiceA().doSomethingElseUsingServiceB(3);
   }

   public static final class MockServiceBForTwoInvocations extends MockUp<ServiceB>
   {
      @Mock(minInvocations = 2)
      public int computeX(int a, int b)
      {
         assertTrue(a + b >= 0);
         return 0;
      }
   }

   @Test
   public void serviceBCalledAtLeastOnceAndAtMostThreeTimes()
   {
      new MockServiceBForOneToThreeInvocations();
      new MockServiceBHelper();

      ServiceA serviceA = new ServiceA();
      serviceA.doSomethingElseUsingServiceB(2);
      String config = serviceA.getConfig();

      assertEquals("test", config);
   }

   public static final class MockServiceBForOneToThreeInvocations extends MockUp<ServiceB>
   {
      @Mock(invocations = 1)
      public void $init(Invocation inv, String config)
      {
         assertNotNull(inv.getInvokedInstance());
         assertEquals("config", config);
      }

      @Mock(minInvocations = 1, maxInvocations = 3)
      public int computeX(Invocation inv, int a, int b)
      {
         assertTrue(a + b >= 0);
         assertNotNull(inv.getInvokedInstance());
         return a - b;
      }

      @Mock
      public String getConfig(Invocation inv)
      {
         String config = inv.proceed();
         assertNull(config);
         return "test";
      }
   }

   static class MockServiceBHelper extends MockUp<ServiceB.Helper>
   {
      @Mock(invocations = 0)
      void $init()
      {
         throw new IllegalStateException("should not be created");
      }
   }

   @Test
   public void beforeAdvice()
   {
      OnEntryTracingAspect aspect = new OnEntryTracingAspect();

      ServiceB b = new ServiceB("test");

      assertEquals(3, b.computeX(1, 2));
      assertEquals(5, b.computeX(2, 3));
      assertEquals(-10, b.computeX(0, -10));
      assertEquals(asList(3, 5, -10), aspect.values);
   }

   public static class OnEntryTracingAspect extends MockUp<ServiceB>
   {
      final List<Integer> values = new ArrayList<Integer>();

      @Mock
      public int computeX(Invocation inv, int a, int b)
      {
         values.add(a + b);
         return inv.proceed();
      }
   }

   @Test
   public void afterAdvice()
   {
      OnExitTracingAspect aspect = new OnExitTracingAspect();

      ServiceB b = new ServiceB("test");

      assertEquals(3, b.computeX(1, 2));
      assertEquals(5, b.computeX(2, 3));
      assertEquals(-10, b.computeX(0, -10));
      assertEquals(asList(3, 5, -10), aspect.values);
   }

   public static class OnExitTracingAspect extends MockUp<ServiceB>
   {
      final List<Integer> values = new ArrayList<Integer>();

      @Mock
      public int computeX(Invocation inv, int a, int b)
      {
         Integer x;

         try {
            x = inv.proceed();
            return x;
         }
         finally {
            // Statements to be executed on exit would be here.
            //noinspection UnusedAssignment
            x = a + b;
            values.add(x);
         }
      }
   }

   @Test
   public void aroundAdvice()
   {
      TracingAspect aspect = new TracingAspect();

      ServiceB b = new ServiceB("test");

      assertEquals(3, b.computeX(1, 2));
      assertEquals(5, b.computeX(2, 3));
      assertEquals(-10, b.computeX(0, -10));
      assertEquals(asList(1, 2, 2, 3, 0, -10), aspect.inputValues);
      assertEquals(asList(3, 5, -10), aspect.outputValues);
   }

   public static class TracingAspect extends MockUp<ServiceB>
   {
      final List<Integer> inputValues = new ArrayList<Integer>();
      final List<Integer> outputValues = new ArrayList<Integer>();

      @Mock
      public int computeX(Invocation inv, int a, int b)
      {
         inputValues.add(a);
         inputValues.add(b);
         int x = inv.proceed();
         outputValues.add(x);
         return x;
      }
   }
}
