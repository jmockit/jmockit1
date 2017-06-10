/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;

import org.junit.*;
import static org.junit.Assert.*;

public final class DelegateInvocationProceedTest
{
   public static class BaseClassToBeMocked
   {
      protected String name;

      public final String getName() { return name; }
      public final int baseMethod(int i) { return i + 1; }
      protected int methodToBeMocked(int i) throws IOException { return i; }
   }

   public static class ClassToBeMocked extends BaseClassToBeMocked
   {
      public ClassToBeMocked() { name = ""; }
      public ClassToBeMocked(String name) { this.name = name; }

      public boolean methodToBeMocked() { return true; }

      @Override
      protected int methodToBeMocked(int i) throws IOException { return super.methodToBeMocked(i); }

      int methodToBeMocked(int i, Object... args)
      {
         int result = i;

         for (Object arg : args) {
            if (arg != null) result++;
         }

         return result;
      }

      String anotherMethodToBeMocked(String s, boolean b, List<Integer> ints)
      { return (b ? s.toUpperCase() : s.toLowerCase()) + ints; }
   }

   @Test
   public void proceedFromDelegateMethodOnRegularMockedClass(@Mocked final ClassToBeMocked mocked)
   {
      new Expectations() {{
         mocked.methodToBeMocked();
         result = new Delegate() {
            @Mock boolean delegate(Invocation inv) { return inv.proceed(); }
         };
      }};

      assertTrue(mocked.methodToBeMocked());
   }

   @Test
   public void proceedFromDelegateMethodOnInjectableMockedClass(@Injectable final ClassToBeMocked mocked)
   {
      new Expectations() {{
         mocked.methodToBeMocked();
         result = new Delegate() {
            @Mock boolean delegate(Invocation inv) { return inv.proceed(); }
         };
      }};

      assertTrue(mocked.methodToBeMocked());
   }

   @Test
   public void proceedFromDelegateMethodWithParameters() throws Exception
   {
      final ClassToBeMocked mocked = new ClassToBeMocked();

      new Expectations(mocked) {{
         mocked.methodToBeMocked(anyInt);
         result = new Delegate() {
            @Mock int delegate(Invocation inv, int i) { Integer j = inv.proceed(); return j + 1; }
         };

         mocked.methodToBeMocked(anyInt, (Object[]) any); maxTimes = 1;
         result = new Delegate() {
            @Mock
            Integer delegate(Invocation inv, int i, Object... args)
            {
               args[2] = "mock";
               return inv.proceed();
            }
         };
      }};

      assertEquals(124, mocked.methodToBeMocked(123));
      assertEquals(-8, mocked.methodToBeMocked(-9));
      assertEquals(7, mocked.methodToBeMocked(3, "Test", new Object(), null, 45));
   }

   @Test
   public void proceedConditionallyFromDelegateMethod()
   {
      final ClassToBeMocked mocked = new ClassToBeMocked();

      new Expectations(mocked) {{
         mocked.anotherMethodToBeMocked(anyString, anyBoolean, null);
         result = new Delegate() {
            @Mock
            String delegate(Invocation inv, String s, boolean b, List<Number> ints)
            {
               if (!b) {
                  return s;
               }

               ints.add(45);
               return inv.proceed();
            }
         };
      }};

      // Do not proceed:
      assertNull(mocked.anotherMethodToBeMocked(null, false, null));

      // Do proceed:
      List<Integer> values = new ArrayList<Integer>();
      assertEquals("TEST[45]", mocked.anotherMethodToBeMocked("test", true, values));

      // Do not proceed again:
      assertEquals("No proceed", mocked.anotherMethodToBeMocked("No proceed", false, null));
   }

   @Test
   public void proceedFromDelegateMethodIntoRealMethodWithModifiedArguments() throws Exception
   {
      final ClassToBeMocked mocked = new ClassToBeMocked();

      new Expectations(ClassToBeMocked.class) {{
         mocked.methodToBeMocked(anyInt);
         result = new Delegate() {
            @Mock Integer delegate1(Invocation invocation, int i) { return invocation.proceed(i + 2); }
         };

         mocked.methodToBeMocked(anyInt, (Object[]) any);
         result = new Delegate() {
            @Mock Integer delegate2(Invocation inv, int i, Object... args) { return inv.proceed(1, 2, "3"); }
         };
      }};

      assertEquals(3, mocked.methodToBeMocked(1));
      assertEquals(3, mocked.methodToBeMocked(-2, null, "Abc", true, 'a'));
   }

   @Test
   public void proceedFromDelegateMethodIntoConstructor()
   {
      new Expectations(ClassToBeMocked.class) {{
         new ClassToBeMocked();
         result = new Delegate() {
            @Mock
            void init(Invocation inv)
            {
               assertNotNull(inv.getInvokedInstance());
               inv.proceed();
            }
         };
      }};

      ClassToBeMocked obj = new ClassToBeMocked();
      assertEquals("", obj.name);
   }

   @Test
   public void proceedConditionallyFromDelegateMethodIntoConstructor()
   {
      new Expectations(ClassToBeMocked.class) {{
         new ClassToBeMocked(anyString);
         result = new Delegate() {
            @Mock
            void init(Invocation inv, String name)
            {
               assertNotNull(inv.getInvokedInstance());

               if ("proceed".equals(name)) {
                  inv.proceed();
               }
            }
         };
      }};

      assertEquals("proceed", new ClassToBeMocked("proceed").name);
      assertNull(new ClassToBeMocked("do not proceed").name);
   }

   @Test
   public void proceedConditionallyFromDelegateMethodIntoJREConstructor()
   {
      new Expectations(ProcessBuilder.class) {{
         new ProcessBuilder(anyString);
         result = new Delegate() {
            @Mock
            void init(Invocation inv, String... command)
            {
               if ("proceed".equals(command[0])) {
                  inv.proceed();
               }
            }
         };
      }};

      ProcessBuilder obj1 = new ProcessBuilder("proceed");
      assertEquals("proceed", obj1.command().get(0));

      ProcessBuilder obj2 = new ProcessBuilder("do not proceed");
      assertNull(obj2.command());
   }

   @Test
   public void proceedFromDelegateMethodIntoMethodInheritedFromBaseClass()
   {
      final ClassToBeMocked obj = new ClassToBeMocked();

      new Expectations(obj) {{
         obj.baseMethod(anyInt);
         result = new Delegate() {
            @Mock int baseMethod(Invocation inv, int i) { return inv.proceed(i + 1); }
         };
      }};

      assertEquals(3, obj.baseMethod(1));
   }

   @Test
   public void proceedFromDelegateMethodIntoOverridingMethodWhichCallsSuper(@Mocked final ClassToBeMocked mocked)
      throws Exception
   {
      new Expectations() {{
         mocked.methodToBeMocked(1);
         result = new Delegate() {
            @Mock int delegate(Invocation inv) { return inv.proceed(); }
         };
      }};

      assertEquals(1, mocked.methodToBeMocked(1));
   }

   @Test
   public void proceedFromDelegateMethodIntoOverridingMethodThatCallsSuperWhichAlsoHasAProceedingDelegate(
      @Mocked final BaseClassToBeMocked mockedBase, @Mocked final ClassToBeMocked mocked) throws Exception
   {
      new Expectations() {{
         mockedBase.methodToBeMocked(1);
         result = new Delegate() {
            // Will not execute when calling on subclass instance.
            @Mock int delegate(Invocation inv) { int i = inv.proceed(); return i + 1; }
         };

         mocked.methodToBeMocked(1);
         result = new Delegate() {
            @Mock int delegate(Invocation inv) { return inv.proceed(); }
         };
      }};

      assertEquals(2, mockedBase.methodToBeMocked(1));
      assertEquals(1, mocked.methodToBeMocked(1));
   }

   @Test
   public void replaceMockedInstanceWithRealOne()
   {
      final ClassToBeMocked notMocked = new ClassToBeMocked("not mocked");

      new Expectations(ClassToBeMocked.class) {{
         new ClassToBeMocked(anyString); result = notMocked;
      }};

      assertEquals("not mocked", new ClassToBeMocked("test").getName());
   }

   @Test
   public void throwExceptionFromProceedIntoJREMethod(
      @Injectable final AbstractExecutorService c1, @Mocked final ClassToBeMocked c2)
      throws Exception
   {
      new Expectations() {{
         c1.submit((Runnable) any);
         result = new Delegate() {
            @Mock void delegate(Invocation inv) { inv.proceed(); }
         };
      }};

      try {
         c1.submit((Runnable) null);
         fail();
      }
      catch (NullPointerException ignored) {
         new Expectations() {{
            c2.methodToBeMocked(anyInt); result = 123;
         }};

         assertEquals(123, c2.methodToBeMocked(1));
      }
   }
}
