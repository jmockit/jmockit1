/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit;

import static org.junit.Assert.*;
import org.junit.*;
import org.junit.rules.*;

public final class MockInvocationTest
{
   @Rule public final ExpectedException thrown = ExpectedException.none();

   public static class Collaborator
   {
      int value;
      
      Collaborator() {}
      public Collaborator(int i) { value = i; }

      public int getValue() { return -1; }
      public void setValue(int i) { value = i; }
      public String doSomething(boolean b, int[] i, String s) { return s + b + i[0]; }
      public static boolean staticMethod() { return true; }
   }

   static final class MockMethods extends MockUp<Collaborator>
   {
      @Mock
      static boolean staticMethod(Invocation context)
      {
         assertNotNull(context);
         assertNull(context.getInvokedInstance());
         assertEquals(1, context.getInvocationCount());
         return false;
      }

      @Mock
      int getValue(Invocation context)
      {
         assertTrue(context.getInvokedInstance() instanceof Collaborator);
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
         @Mock
         boolean staticMethod(Invocation context)
         {
            assertNull(context.getInvokedInstance());
            assertEquals(context.getInvocationCount() - 1, context.getInvocationIndex());
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

         @Mock
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

      @Mock
      void $init(Invocation context, int i)
      {
         capturedArgument = i + context.getInvocationCount();
         assertNull(mockedInstance);
         assertTrue(context.getInvokedInstance() instanceof Collaborator);
         assertEquals(1, context.getInvokedArguments().length);
      }

      @Mock
      void setValue(Invocation context, int i)
      {
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

   @Test
   public void useOfContextParametersForJREMethods() throws Exception
   {
      new MockUp<Runtime>() {
         @Mock
         void runFinalizersOnExit(Invocation inv, boolean b)
         {
            assertNull(inv.getInvokedInstance());
            assertEquals(1, inv.getInvocationCount());
            assertTrue(b);
         }

         @Mock
         Process exec(Invocation inv, String command, String[] envp)
         {
            assertSame(Runtime.getRuntime(), inv.getInvokedInstance());
            assertEquals(0, inv.getInvocationIndex());
            assertNotNull(command);
            assertNull(envp);
            return null;
         }
      };

      //noinspection deprecation
      Runtime.runFinalizersOnExit(true);
      assertNull(Runtime.getRuntime().exec("test", null));
   }
}