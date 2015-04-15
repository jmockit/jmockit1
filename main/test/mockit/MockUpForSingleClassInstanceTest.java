/*
 * Copyright (c) 2006-2015 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit;

import java.io.*;
import java.util.concurrent.*;

import org.junit.*;
import static org.junit.Assert.*;

public final class MockUpForSingleClassInstanceTest
{
   static class AClass
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
      String getTextValue() { return textValue; }
      private int getSomeOtherValue() { return 0; }
      static boolean doSomething() { return false; }
   }

   @Test
   public void multipleMockUpsOfSameTypeWithOwnMockInstanceEach()
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

   @Test
   public void multipleMockUpsOfSameTypeHavingInvocationConstraints()
   {
      AClass[] mockInstances = new AClass[2];

      for (int i = 0; i < 2; i++) {
         final int value = i + 1;
         mockInstances[i] = new MockUp<AClass>() {
            @Mock(invocations = 1) String getTextValue() { return String.valueOf(value); }
         }.getMockInstance();
      }

      assertEquals("1", mockInstances[0].getTextValue());
      assertEquals("2", mockInstances[1].getTextValue());
   }

   public static class AClassMockUp extends MockUp<AClass>
   {
      private final String value;
      AClassMockUp(String value) { this.value = value; }

      @Mock(maxInvocations = 1) public String getTextValue() { return value; }
      @Mock public static boolean doSomething() { return true; }
   }

   @Test
   public void multiplePublicMockUps()
   {
      AClass mock1 = new AClassMockUp("Abc").getMockInstance();
      AClass mock2 = new AClassMockUp("Xpto").getMockInstance();

      assertNotSame(mock1, mock2);
      assertEquals("Abc", mock1.getTextValue());
      assertEquals("Xpto", mock2.getTextValue());
      assertTrue(AClass.doSomething());
   }

   @Test
   public void getMockInstanceFromInsideMockMethodForStaticMockedMethod()
   {
      new MockUp<AClass>() {
         @Mock
         boolean doSomething()
         {
            assertNull(getMockInstance());
            return true;
         }
      };

      assertTrue(AClass.doSomething());
   }

   @Test
   public void mockUpAffectingOneInstanceButNotOthersOfSameClass()
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

      MockUp<AClass> mockUp = new MockUp<AClass>() {
         @Mock
         String getTextValue()
         {
            AClass mockedInstance = getMockInstance();
            return "mocked: " + mockedInstance.textValue;
         }
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
      @Override String getTextValue() { return "subtext"; }
   }

   @Test
   public void applyMockUpWithGivenSubclassInstance()
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
   public void getMockInstanceFromMockUpForAbstractClass()
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
   public void getMockInstanceFromMockUpForGenericAbstractClass() throws Exception
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
   public void getMockInstanceFromMockUpForAbstractJREClass() throws Exception
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

   static class ClassWithHashCode
   {
      private final Object value = "test";
      @Override public int hashCode() { return value.hashCode(); }
   }

   @Test // ensures the hashCode method never gets called
   public void getUninitializedMockInstanceForClassWithHashCodeMethodWhichNeedsInitializedState()
   {
      ClassWithHashCode uninitializedMock = new MockUp<ClassWithHashCode>() {}.getMockInstance();

      assertNotNull(uninitializedMock);
   }
}
