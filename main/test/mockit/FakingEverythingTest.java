/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit;

import java.lang.reflect.*;
import java.util.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.*;
import javax.xml.transform.sax.*;
import static java.util.Arrays.*;

import org.junit.*;
import static org.junit.Assert.*;

public final class FakingEverythingTest
{
   final List<String> traces = new ArrayList<String>();

   void traceEntry(Invocation inv) { traces.add("Entered " + getDescription(inv)); }
   void traceExit(Invocation inv) { traces.add("Exited " + getDescription(inv)); }

   String getDescription(Invocation inv)
   {
      Member member = inv.getInvokedMember();
      String args = Arrays.toString(inv.getInvokedArguments());
      Object instance = inv.getInvokedInstance();
      return member.getDeclaringClass().getSimpleName() + '#' + member.getName() + " with " + args + " on " + instance;
   }

   @Test
   public void fakeEveryMethodInSingleClass()
   {
      new MockUp<TargetClass>() {
         @Mock
         Object $advice(Invocation inv)
         {
            traceEntry(inv);

            try {
               return inv.proceed();
            }
            finally {
               traceExit(inv);
            }
         }

         @Mock
         void validateSomething(Invocation inv)
         {
            Method m = inv.getInvokedMember();
            assertEquals("validateSomething", m.getName());
         }
      };

      TargetClass.staticMethod(123);
      final TargetClass tc0 = new TargetClass();
      assertEquals(4, tc0.doSomething("test", true));
      tc0.performAction(new Runnable() {
         @Override public void run() { tc0.doSomething("internal", false); }
         @Override public String toString() { return "action"; }
      });
      TargetClass tc1 = new TargetClass(1);
      tc1.performAction(null);
      tc1.validateSomething();

      List<String> expectedTraces = asList(
         "Entered TargetClass#staticMethod with [123] on null",
         "Exited TargetClass#staticMethod with [123] on null",
         "Entered TargetClass#doSomething with [test, true] on TargetClass0",
         "Exited TargetClass#doSomething with [test, true] on TargetClass0",
         "Entered TargetClass#performAction with [action] on TargetClass0",
         "Entered TargetClass#doSomething with [internal, false] on TargetClass0",
         "Exited TargetClass#doSomething with [internal, false] on TargetClass0",
         "Exited TargetClass#performAction with [action] on TargetClass0",
         "Entered TargetClass#performAction with [null] on TargetClass1",
         "Exited TargetClass#performAction with [null] on TargetClass1"
      );
      assertEquals(expectedTraces, traces);
   }

   @Test
   public void fakeEveryMethodInSingleClassWithAdviceOnly()
   {
      new MockUp<TargetClass>() {
         @Mock
         Object $advice(Invocation inv)
         {
            Integer i = inv.proceed();
            return i + 2;
         }
      };

      assertEquals(1, new TargetClass().doSomething("", false));
   }

   @Test
   public <B extends TargetClass> void fakeEveryMethodInClassHierarchy()
   {
      new MockUp<B>() {
         @Mock
         Object $advice(Invocation inv)
         {
            traceEntry(inv);

            try {
               return inv.proceed();
            }
            finally {
               traceExit(inv);
            }
         }
      };

      final TargetSubclass s1 = new TargetSubclass(1);
      assertEquals(4, s1.doSomething("test", true));
      assertEquals("123", s1.additionalMethod(123));
      s1.performAction(new Runnable() {
         @Override public void run() { assertSame(s1, this); }
         @Override public String toString() { return "sub-action"; }
      });

      TargetClass s2 = new TargetClass(2);
      s2.performAction(null);

      try {
         s2.validateSomething();
         fail();
      }
      catch (IllegalArgumentException e) {
         assertEquals("Invalid something", e.getMessage());
      }

      List<String> expectedTraces = asList(
         "Entered TargetClass#doSomething with [test, true] on TargetSubclass1",
         "Exited TargetClass#doSomething with [test, true] on TargetSubclass1",
         "Entered TargetSubclass#additionalMethod with [123] on TargetSubclass1",
         "Exited TargetSubclass#additionalMethod with [123] on TargetSubclass1",
         "Entered TargetSubclass#performAction with [sub-action] on TargetSubclass1",
         "Entered TargetSubclass#additionalMethod with [45] on TargetSubclass1",
         "Exited TargetSubclass#additionalMethod with [45] on TargetSubclass1",
         "Exited TargetSubclass#performAction with [sub-action] on TargetSubclass1",
         "Entered TargetClass#performAction with [null] on TargetClass2",
         "Exited TargetClass#performAction with [null] on TargetClass2",
         "Entered TargetClass#validateSomething with [] on TargetClass2",
         "Exited TargetClass#validateSomething with [] on TargetClass2"
      );
      assertEquals(expectedTraces, traces);
   }

   static final class XMLSourceTimingAspect<S extends Source> extends MockUp<S>
   {
      final Map<String, List<Long>> executionTimesMillis = new HashMap<String, List<Long>>();

      @Mock
      Object $advice(Invocation invocation)
      {
         long startTimeMillis = System.nanoTime() / 1000000;

         try {
            return invocation.proceed();
         }
         finally {
            long endTimeMillis = System.nanoTime() / 1000000;
            long dtMillis = endTimeMillis - startTimeMillis;
            Method invokedMethod = invocation.getInvokedMember();
            addMethodExecutionTime(invokedMethod, dtMillis);
         }
      }

      private void addMethodExecutionTime(Method invokedMethod, long executionTimeMillis)
      {
         String methodId = invokedMethod.getName();
         List<Long> methodTimesMillis = executionTimesMillis.get(methodId);

         if (methodTimesMillis == null) {
            methodTimesMillis = new ArrayList<Long>();
            executionTimesMillis.put(methodId, methodTimesMillis);
         }

         methodTimesMillis.add(executionTimeMillis);
      }

      void assertTimes(String methodId, int... expectedTimesMillisForConsecutiveExecutions)
      {
         List<Long> actualExecutionTimesMillis = executionTimesMillis.get(methodId);
         assertEquals(expectedTimesMillisForConsecutiveExecutions.length, actualExecutionTimesMillis.size());

         for (int i = 0; i < expectedTimesMillisForConsecutiveExecutions.length; i++) {
            long expectedTime = expectedTimesMillisForConsecutiveExecutions[i];
            long executionTime = actualExecutionTimesMillis.get(i);
            assertEquals("Expected time at index " + i, expectedTime, executionTime, 15);
         }
      }
   }

   static void takeSomeTime(int millis)
   {
      try { Thread.sleep(millis); } catch (InterruptedException ignore) {}
   }

   static class TestSource implements Source
   {
      @Override
      public void setSystemId(String systemId)
      {
         takeSomeTime(30);
      }

      @Override
      public String getSystemId()
      {
         takeSomeTime(20);
         return null;
      }
   }

   @Test
   public void fakeEveryMethodInAllClassesImplementingAnInterface() throws Exception
   {
      XMLSourceTimingAspect<?> timingAspect = new XMLSourceTimingAspect<Source>();

      Source src1 = new TestSource();
      src1.setSystemId("Abc");
      src1.getSystemId();

      Source src2 = new TestSource();
      src2.getSystemId();
      src2.setSystemId("Gh34");
      src2.getSystemId();

      // From the JRE, not mocked:
      new SAXSource().setSystemId("sax");
      new DOMSource().getSystemId();

      timingAspect.assertTimes("getSystemId", 20, 20, 20);
      timingAspect.assertTimes("setSystemId", 30, 30);
   }

   public static final class PublicFake extends MockUp<TargetClass>
   {
      @Mock
      public static Object $advice(Invocation inv)
      {
         Object[] args = inv.getInvokedArguments();

         if (args.length > 0) {
            Integer i = (Integer) args[0];
            return -i;
         }

         return null;
      }
   }

   @Test
   public void publicAdviceMethodInPublicFakeClass()
   {
      new PublicFake();

      new TargetClass().validateSomething();
      int i = TargetClass.staticMethod(123);

      assertEquals(-123, i);
   }
}

class TargetClass
{
   final int value;

   TargetClass() { value = 0; }
   TargetClass(int value) { this.value = value; }

   public static int staticMethod(int i) { return i; }
   int doSomething(String s, boolean b) { return b ? s.length() : -1; }
   protected void performAction(Runnable action) { if (action != null) action.run(); }
   protected void validateSomething() { throw new IllegalArgumentException("Invalid something"); }

   @Override
   public String toString() { return getClass().getSimpleName() + value; }
}

final class TargetSubclass extends TargetClass
{
   TargetSubclass(int value) { super(value); }

   String additionalMethod(int i) { return String.valueOf(i); }

   @Override
   protected void performAction(Runnable action)
   {
      additionalMethod(45);
      super.performAction(action);
   }
}