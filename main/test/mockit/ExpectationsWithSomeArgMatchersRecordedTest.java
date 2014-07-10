/*
 * Copyright (c) 2006-2014 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit;

import java.util.*;
import static java.util.Arrays.*;

import org.junit.*;
import static org.junit.Assert.*;

import mockit.internal.*;

public final class ExpectationsWithSomeArgMatchersRecordedTest
{
   static class Dependency
   {
      private final int value;

      Dependency() { value = 0; }
      Dependency(int value) { this.value = value; }

      @Override
      public boolean equals(Object obj) { return obj instanceof Dependency && value == ((Dependency) obj).value; }

      @Override
      public int hashCode() { return value; }
   }

   static class Collaborator
   {
      void setValue(int value) {}
      void setValue(double value) {}
      void setValue(float value) {}
      String setValue(String value) { return ""; }

      void setValues(long value1, byte value2, double value3, short value4) {}
      boolean booleanValues(long value1, byte value2, double value3, short value4) { return true; }

      @SuppressWarnings("unused")
      static void staticSetValues(long value1, byte value2, double value3, short value4) {}

      @SuppressWarnings("unused")
      static long staticLongValues(long value1, byte value2, double value3, short value4) { return -2; }

      int doSomething(Dependency src) { return -1; }
      int doSomething(Dependency src, String s) { return -1; }
      int doSomething(Dependency src, String... s) { return -1; }

      final void simpleOperation(int a, String b) {}
      final void simpleOperation(int a, String b, Date c) {}
      long anotherOperation(byte b, Long l) { return -1; }

      @SuppressWarnings("unused")
      static void staticVoidMethod(long l, char c, double d) {}

      @SuppressWarnings("unused")
      static boolean staticBooleanMethod(boolean b, String s, int[] array) { return false; }

      void methodWithArrayParameters(char[][] c, String[] s, Object[][][] matrix) {}

      void methodWithManyParameters(
         byte b1, short s1, int i1, long l1, String str1, boolean bo1, float f1, double d1, int[] ii1, String[] ss1,
         byte b2, short s2, int i2, long l2, String str2, boolean bo2, float f2, double d2, int[] ii2, String[] ss2,
         char c) {}
   }

   @Mocked Collaborator mock;

   @Test
   public void useMatcherOnlyForOneArgument()
   {
      final Object o = new Object();

      new Expectations() {{
         mock.simpleOperation(withEqual(1), "", null);
         mock.simpleOperation(withNotEqual(1), null, (Date) withNull());
         mock.simpleOperation(1, withNotEqual("arg"), null); minTimes = 1; maxTimes = 2;
         mock.simpleOperation(12, "arg", (Date) withNotNull());

         mock.anotherOperation((byte) 0, anyLong); result = 123L;
         mock.anotherOperation(anyByte, 5L); result = -123L;

         Collaborator.staticVoidMethod(34L, anyChar, 5.0);
         Collaborator.staticBooleanMethod(true, withSuffix("end"), null); result = true;
         Collaborator.staticBooleanMethod(true, "", new int[] {1, 2, 3}); result = true;

         char[][] chars = {{'a', 'b'}, {'X', 'Y', 'Z'}};
         Object[][][] matrix = {null, {{1, 'X', "test"}}, {{o}}};
         mock.methodWithArrayParameters(chars, (String[]) any, matrix);
      }};

      mock.simpleOperation(1, "", null);
      mock.simpleOperation(2, "str", null);
      mock.simpleOperation(1, "", null);
      mock.simpleOperation(12, "arg", new Date());

      assertEquals(123L, mock.anotherOperation((byte) 0, 5L));
      assertEquals(-123L, mock.anotherOperation((byte) 3, 5L));

      Collaborator.staticVoidMethod(34L, '8', 5.0);
      assertTrue(Collaborator.staticBooleanMethod(true, "start-end", null));
      assertTrue(Collaborator.staticBooleanMethod(true, "", new int[] {1, 2, 3}));

      mock.methodWithArrayParameters(
         new char[][] {{'a', 'b'}, {'X', 'Y', 'Z'}}, null, new Object[][][] {null, {{1, 'X', "test"}}, {{o}}});
   }

   @Test(expected = UnexpectedInvocation.class)
   public void useMatcherOnlyForFirstArgumentWithUnexpectedReplayValue()
   {
      new Expectations() {{
         mock.simpleOperation(withEqual(1), "", null);
      }};

      mock.simpleOperation(2, "", null);
   }

   @Test(expected = UnexpectedInvocation.class)
   public void useMatcherOnlyForSecondArgumentWithUnexpectedReplayValue()
   {
      new Expectations() {{
         mock.simpleOperation(1, withPrefix("arg"), null);
      }};

      mock.simpleOperation(1, "Xyz", null);
   }

   @Test(expected = UnexpectedInvocation.class)
   public void useMatcherOnlyForLastArgumentWithUnexpectedReplayValue()
   {
      new Expectations() {{
         mock.simpleOperation(12, "arg", (Date) withNotNull());
      }};

      mock.simpleOperation(12, "arg", null);
   }

   @Test
   public void useMatchersForParametersOfAllSizes()
   {
      new NonStrictExpectations() {{
         mock.setValues(123L, withEqual((byte) 5), 6.4, withNotEqual((short) 14));
         mock.booleanValues(12L, (byte) 4, withEqual(6.0, 0.1), withEqual((short) 14));
         Collaborator.staticSetValues(withNotEqual(1L), (byte) 4, 6.1, withEqual((short) 3));
         Collaborator.staticLongValues(12L, anyByte, withEqual(6.1), (short) 4);
      }};

      mock.setValues(123L, (byte) 5, 6.4, (short) 41);
      assertFalse(mock.booleanValues(12L, (byte) 4, 6.1, (short) 14));
      Collaborator.staticSetValues(2L, (byte) 4, 6.1, (short) 3);
      assertEquals(0L, Collaborator.staticLongValues(12L, (byte) -7, 6.1, (short) 4));
   }

   @Test
   public void useAnyIntField()
   {
      new Expectations() {{ mock.setValue(anyInt); }};

      mock.setValue(1);
   }

   @Test
   public void useAnyStringField()
   {
      new NonStrictExpectations() {{
         mock.setValue(anyString); returns("one", "two");
      }};

      assertEquals("one", mock.setValue("test"));
      assertEquals("two", mock.setValue(""));
      assertEquals("two", mock.setValue(null));
   }

   @Test
   public void useSeveralAnyFields()
   {
      final Date now = new Date();

      new Expectations() {{
         mock.simpleOperation(anyInt, null, null);
         mock.simpleOperation(anyInt, "test", null);
         mock.simpleOperation(3, "test2", null);
         mock.simpleOperation(-1, null, (Date) any);
         mock.simpleOperation(1, anyString, now);

         Collaborator.staticSetValues(2L, anyByte, 0.0, anyShort);

         mock.methodWithManyParameters(
            anyByte, anyShort, anyInt, anyLong, anyString, anyBoolean, anyFloat, anyDouble, (int[]) any, (String[]) any,
            anyByte, anyShort, anyInt, anyLong, anyString, anyBoolean, anyFloat, anyDouble, (int[]) any, (String[]) any,
            anyChar);
      }};

      mock.simpleOperation(2, "abc", now);
      mock.simpleOperation(5, "test", null);
      mock.simpleOperation(3, "test2", null);
      mock.simpleOperation(-1, "Xyz", now);
      mock.simpleOperation(1, "", now);

      Collaborator.staticSetValues(2, (byte) 1, 0, (short) 2);

      mock.methodWithManyParameters(
         (byte) 1, (short) 2, 3, 4L, "5", false, 7.0F, 8.0, null, null,
         (byte) 10, (short) 20, 30, 40L, "50", true, 70.0F, 80.0, null, null, 'x');
   }

   @Test
   public void useWithMethodsMixedWithAnyFields()
   {
      new Expectations() {{
         mock.simpleOperation(anyInt, null, (Date) any);
         mock.simpleOperation(anyInt, withEqual("test"), null);
         mock.simpleOperation(3, withPrefix("test"), (Date) any);
         mock.simpleOperation(-1, anyString, (Date) any);
         mock.simpleOperation(1, anyString, (Date) withNotNull());
      }};

      mock.simpleOperation(2, "abc", new Date());
      mock.simpleOperation(5, "test", null);
      mock.simpleOperation(3, "test2", null);
      mock.simpleOperation(-1, "Xyz", new Date());
      mock.simpleOperation(1, "", new Date());
   }

   public interface Scheduler
   {
      List<String> getAlerts(Object o, int i, boolean b);
   }

   @Test
   public void useMatchersInInvocationsToInterfaceMethods(@Mocked final Scheduler scheduler)
   {
      new NonStrictExpectations() {{
         scheduler.getAlerts(any, 1, anyBoolean); result = asList("A", "b");
      }};

      assertEquals(2, scheduler.getAlerts("123", 1, true).size());
   }

   // Tests for the matching of expectations to instances created during replay ///////////////////////////////////////

   @Test
   public void recordStrictExpectationWithMatcherForMockedObjectInstantiatedInsideSUT(@Mocked Dependency dep)
   {
      new Expectations() {{
         Dependency src = new Dependency();
         mock.doSomething(withEqual(src));
      }};

      Dependency src = new Dependency();
      mock.doSomething(src);
   }

   @Test
   public void recordNonStrictExpectationsForMockedObjectsInstantiatedInsideSUT(@Mocked Dependency dep)
   {
      new NonStrictExpectations() {{
         Dependency src1 = new Dependency(1);
         Dependency src2 = new Dependency(2);
         mock.doSomething(src1); result = 1; times = 2;
         mock.doSomething(src2); result = 2; times = 2;
      }};

      Dependency src1 = new Dependency(1);
      Dependency src2 = new Dependency(2);
      Dependency src3 = new Dependency(1);
      Dependency src4 = new Dependency(2);

      assertEquals(1, mock.doSomething(src1));
      assertEquals(2, mock.doSomething(src2));
      assertEquals(1, mock.doSomething(src3));
      assertEquals(2, mock.doSomething(src4));
   }

   @Test
   public void verifyUnorderedExpectationsForMockedObjectsInstantiatedInsideSUT(@Mocked Dependency dep)
   {
      Dependency src1 = new Dependency(1);
      Dependency src2 = new Dependency(2);
      Dependency src3 = new Dependency(1);
      Dependency src4 = new Dependency(2);

      mock.doSomething(src1);
      mock.doSomething(src2);
      mock.doSomething(src3);
      mock.doSomething(src4);

      new Verifications() {{
         Dependency dep2 = new Dependency(2);
         mock.doSomething(dep2); times = 2;

         Dependency dep1 = new Dependency(1);
         mock.doSomething(dep1); times = 2;
      }};
   }

   @Test
   public void verifyOrderedExpectationsForMockedObjectsInstantiatedInsideSUT(@Mocked Dependency dep)
   {
      Dependency src1 = new Dependency(1);
      Dependency src2 = new Dependency(2);
      Dependency src3 = new Dependency(2);
      Dependency src4 = new Dependency(1);

      mock.doSomething(src1);
      mock.doSomething(src2);
      mock.doSomething(src3);
      mock.doSomething(src4);

      new VerificationsInOrder() {{
         Dependency dep1 = new Dependency(1);
         Dependency dep2 = new Dependency(2); times = 2;
         Dependency dep3 = new Dependency(1);
         mock.doSomething(dep1);
         mock.doSomething(dep2); times = 2;
         mock.doSomething(dep3);
      }};
   }

   @Test
   public void recordStrictExpectationWithMatcherAndRegularArgumentMatchingMockedObjectInstantiatedInsideSUT(
      @Mocked Dependency dep)
   {
      new Expectations() {{
         Dependency src = new Dependency();
         mock.doSomething(src, anyString);
      }};

      Dependency src = new Dependency();
      mock.doSomething(src, "test");
   }

   @Test
   public void recordNonStrictExpectationWithMatcherAndRegularArgumentMatchingMockedObjectInstantiatedInsideSUT(
      @Mocked Dependency dep)
   {
      final List<Dependency> dependencies = new ArrayList<Dependency>();

      new Expectations() {{
         Dependency src = new Dependency();
         dependencies.add(src);
      }};

      new NonStrictExpectations() {{
         Dependency firstDep = dependencies.get(0);
         mock.doSomething(firstDep, anyString);
         result = 123;
      }};

      Dependency src = new Dependency();
      int i = mock.doSomething(src, "test");

      assertEquals(123, i);
   }

   @Test
   public void recordStrictVarargsExpectationWithMatcherAndRegularArgumentMatchingMockedObjectInstantiatedInsideSUT(
      @Mocked Dependency dep)
   {
      new Expectations() {{
         Dependency src = new Dependency();
         mock.doSomething(src, (String[]) any);
      }};

      Dependency src = new Dependency();
      mock.doSomething(src, "a", "b");
   }

   @Test
   public void recordNonStrictVarargsExpectationWithMatcherAndRegularArgumentMatchingMockedObjectInstantiatedInsideSUT(
      @Mocked Dependency dep)
   {
      final List<Dependency> dependencies = new ArrayList<Dependency>();

      new Expectations() {{
         Dependency src = new Dependency();
         dependencies.add(src);
      }};

      new NonStrictExpectations() {{
         Dependency firstDep = dependencies.get(0);
         mock.doSomething(firstDep, (String[]) any);
         result = 123;
      }};

      Dependency src = new Dependency();
      int i = mock.doSomething(src, "a", "b");

      assertEquals(123, i);
   }

   @Test
   public void recordStrictExpectationWithReplacementInstanceForMockedObjectInstantiatedInsideSUT(
      @Mocked final Dependency dep)
   {
      new Expectations() {{
         new Dependency(); result = dep;
         mock.doSomething(dep);
      }};

      Dependency src = new Dependency();
      mock.doSomething(src);
   }

   @Test
   public void recordNonStrictExpectationsWithReplacementInstancesForMockedObjectsInstantiatedInsideSUT(
      @Mocked final Dependency dep1, @Mocked final Dependency dep2)
   {
      new NonStrictExpectations() {{
         new Dependency(1); result = dep1;
         new Dependency(2); result = dep2;
         mock.doSomething(dep1); result = 1; times = 2;
         mock.doSomething(dep2); result = 2; times = 2;
      }};

      Dependency src1 = new Dependency(1);
      Dependency src2 = new Dependency(2);
      Dependency src3 = new Dependency(1);
      Dependency src4 = new Dependency(2);

      assertEquals(1, mock.doSomething(src1));
      assertEquals(2, mock.doSomething(src2));
      assertEquals(1, mock.doSomething(src3));
      assertEquals(2, mock.doSomething(src4));
   }

   // The following tests failed only when compiled with the Eclipse compiler /////////////////////////////////////////

   @Test
   public void expectationWithMatchersSpanningMultipleLines()
   {
      new Expectations() {{
         mock.simpleOperation(1,
            (String) withNull());
      }};

      mock.simpleOperation(1, null);
   }

   @Test
   public void expectationWithMatcherInSecondLineAndConstantArgumentInThirdLine()
   {
      new Expectations() {{
         mock.simpleOperation(
            anyInt,
            "test");
      }};

      mock.simpleOperation(123, "test");
   }

   @Test
   public void expectationsWithPartialMatchersInEveryCombinationForMethodWithThreeParameters()
   {
      final Date now = new Date();

      new Expectations() {{
         // Expectations with one matcher:
         mock.simpleOperation(
            anyInt,
            "test", null);
         mock.simpleOperation(-2, anyString,
            null);
         mock.simpleOperation(
            0,
            "test", (Date) withNotNull());
         mock.simpleOperation(
            1,
            null,
            (Date) withNull());
         mock.simpleOperation(
            0, "test",
            (Date) any);

         // Expectations with two matchers:
         mock.simpleOperation(-3, anyString,
            (Date) any);
         mock.simpleOperation(
            withNotEqual(0), anyString,
            now);
         mock.simpleOperation(anyInt,
            "",
            (Date) any);
      }};

      mock.simpleOperation(123, "test", null);
      mock.simpleOperation(-2, "", now);
      mock.simpleOperation(0, "test", now);
      mock.simpleOperation(1, "test", null);
      mock.simpleOperation(0, "test", null);
      mock.simpleOperation(-3, "xyz", now);
      mock.simpleOperation(123, null, now);
      mock.simpleOperation(123, "", null);
   }
}