/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit;

import java.awt.*;
import java.io.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;

import org.junit.*;
import static org.junit.Assert.*;

public final class MockUpForSingleClassInstanceTest
{
   public static class AClass
   {
      final int numericValue;
      final String textValue;

      AClass(int n) { this(n, null); }

      AClass(int n, String s)
      {
         numericValue = n;
         textValue = s;
      }

      public final int getNumericValue() { return numericValue; }
      public String getTextValue() { return textValue; }
      protected final int getSomeOtherValue() { return 0; }
      public static boolean doSomething() { return false; }
   }

   @Test
   public void multipleMockupsOfSameTypeWithOwnMockInstanceEach()
   {
      final class AClassMockUp extends MockUp<AClass>
      {
         private final int number;
         private final String text;

         AClassMockUp(int number, String text)
         {
            this.number = number;
            this.text = text;
         }

         @Mock int getNumericValue() { return number; }
         @Mock String getTextValue() { return text; }
      }

      MockUp<AClass> mockUp1 = new AClassMockUp(1, "one");
      AClass mock1 = mockUp1.getMockInstance();

      AClassMockUp mockUp2 = new AClassMockUp(2, "two");
      AClass mock2 = mockUp2.getMockInstance();

      assertNotSame(mock1, mock2);
      assertEquals(1, mock1.getNumericValue());
      assertEquals("one", mock1.getTextValue());
      assertEquals(0, mock1.getSomeOtherValue());
      assertEquals(2, mock2.getNumericValue());
      assertEquals("two", mock2.getTextValue());
      assertEquals(0, mock2.getSomeOtherValue());
      assertEquals("two", mock2.getTextValue());
   }

   public static class AClassMockUp extends MockUp<BasicStroke>
   {
      private final int value;
      AClassMockUp(int value) { this.value = value; }

      @Mock public float getLineWidth() { return value; }
   }

   @Test
   public void samePublicMockupAppliedMultipleTimes()
   {
      BasicStroke mock1 = new AClassMockUp(1).getMockInstance();
      BasicStroke mock2 = new AClassMockUp(2).getMockInstance();

      assertNotSame(mock1, mock2);
      assertEquals(1, mock1.getLineWidth(), 0);
      assertEquals(2, mock2.getLineWidth(), 0);
   }

   @Test
   public void sameAnonymousMockupAppliedMultipleTimesWithDifferentTargetInstances()
   {
      List<BasicStroke> targetInstances = new ArrayList<BasicStroke>();

      for (int i = 1; i <= 2; i++) {
         final int width = 100 * i;
         BasicStroke targetInstance = new BasicStroke(i);
         new MockUp<BasicStroke>(targetInstance) {
            @Mock float getLineWidth() { return width; }
         };
         targetInstances.add(targetInstance);
      }

      assertEquals(100, targetInstances.get(0).getLineWidth(), 0);
      assertEquals(200, targetInstances.get(1).getLineWidth(), 0);
   }

   @Test
   public void sameAnonymousMockupAppliedMultipleTimesWithoutTargetInstanceButWithMockInstanceCreatedFromMockup()
   {
      List<BasicStroke> mockInstances = new ArrayList<BasicStroke>();

      for (int i = 1; i <= 2; i++) {
         final int width = 100 * i;
         BasicStroke mockInstance = new MockUp<BasicStroke>() {
            @Mock float getLineWidth() { return width; }
         }.getMockInstance();
         mockInstances.add(mockInstance);
      }

      assertEquals(100, mockInstances.get(0).getLineWidth(), 0);
      assertEquals(200, mockInstances.get(1).getLineWidth(), 0);
   }

   @Test
   public void getMockInstanceFromInsideMockMethodForNonStaticMockedMethod()
   {
      new MockUp<AClass>() {
         @Mock
         String getTextValue()
         {
            assertNotNull(getMockInstance());
            return "mock";
         }
      };

      assertEquals("mock", new AClass(123).getTextValue());
   }

   @Test(expected = IllegalStateException.class)
   public void attemptToGetMockInstanceFromInsideMockMethodForStaticMockedMethod()
   {
      new MockUp<AClass>() {
         @Mock
         boolean doSomething()
         {
            getMockInstance(); // not valid because "doSomething" is static
            fail("Must not get here");
            return true;
         }
      };

      AClass.doSomething();
   }

   @Test
   public void mockupAffectingOneInstanceButNotOthersOfSameClass()
   {
      AClass instance1 = new AClass(1);
      AClass instance2 = new AClass(2);

      AClass mockInstance = new MockUp<AClass>(instance1) {
         @Mock int getNumericValue() { return 3; }
      }.getMockInstance();

      assertSame(instance1, mockInstance);
      assertEquals(3, instance1.getNumericValue());
      assertEquals(2, instance2.getNumericValue());
      assertEquals(1, new AClass(1).getNumericValue());
   }

   @Test
   public void accessCurrentMockedInstanceFromInsideMockMethodForAnyInstanceOfTheMockedClass()
   {
      AClass instance1 = new AClass(1);
      AClass instance2 = new AClass(2, "test2");

      MockUp<AClass> mockUp = new MockUp<AClass>() {
         @Mock
         String getTextValue()
         {
            AClass mockedInstance = getMockInstance();
            return "mocked: " + mockedInstance.textValue;
         }
      };

      AClass instance3 = new AClass(3, "test3");
      assertEquals("mocked: null", instance1.getTextValue());
      assertEquals("mocked: test2", instance2.getTextValue());
      assertEquals("mocked: test3", instance3.getTextValue());
      assertSame(instance3, mockUp.getMockInstance());
   }

   @Test
   public void accessCurrentMockedInstanceFromInsideMockMethodForSingleMockedInstance()
   {
      AClass unmockedInstance1 = new AClass(1, "test1");
      final int i = 123;

      MockUp<AClass> mockUp = new MockUp<AClass>() {
         final int numericValue = i;

         @Mock
         String getTextValue()
         {
            AClass mockedInstance = getMockInstance();
            return "mocked: " + mockedInstance.textValue;
         }

         @Mock
         int getNumericValue() { return numericValue; }
      };
      AClass onlyInstanceToBeMocked = mockUp.getMockInstance();

      assertEquals("test1", unmockedInstance1.getTextValue());
      AClass unmockedInstance2 = new AClass(2, "test2");
      assertEquals("mocked: null", onlyInstanceToBeMocked.getTextValue());
      assertEquals("test2", unmockedInstance2.getTextValue());
      assertSame(onlyInstanceToBeMocked, mockUp.getMockInstance());
   }

   static final class ASubClass extends AClass
   {
      ASubClass(int n, String s) { super(n, s); }
      @Override public String getTextValue() { return "subtext"; }
   }

   @Test
   public void applyMockupWithGivenSubclassInstance()
   {
      AClass realInstance = new ASubClass(123, "test");

      MockUp<AClass> mockUp = new MockUp<AClass>(realInstance) {
         @Mock String getTextValue() { return "mock"; }
         @Mock int getSomeOtherValue() { return 45; }
      };

      AClass mockInstance = mockUp.getMockInstance();
      assertSame(realInstance, mockInstance);

      assertEquals(123, realInstance.getNumericValue());
      assertEquals("mock", mockInstance.getTextValue());
      assertEquals(45, mockInstance.getSomeOtherValue());
   }

   public abstract static class AbstractBase implements Runnable
   {
      protected abstract String getValue();
      public abstract void doSomething(int i);
      public boolean doSomethingElse() { return true; }
   }

   @Test
   public void getMockInstanceFromMockupForAbstractClass()
   {
      MockUp<AbstractBase> mockUp = new MockUp<AbstractBase>() {
         @Mock
         String getValue()
         {
            AbstractBase mockInstance = getMockInstance();
            assertNotNull(mockInstance);
            return "test";
         }

         @Mock
         boolean doSomethingElse() { return false; }
      };

      AbstractBase mock = mockUp.getMockInstance();

      assertEquals("test", mock.getValue());
      mock.doSomething(123);
      mock.run();
      assertFalse(mock.doSomethingElse());
      assertSame(mock, mockUp.getMockInstance());
   }

   public abstract static class GenericAbstractBase<T, N extends Number> implements Callable<N>
   {
      protected abstract int doSomething(String s, T value);
   }

   @Test
   public void getMockInstanceFromMockupForGenericAbstractClass() throws Exception
   {
      GenericAbstractBase<Boolean, Long> mock = new MockUp<GenericAbstractBase<Boolean, Long>>() {
         @Mock
         Long call()
         {
            GenericAbstractBase<Boolean, Long> mockInstance = getMockInstance();
            assertNotNull(mockInstance);
            return 123L;
         }

         @Mock
         int doSomething(String s, Boolean value) { return value ? s.length() : 1; }
      }.getMockInstance();

      assertEquals(123L, mock.call().longValue());
      assertEquals(5, mock.doSomething("test1", true));
      assertEquals(1, mock.doSomething("test2", false));
   }

   @Test
   public void getMockInstanceFromMockupForAbstractJREClass() throws Exception
   {
      MockUp<Reader> mockUp = new MockUp<Reader>() {
         @Mock
         int read(char[] cbuf, int off, int len)
         {
            Reader mockInstance = getMockInstance();
            assertNotNull(mockInstance);
            return 123;
         }

         @Mock
         boolean ready() { return true; }
      };

      Reader mock = mockUp.getMockInstance();

      assertEquals(123, mock.read(new char[0], 0, 0));
      mock.close();
      assertTrue(mock.ready());
      assertSame(mock, mockUp.getMockInstance());
   }
}
