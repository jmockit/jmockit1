/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit;

import java.io.*;
import java.util.*;

import org.junit.*;
import org.junit.rules.*;
import static org.junit.Assert.*;

public final class FakeInvocationProceedTest
{
   @Rule public final ExpectedException thrown = ExpectedException.none();

   public static class BaseClassToBeFaked
   {
      protected String name;

      public final int baseMethod(int i) { return i + 1; }
      protected int methodToBeFaked(int i) throws IOException { return i; }
   }

   public static class ClassToBeFaked extends BaseClassToBeFaked
   {
      public ClassToBeFaked() { name = ""; }
      public ClassToBeFaked(String name) { this.name = name; }

      public boolean methodToBeFaked() { return true; }

      protected int methodToBeFaked(int i, Object... args)
      {
         int result = i;

         for (Object arg : args) {
            if (arg != null) result++;
         }

         return result;
      }

      public String anotherMethodToBeFaked(String s, boolean b, List<Integer> ints)
      { return (b ? s.toUpperCase() : s.toLowerCase()) + ints; }

      public static boolean staticMethodToBeFaked() throws FileNotFoundException { throw new FileNotFoundException(); }

      protected static native void nativeMethod();
   }

   @Test
   public void proceedFromFakeMethodWithoutParameters()
   {
      new MockUp<ClassToBeFaked>() {
         @Mock boolean methodToBeMocked(Invocation inv) { return inv.proceed(); }
      };

      assertTrue(new ClassToBeFaked().methodToBeFaked());
   }

   @Test
   public void proceedFromFakeMethodWithParameters() throws Exception
   {
      new MockUp<ClassToBeFaked>() {
         @Mock int methodToBeFaked(Invocation inv, int i) { Integer j = inv.proceed(); return j + 1; }

         @Mock
         private int methodToBeFaked(Invocation inv, int i, Object... args)
         {
            args[2] = "mock";
            return inv.<Integer>proceed();
         }
      };

      ClassToBeFaked faked = new ClassToBeFaked();

      assertEquals(124, faked.methodToBeFaked(123));
      assertEquals(-8, faked.methodToBeFaked(-9));
      assertEquals(7, faked.methodToBeFaked(3, "Test", new Object(), null, 45));
   }

   @Test
   public void proceedConditionallyFromFakeMethod() throws Exception
   {
      new MockUp<ClassToBeFaked>() {
         @Mock
         String anotherMethodToBeFaked(Invocation inv, String s, boolean b, List<Number> ints)
         {
            if (!b) {
               return s;
            }

            ints.add(45);
            return inv.proceed();
         }
      };

      ClassToBeFaked mocked = new ClassToBeFaked();

      // Do not proceed:
      assertNull(mocked.anotherMethodToBeFaked(null, false, null));

      // Do proceed:
      List<Integer> values = new ArrayList<Integer>();
      assertEquals("TEST[45]", mocked.anotherMethodToBeFaked("test", true, values));

      // Do not proceed again:
      assertEquals("No proceed", mocked.anotherMethodToBeFaked("No proceed", false, null));
   }

   @Test
   public void proceedFromFakeMethodWhichThrowsCheckedException() throws Exception
   {
      new MockUp<ClassToBeFaked>() {
         @Mock
         boolean staticMethodToBeFaked(Invocation inv) throws Exception
         {
            if (inv.getInvocationIndex() == 0) {
               return inv.<Boolean>proceed();
            }

            throw new InterruptedException("fake");
         }
      };

      try { ClassToBeFaked.staticMethodToBeFaked(); fail(); } catch (FileNotFoundException ignored) {}

      thrown.expect(InterruptedException.class);
      ClassToBeFaked.staticMethodToBeFaked();
   }

   @Test
   public void proceedFromFakeMethodIntoRealMethodWithModifiedArguments() throws Exception
   {
      class FakeWhichModifiesArguments extends MockUp<ClassToBeFaked> {
         @Mock
         final int methodToBeFaked(Invocation invocation, int i) { return invocation.<Integer>proceed(i + 2); }
      }

      new FakeWhichModifiesArguments() {
         @Mock
         synchronized int methodToBeFaked(Invocation inv, int i, Object... args)
         {
            return inv.<Integer>proceed(1, 2, "3");
         }
      };

      ClassToBeFaked faked = new ClassToBeFaked();
      assertEquals(3, faked.methodToBeFaked(1));
      assertEquals(3, faked.methodToBeFaked(-2, null, "Abc", true, 'a'));
   }

   @Test
   public void cannotProceedFromFakeMethodIntoNativeMethod()
   {
      new MockUp<ClassToBeFaked>() {
         @Mock
         void nativeMethod(Invocation inv)
         {
            inv.proceed();
            fail("Should not get here");
         }
      };

      thrown.expect(UnsupportedOperationException.class);
      thrown.expectMessage("Cannot proceed");
      thrown.expectMessage("native method");

      ClassToBeFaked.nativeMethod();
   }

   @Test
   public void proceedFromFakeMethodIntoConstructor()
   {
      new MockUp<ClassToBeFaked>() {
         @Mock void $init(Invocation inv)
         {
            assertNotNull(inv.<ClassToBeFaked>getInvokedInstance());
            inv.proceed();
         }
      };

      ClassToBeFaked obj = new ClassToBeFaked();
      assertEquals("", obj.name);
   }

   @Test
   public void proceedConditionallyFromFakeMethodIntoConstructor()
   {
      new MockUp<ClassToBeFaked>() {
         @Mock void $init(Invocation inv, String name)
         {
            assertNotNull(inv.getInvokedInstance());

            if ("proceed".equals(name)) {
               inv.proceed();
            }
         }
      };

      assertEquals("proceed", new ClassToBeFaked("proceed").name);
      assertNull(new ClassToBeFaked("do not proceed").name);
   }

   @Test
   public void proceedConditionallyFromFakeMethodIntoJREConstructor()
   {
      new MockUp<File>() {
         @Mock void $init(Invocation inv, String name)
         {
            if ("proceed".equals(name)) {
               inv.proceed();
            }
         }
      };

      assertEquals("proceed", new File("proceed").getPath());
      assertNull(new File("do not proceed").getPath());
   }

   @Test
   public void proceedFromFakeMethodIntoMethodInheritedFromBaseClass()
   {
      new MockUp<ClassToBeFaked>() {
         @Mock int baseMethod(Invocation inv, int i) { return inv.proceed(i + 1); }
      };

      assertEquals(3, new ClassToBeFaked().baseMethod(1));
   }
}
