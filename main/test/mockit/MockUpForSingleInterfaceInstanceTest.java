/*
 * Copyright (c) 2006-2014 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit;

import java.util.*;

import org.junit.*;
import static org.junit.Assert.*;

public final class MockUpForSingleInterfaceInstanceTest
{
   public interface APublicInterface
   {
      int getNumericValue();
      String getTextValue();
      int getSomeOtherValue();
   }

   @Test
   public void multipleMockUpInstancesForAPublicInterfaceWithASingleMockInstanceEach()
   {
      final class AnInterfaceMockUp extends MockUp<APublicInterface>
      {
         private final int number;
         private final String text;

         AnInterfaceMockUp(int number, String text)
         {
            this.number = number;
            this.text = text;
         }

         @Mock(minInvocations = 1) int getNumericValue() { return number; }
         @Mock(maxInvocations = 2) String getTextValue() { return text; }
      }

      MockUp<APublicInterface> mockUp1 = new AnInterfaceMockUp(1, "one");
      APublicInterface mock1 = mockUp1.getMockInstance();

      AnInterfaceMockUp mockUp2 = new AnInterfaceMockUp(2, "two");
      APublicInterface mock2 = mockUp2.getMockInstance();

      assertNotSame(mock1, mock2);
      assertSame(mock1.getClass(), mock2.getClass());
      assertEquals(1, mock1.getNumericValue());
      assertEquals("one", mock1.getTextValue());
      assertEquals(0, mock1.getSomeOtherValue());
      assertEquals(2, mock2.getNumericValue());
      assertEquals("two", mock2.getTextValue());
      assertEquals(0, mock2.getSomeOtherValue());
   }

   @Test
   public void multipleMockUpInstancesForPublicInterfacePassingInterfaceToMockUpConstructor()
   {
      final class AnInterfaceMockUp extends MockUp<APublicInterface> {
         private final int number;
         AnInterfaceMockUp(int number) { super(APublicInterface.class); this.number = number; }
         @Mock int getNumericValue() { return number; }
      }

      MockUp<APublicInterface> mockUp1 = new AnInterfaceMockUp(1);
      APublicInterface mock1 = mockUp1.getMockInstance();

      AnInterfaceMockUp mockUp2 = new AnInterfaceMockUp(2);
      APublicInterface mock2 = mockUp2.getMockInstance();

      assertNotSame(mock1, mock2);
      assertSame(mock1.getClass(), mock2.getClass());
      assertEquals(1, mock1.getNumericValue());
      assertEquals(2, mock2.getNumericValue());
   }

   @Test(timeout = 500)
   public void instantiateSameMockUpForPublicInterfaceManyTimesButApplyOnlyOnce()
   {
      class InterfaceMockUp extends MockUp<APublicInterface> {
         final int value;
         InterfaceMockUp(int value) { this.value = value; }
         @Mock int getNumericValue() { return value; }
      }

      int n = 10000;
      List<APublicInterface> mocks = new ArrayList<APublicInterface>(n);
      Class<?> implementationClass = null;

      for (int i = 0; i < n; i++) {
         if (Thread.interrupted()) {
            System.out.println("a) Interrupted at i = " + i);
            return;
         }

         APublicInterface mockInstance = new InterfaceMockUp(i).getMockInstance();
         Class<?> mockInstanceClass = mockInstance.getClass();

         if (implementationClass == null) {
            implementationClass = mockInstanceClass;
         }
         else {
            assertSame(implementationClass, mockInstanceClass);
         }

         mocks.add(mockInstance);
      }

      for (int i = 0; i < n; i++) {
         if (Thread.interrupted()) {
            System.out.println("b) Interrupted at i = " + i);
            return;
         }

         APublicInterface mockInstance = mocks.get(i);
         assertEquals(i, mockInstance.getNumericValue());
      }
   }

   interface ANonPublicInterface { int getValue(); }

   @Test
   public void multipleMockUpInstancesForANonPublicInterfaceWithASingleMockInstanceEach()
   {
      class AnotherInterfaceMockUp extends MockUp<ANonPublicInterface> implements ANonPublicInterface
      {
         private final int value;
         AnotherInterfaceMockUp(int value) { this.value = value; }
         @Override @Mock(invocations = 1) public int getValue() { return value; }
      }

      MockUp<ANonPublicInterface> mockUp1 = new AnotherInterfaceMockUp(1);
      ANonPublicInterface mock1 = mockUp1.getMockInstance();

      AnotherInterfaceMockUp mockUp2 = new AnotherInterfaceMockUp(2);
      ANonPublicInterface mock2 = mockUp2.getMockInstance();

      assertNotSame(mock1, mock2);
      assertSame(mock1.getClass(), mock2.getClass());
      assertEquals(1, mock1.getValue());
      assertEquals(2, mock2.getValue());
   }

   @Test
   public void applyDifferentMockUpsToSameInterface()
   {
      APublicInterface mock1 = new MockUp<APublicInterface>() {
         @Mock String getTextValue() { return "test"; }
      }.getMockInstance();

      APublicInterface mock2 = new MockUp<APublicInterface>() {
         @Mock int getNumericValue() { return 123; }
      }.getMockInstance();

      assertEquals("test", mock1.getTextValue());
      assertEquals(0, mock1.getNumericValue());
      assertEquals(123, mock2.getNumericValue());
      assertNull(mock2.getTextValue());
   }

   @Test
   public void applyMockUpWithGivenInterfaceInstance()
   {
      APublicInterface realInstance = new APublicInterface() {
         @Override public int getNumericValue() { return 1; }
         @Override public String getTextValue() { return "test"; }
         @Override public int getSomeOtherValue() { return 2; }
      };

      MockUp<APublicInterface> mockUp = new MockUp<APublicInterface>(realInstance) {
         @Mock int getNumericValue() { return 3; }
      };

      APublicInterface mockInstance = mockUp.getMockInstance();
      assertSame(realInstance, mockInstance);

      assertEquals(2, realInstance.getSomeOtherValue());
      assertEquals(3, mockInstance.getNumericValue());
   }
}
