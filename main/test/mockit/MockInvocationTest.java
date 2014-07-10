/*
 * Copyright (c) 2006-2014 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit;

import static org.junit.Assert.*;
import org.junit.*;
import org.junit.rules.*;

public final class MockInvocationTest
{
   @Rule public final ExpectedException thrown = ExpectedException.none();

   static class Collaborator
   {
      int value;
      
      Collaborator() {}
      Collaborator(int i) { value = i; }

      int getValue() { return -1; }
      void setValue(int i) { value = i; }
      String doSomething(boolean b, int[] i, String s) { return s + b + i[0]; }
      static boolean staticMethod() { return true; }
   }

   static final class MockMethods extends MockUp<Collaborator>
   {
      @Mock
      static boolean staticMethod(Invocation context)
      {
         assertNotNull(context);
         assertNull(context.getInvokedInstance());
         assertEquals(0, context.getMinInvocations());
         assertEquals(-1, context.getMaxInvocations());
         assertEquals(1, context.getInvocationCount());
         return false;
      }

      @Mock(minInvocations = 1, maxInvocations = 2)
      int getValue(Invocation context)
      {
         assertTrue(context.getInvokedInstance() instanceof Collaborator);
         assertEquals(1, context.getMinInvocations());
         assertEquals(2, context.getMaxInvocations());
         assertEquals(0, context.getInvocationIndex());
         return 123;
      }
   }

   @Test
   public void mockMethodsForMethodsWithoutParameters()
   {
      new MockMethods();
      assertFalse(Collaborator.staticMethod());
      assertEquals(123, new Collaborator().getValue());
   }

   @Test
   public void instanceMockMethodForStaticMethod()
   {
      new MockUp<Collaborator>() {
         @Mock(invocations = 2)
         boolean staticMethod(Invocation context)
         {
            assertNull(context.getInvokedInstance());
            assertEquals(context.getInvocationCount() - 1, context.getInvocationIndex());
            assertEquals(2, context.getMinInvocations());
            assertEquals(2, context.getMaxInvocations());
            return context.getInvocationCount() <= 0;
         }
      };

      assertFalse(Collaborator.staticMethod());
      assertFalse(Collaborator.staticMethod());
   }

   @Test
   public void mockMethodsWithInvocationParameter()
   {
      new MockUp<Collaborator>() {
         Collaborator instantiated;

         @Mock(invocations = 1)
         void $init(Invocation inv, int i)
         {
            assertNotNull(inv.getInvokedInstance());
            assertTrue(i > 0);
            instantiated = inv.getInvokedInstance();
         }

         @Mock
         String doSomething(Invocation inv, boolean b, int[] array, String s)
         {
            assertNotNull(inv);
            assertSame(instantiated, inv.getInvokedInstance());
            assertEquals(1, inv.getInvocationCount());
            assertTrue(b);
            assertNull(array);
            assertEquals("test", s);
            return "mock";
         }
      };

      String s = new Collaborator(123).doSomething(true, null, "test");
      assertEquals("mock", s);
   }

   static class MockMethodsWithParameters extends MockUp<Collaborator>
   {
      int capturedArgument;
      Collaborator mockedInstance;

      @Mock(invocations = 1)
      void $init(Invocation context, int i)
      {
         assertEquals(1, context.getMinInvocations());
         assertEquals(1, context.getMaxInvocations());
         capturedArgument = i + context.getInvocationCount();
         assertNull(mockedInstance);
         assertTrue(context.getInvokedInstance() instanceof Collaborator);
         assertEquals(1, context.getInvokedArguments().length);
      }

      @Mock(invocations = 2)
      void setValue(Invocation context, int i)
      {
         assertEquals(2, context.getMinInvocations());
         assertEquals(2, context.getMaxInvocations());
         assertEquals(i, context.getInvocationIndex());
         assertSame(mockedInstance, context.getInvokedInstance());
         assertEquals(1, context.getInvokedArguments().length);
      }
   }

   @Test
   public void mockMethodsWithParameters()
   {
      MockMethodsWithParameters mock = new MockMethodsWithParameters();

      Collaborator col = new Collaborator(4);
      mock.mockedInstance = col;

      assertEquals(5, mock.capturedArgument);
      col.setValue(0);
      col.setValue(1);
   }

   @SuppressWarnings("deprecation")
   @Test
   public void useOfContextParametersForJREMethods() throws Exception
   {
      new MockUp<Runtime>() {
         @Mock(minInvocations = 1)
         void runFinalizersOnExit(Invocation inv, boolean b)
         {
            assertNull(inv.getInvokedInstance());
            assertEquals(1, inv.getInvocationCount());
            assertEquals(1, inv.getMinInvocations());
            assertEquals(-1, inv.getMaxInvocations());
            assertTrue(b);
         }

         @Mock(maxInvocations = 1)
         Process exec(Invocation inv, String command, String[] envp)
         {
            assertSame(Runtime.getRuntime(), inv.getInvokedInstance());
            assertEquals(0, inv.getInvocationIndex());
            assertEquals(0, inv.getMinInvocations());
            assertEquals(1, inv.getMaxInvocations());
            assertNotNull(command);
            assertNull(envp);
            return null;
         }
      };

      Runtime.runFinalizersOnExit(true);
      assertNull(Runtime.getRuntime().exec("test", null));
   }
}