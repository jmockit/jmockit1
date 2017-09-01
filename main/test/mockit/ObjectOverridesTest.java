/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit;

import java.util.*;

import org.junit.*;
import static org.junit.Assert.*;

import mockit.internal.util.*;

@SuppressWarnings({
   "ObjectEqualsNull", "EqualsBetweenInconvertibleTypes", "FinalizeCalledExplicitly", "LiteralAsArgToStringEquals",
   "EqualsWithItself"})
public final class ObjectOverridesTest
{
   @Test
   public void verifyStandardBehaviorOfOverridableObjectMethodsInMockedInterface(
      @Mocked Runnable r1, @Mocked Runnable r2)
   {
      assertDefaultEqualsBehavior(r1, r2);
      assertDefaultEqualsBehavior(r2, r1);

      assertDefaultHashCodeBehavior(r1);
      assertDefaultHashCodeBehavior(r2);

      assertDefaultToStringBehavior(r1);
      assertDefaultToStringBehavior(r2);
   }

   void assertDefaultEqualsBehavior(Object obj1, Object obj2)
   {
      assertFalse(obj1.equals(null));
      assertFalse(obj1.equals("test"));
      assertTrue(obj1.equals(obj1));
      assertFalse(obj1.equals(obj2));
   }

   void assertDefaultHashCodeBehavior(Object obj) { assertEquals(System.identityHashCode(obj), obj.hashCode()); }
   void assertDefaultToStringBehavior(Object obj) { assertEquals(ObjectMethods.objectIdentity(obj), obj.toString()); }

   @Test
   public void verifyStandardBehaviorOfOverriddenObjectMethodsInMockedJREClass(@Mocked Date d1, @Mocked Date d2)
   {
      assertDefaultEqualsBehavior(d1, d2);
      assertDefaultEqualsBehavior(d2, d1);

      assertDefaultHashCodeBehavior(d1);
      assertDefaultHashCodeBehavior(d2);

      assertDefaultToStringBehavior(d1);
      assertDefaultToStringBehavior(d2);
   }

   @Mocked ClassWithObjectOverrides a;
   @Mocked ClassWithObjectOverrides b;

   @Before
   public void callObjectMethodsInMockBeforeEveryTest()
   {
      assertEquals(System.identityHashCode(a), a.hashCode());
      assertEquals(b, b);
   }

   @Test
   public void verifyStandardBehaviorOfOverriddenObjectMethodsInMockedClass() throws Throwable
   {
      assertDefaultEqualsBehavior(a, b);
      assertDefaultEqualsBehavior(b, a);

      assertDefaultHashCodeBehavior(a);
      assertDefaultHashCodeBehavior(b);

      assertDefaultToStringBehavior(a);
      assertDefaultToStringBehavior(b);

      a.finalize();
      b.finalize();
   }

   @Test
   public void mockOverrideOfEqualsMethod()
   {
      new Expectations() {{
         a.equals(null); result = true;
         a.equals(anyString); result = true;
      }};

      new Expectations() {{
         b.equals(a); result = true;
      }};

      assertTrue(a.equals(null));
      assertTrue(a.equals("test"));
      assertTrue(b.equals(a));
   }

   @Test
   public void mockOverrideOfHashCodeMethod()
   {
      assertTrue(a.hashCode() != b.hashCode());

      new Expectations() {{
         a.hashCode(); result = 123;
         b.hashCode(); result = 45; times = 1;
      }};

      assertEquals(123, a.hashCode());
      assertEquals(45, b.hashCode());
   }

   @Test
   public void mockOverrideOfToStringMethod()
   {
      assertFalse(a.toString().equals(b.toString()));

      new Expectations() {{
         a.toString(); result = "mocked";
      }};

      assertTrue("mocked".equals(a.toString()));

      new Verifications() {{
         a.toString();
         b.toString(); times = 0;
      }};
   }

   @Test
   public void mockOverrideOfCloneMethod()
   {
      new Expectations() {{
         a.clone(); result = b;
      }};

      assertSame(b, a.clone());
   }

   @Test
   public void recordExpectationsOnOverriddenObjectMethodAsAlwaysNonStrict()
   {
      new Expectations() {{
         a.doSomething();
         a.hashCode(); result = 1;
         a.equals(any);
         a.toString();
      }};

      a.doSomething();
   }

   @SuppressWarnings("EqualsWhichDoesntCheckParameterClass")
   static class ClassWithEqualsOverride
   {
      private final int value;
      ClassWithEqualsOverride(int value) { this.value = value; }
      @Override public boolean equals(Object other) { return ((ClassWithEqualsOverride) other).value == value; }
   }

   @Test
   public void mockClassWithEqualsOverrideWhoseInstanceGetsPassedInRecordedExpectation()
   {
      final Object o1 = new ClassWithEqualsOverride(123);
      Object o2 = new ClassWithEqualsOverride(123);

      new Expectations(ClassWithEqualsOverride.class) {{ a.doSomething(o1); }};

      a.doSomething(o2);
   }

   @Test
   public void mockJREClassWithEqualsOverrideWhoseInstanceGetsPassedInRecordedExpectation()
   {
      final Object o1 = new Date(123);
      Object o2 = new Date(123);

      new Expectations(Date.class) {{ a.doSomething(o1); }};

      a.doSomething(o2);
   }

   @Test
   public void callEqualsOnInstanceOfPartiallyMockedClassDuringVerification()
   {
      final ClassWithEqualsOverride instance1 = new ClassWithEqualsOverride(1);
      final ClassWithEqualsOverride instance2 = new ClassWithEqualsOverride(2);
      new Expectations(ClassWithEqualsOverride.class) {};

      new Verifications() {{ assertNotEquals(instance1, instance2); }};
   }
}
