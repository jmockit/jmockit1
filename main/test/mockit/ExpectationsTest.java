/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit;

import java.lang.reflect.*;

import org.junit.*;

import static org.junit.Assert.*;

import mockit.internal.*;

@SuppressWarnings({"unused", "deprecation"})
public final class ExpectationsTest
{
   @Deprecated
   public static class Dependency
   {
      @Deprecated int value;

      @Deprecated public Dependency() { value = -1; }

      @Ignore("test") public void setSomething(@Deprecated int value) {}

      public void setSomethingElse(String value) {}
      public int doSomething(Integer i, boolean b) { return i; }
      public int editABunchMoreStuff() { return 1; }
      public boolean notifyBeforeSave() { return true; }
      public void prepare() {}
      public void save() {}

      static int staticMethod(Object o, Exception e) { return -1; }
   }

   @Mocked Dependency mock;

   void exerciseCodeUnderTest()
   {
      mock.prepare();
      mock.setSomething(123);
      mock.setSomethingElse("anotherValue");
      mock.setSomething(45);
      mock.editABunchMoreStuff();
      mock.notifyBeforeSave();
      mock.save();
   }

   @Test
   public void recordSimpleInvocations()
   {
      new Expectations() {{
         mock.prepare();
         mock.editABunchMoreStuff();
         mock.setSomething(45);
      }};

      exerciseCodeUnderTest();
   }

   @Test
   public void recordInvocationThatWillNotOccur()
   {
      new Expectations() {{
         mock.editABunchMoreStuff(); result = 123; times = 0;
      }};

      mock.setSomething(123);
      mock.prepare();
   }

   @Test
   public void expectationsRecordedOnSameMethodWithSameMatchersButDifferentArguments()
   {
      new Expectations() {{
         mock.doSomething(1, anyBoolean); result = 1;
         mock.doSomething(2, anyBoolean); result = 2;
      }};

      assertEquals(1, mock.doSomething(1, true));
      assertEquals(2, mock.doSomething(2, false));
      assertEquals(0, mock.doSomething(3, false));
   }

   @Test
   public void expectationsRecordedOnSameMethodWithMatcherInOneAndFixedArgumentInAnother()
   {
      new Expectations() {{
         mock.doSomething(1, anyBoolean); result = 1;
         mock.doSomething(anyInt, anyBoolean); result = 2;
      }};

      assertEquals(1, mock.doSomething(1, true));
      assertEquals(2, mock.doSomething(null, false));
      assertEquals(2, mock.doSomething(2, true));
      assertEquals(1, mock.doSomething(1, false));
   }

   @Test(expected = MissingInvocation.class)
   public void recordInvocationWithExactExpectedNumberOfInvocationsButFailToSatisfy()
   {
      new Expectations() {{
         mock.editABunchMoreStuff(); times = 1;
      }};
   }

   @Test(expected = MissingInvocation.class)
   public void recordInvocationWithMinimumExpectedNumberOfInvocationsButFailToSatisfy()
   {
      new Expectations() {{
         mock.editABunchMoreStuff(); minTimes = 2;
      }};

      mock.editABunchMoreStuff();
   }

   @Test(expected = UnexpectedInvocation.class)
   public void recordInvocationWithMaximumExpectedNumberOfInvocationsButFailToSatisfy()
   {
      new Expectations() {{
         mock.editABunchMoreStuff(); maxTimes = 1;
      }};

      mock.editABunchMoreStuff();
      mock.editABunchMoreStuff();
   }

   @Test
   public void recordInvocationsWithExpectedInvocationCounts()
   {
      new Expectations() {{
         mock.setSomethingElse(anyString); minTimes = 1;
         mock.save(); times = 2;
      }};

      mock.setSomething(3);
      mock.save();
      mock.setSomethingElse("test");
      mock.save();
   }

   @Test(expected = MissingInvocation.class)
   public void recordInvocationsWithMinInvocationCountLargerThanWillOccur()
   {
      new Expectations() {{
         mock.save(); minTimes = 2;
      }};

      mock.save();
   }

   @Test
   public void recordWithArgumentMatcherAndIndividualInvocationCounts()
   {
      new Expectations() {{
         mock.prepare(); maxTimes = 1;
         mock.setSomething(anyInt); minTimes = 2;
         mock.editABunchMoreStuff(); maxTimes = 5;
         mock.save(); times = 1;
      }};

      exerciseCodeUnderTest();
   }

   @Test
   public void recordWithMaxInvocationCountFollowedByReturnValue()
   {
      new Expectations() {{
         Dependency.staticMethod(any, null);
         maxTimes = 1;
         result = 1;
      }};

      assertEquals(1, Dependency.staticMethod(new Object(), new Exception()));
   }

   @Test(expected = UnexpectedInvocation.class)
   public void recordWithMaxInvocationCountFollowedByReturnValueButReplayOneTimeBeyondMax()
   {
      new Expectations() {{
         Dependency.staticMethod(any, null);
         maxTimes = 1;
         result = 1;
      }};

      Dependency.staticMethod(null, null);
      Dependency.staticMethod(null, null);
   }

   @Test
   public void recordWithReturnValueFollowedByExpectedInvocationCount()
   {
      new Expectations() {{
         Dependency.staticMethod(any, null);
         result = 1;
         times = 1;
      }};

      assertEquals(1, Dependency.staticMethod(null, null));
   }

   @Test
   public void recordWithMinInvocationCountFollowedByReturnValueUsingDelegate()
   {
      new Expectations() {{
         Dependency.staticMethod(any, null);
         minTimes = 1;
         result = new Delegate() {
            int staticMethod(Object o, Exception e) { return 1; }
         };
      }};

      assertEquals(1, Dependency.staticMethod(null, null));
   }

   @Test
   public void mockedClassWithAnnotatedElements() throws Exception
   {
      Class<?> mockedClass = mock.getClass();
      assertTrue(mockedClass.isAnnotationPresent(Deprecated.class));
      assertTrue(mockedClass.getDeclaredField("value").isAnnotationPresent(Deprecated.class));
      assertTrue(mockedClass.getDeclaredConstructor().isAnnotationPresent(Deprecated.class));

      Method mockedMethod = mockedClass.getDeclaredMethod("setSomething", int.class);
      Ignore ignore = mockedMethod.getAnnotation(Ignore.class);
      assertNotNull(ignore);
      assertEquals("test", ignore.value());
      assertTrue(mockedMethod.getParameterAnnotations()[0][0] instanceof Deprecated);
   }
}
