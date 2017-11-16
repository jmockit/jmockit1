/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit;

import java.util.*;

import org.junit.*;
import org.junit.rules.*;
import static org.junit.Assert.*;

@SuppressWarnings("unused")
public final class DelegateTest
{
   @Rule public final ExpectedException thrown = ExpectedException.none();

   static class Collaborator
   {
      Collaborator() {}
      Collaborator(int i) {}

      int getValue() { return -1; }
      String doSomething(boolean b, int[] i, String s) { return s + b + i[0]; }
      static boolean staticMethod() { return true; }
      static boolean staticMethod(int i) { return i > 0; }
      protected native long nativeMethod(boolean b);
      final char finalMethod() { return 's'; }
      void addElements(List<String> elements) { elements.add("one element"); }
      Foo getFoo() { return null; }
      byte[] getArray() { return null ;}
   }

   static final class Foo { int doSomething() { return 1; } }

   @Test
   public void resultFromDelegate(@Mocked final Collaborator collaborator)
   {
      final boolean bExpected = true;
      final int[] iExpected = new int[0];
      final String sExpected = "test";

      new Expectations() {{
         collaborator.getValue(); result = new Delegate() { int getValue() { return 2; } };

         collaborator.doSomething(bExpected, iExpected, sExpected);
         result = new Delegate() {
            String doSomething(boolean b, int[] i, String s)
            {
               assertEquals(bExpected, b);
               assertArrayEquals(iExpected, i);
               assertEquals(sExpected, s);
               return "";
            }
         };
      }};

      assertEquals(2, collaborator.getValue());
      assertEquals("", collaborator.doSomething(bExpected, iExpected, sExpected));
   }

   @Test
   public void consecutiveResultsThroughDelegatesHavingDifferentValues(@Mocked final Collaborator mock)
   {
      new Expectations() {{
         mock.getValue();
         result = new Delegate() { int getValue() { return 1; } };
         result = new Delegate() { int getValue() { return 2; } };
      }};

      Collaborator collaborator = new Collaborator();
      assertEquals(1, collaborator.getValue());
      assertEquals(2, collaborator.getValue());
   }

   @Test
   public void consecutiveReturnValuesThroughDelegatesUsingSingleReturnsWithVarargs(
      @Mocked final Collaborator collaborator)
   {
      final int[] array = {1, 2};

      new Expectations() {{
         collaborator.doSomething(true, array, "");
         returns(
            new Delegate() {
               String execute(boolean b, int[] i, String s)
               {
                  assertEquals(1, i[0]);
                  return "a";
               }
            },
            new Delegate() {
               String execute(boolean b, int[] i, String s)
               {
                  assertEquals(2, i[0]);
                  return "b";
               }
            });
      }};

      assertEquals("a", collaborator.doSomething(true, array, ""));

      array[0] = 2;
      assertEquals("b", collaborator.doSomething(true, array, ""));
   }

   @Test
   public void resultWithMultipleReturnValuesThroughSingleDelegate(@Mocked final Collaborator collaborator)
   {
      new Expectations() {{
         collaborator.getValue();
         result = new Delegate() {
            int i = 1;
            int getValue() { return i++; }
         };
      }};

      assertEquals(1, collaborator.getValue());
      assertEquals(2, collaborator.getValue());
      assertEquals(3, collaborator.getValue());
   }

   @Test
   public void constructorDelegateWithSingleMethod(@Mocked Collaborator mock)
   {
      final ConstructorDelegate delegate = new ConstructorDelegate();

      new Expectations() {{
         new Collaborator(anyInt); result = delegate;
      }};

      new Collaborator(4);

      assertTrue(delegate.capturedArgument > 0);
   }

   static class ConstructorDelegate implements Delegate<Void>
   {
      int capturedArgument;
      void delegate(int i) { capturedArgument = i; }
   }

   @Test
   public void constructorDelegateWithMultipleMethods(@Mocked Collaborator mock)
   {
      new Expectations() {{
         new Collaborator(anyInt);
         result = new Delegate() {
            void init(int i) { if (i < 0) throw new IllegalArgumentException(); }
            private void anotherMethod() {}
         };
      }};

      new Collaborator(123);

      try {
         new Collaborator(-123);
         fail();
      }
      catch (IllegalArgumentException ignore) {}
   }

   @Test
   public void attemptToUseConstructorDelegateWithPrivateMethodsOnly(@Mocked Collaborator mock)
   {
      thrown.expect(IllegalArgumentException.class);
      thrown.expectMessage("No non-private instance method found");

      new Expectations() {{
         new Collaborator();
         result = new Delegate() {
            private void delegate() {}
            private void anotherMethod() {}
         };
      }};
   }

   @Test
   public void delegateForStaticMethod(@Mocked Collaborator unused)
   {
      new Expectations() {{
         Collaborator.staticMethod();
         result = new Delegate() { boolean staticMethod() { return false; } };
      }};

      assertFalse(Collaborator.staticMethod());
   }

   @Test
   public void delegateWithStaticMethod(@Mocked Collaborator mock)
   {
      new Expectations() {{
         Collaborator.staticMethod(anyInt); result = StaticDelegate.create();
      }};

      assertTrue(Collaborator.staticMethod(34));
   }

   static final class StaticDelegate implements Delegate<Object>
   {
      static StaticDelegate create() { return new StaticDelegate(); }

      boolean delegateMethod(int i)
      {
         assertEquals(34, i);
         return true;
      }
   }

   @Test
   public void delegateForNativeMethod(@Mocked final Collaborator mock)
   {
      new Expectations() {{
         mock.nativeMethod(anyBoolean);
         result = new Delegate() {
            Long nativeMethod(boolean b) { assertTrue(b); return 0L; }
         };
      }};

      assertEquals(0L, new Collaborator().nativeMethod(true));
   }

   @Test
   public void delegateForFinalMethod(@Mocked final Collaborator mock)
   {
      new Expectations() {{
         mock.finalMethod();
         result = new Delegate() { char finalMethod() { return 'M'; } };
      }};

      assertEquals('M', new Collaborator().finalMethod());
   }

   @Test
   public void delegateForMethodWithCompatibleButDistinctParameterType(@Mocked final Collaborator collaborator)
   {
      new Expectations() {{
         collaborator.addElements(this.<List<String>>withNotNull());
         result = new Delegate() {
            void delegate(Collection<String> elements) { elements.add("test"); }
         };
      }};

      List<String> elements = new ArrayList<String>();
      new Collaborator().addElements(elements);

      assertTrue(elements.contains("test"));
   }

   @Test
   public void delegateReceivingNullArguments(@Mocked final Collaborator collaborator)
   {
      new Expectations() {{
         collaborator.doSomething(true, null, null);
         result = new Delegate() {
            String delegate(boolean b, int[] i, String s) {
               //noinspection ImplicitArrayToString
               return b + " " + i + " " + s;
            }
         };
      }};

      String s = new Collaborator().doSomething(true, null, null);
      assertEquals("true null null", s);
   }

   @Test
   public void delegateWithTwoMethods(@Mocked final Collaborator collaborator)
   {
      new Expectations() {{
         collaborator.doSomething(true, null, "str");
         result = new Delegate() {
            private String someOther() { return ""; }
            void doSomething(boolean b, int[] i, String s) {}
         };
      }};

      assertNull(collaborator.doSomething(true, null, "str"));
   }

   @Test
   public void delegateWithSingleMethodHavingADifferentName(@Mocked final Collaborator collaborator)
   {
      new Expectations() {{
         collaborator.doSomething(true, null, "str");
         result = new Delegate() {
            void onReplay(boolean b, int[] i, String s)
            {
               assertTrue(b);
               assertNull(i);
               assertEquals("str", s);
            }
         };
      }};

      assertNull(new Collaborator().doSomething(true, null, "str"));
   }

   @Test
   public void delegateWithSingleMethodHavingNoParameters(@Mocked final Collaborator collaborator)
   {
      new Expectations() {{
         collaborator.doSomething(anyBoolean, null, null);
         result = new Delegate() { String onReplay() { return "action"; } };
      }};

      String result = new Collaborator().doSomething(true, null, null);

      assertEquals("action", result);
   }

   @Test
   public void delegateWithSingleMethodHavingNoParametersExceptForInvocationContext(
      @Mocked final Collaborator collaborator)
   {
      new Expectations() {{
         collaborator.doSomething(anyBoolean, null, null);
         result = new Delegate() {
            void doSomething(Invocation inv) { assertEquals(1, inv.getInvocationCount()); }
         };
      }};

      assertNull(new Collaborator().doSomething(false, new int[] {1, 2}, "test"));
   }

   @Test
   public void delegateWithOneMethodHavingDifferentParameters(@Mocked final Collaborator collaborator)
   {
      thrown.expect(IllegalArgumentException.class);
      thrown.expectMessage("delegate(");

      new Expectations() {{
         collaborator.doSomething(true, null, "str");
         result = new Delegate() { void delegate(boolean b, String s) {} };
      }};

      collaborator.doSomething(true, null, "str");
   }

   @Test
   public void delegateWithTwoNonPrivateMethods(@Mocked final Collaborator collaborator)
   {
      thrown.expect(IllegalArgumentException.class);
      thrown.expectMessage("More than one candidate delegate method found: ");
      thrown.expectMessage("someOther()");
      thrown.expectMessage("doSomethingElse(boolean,int[],String)");

      new Expectations() {{
         collaborator.doSomething(true, null, "str");
         result = new Delegate() {
            String someOther() { return ""; }
            void doSomethingElse(boolean b, int[] i, String s) {}
         };
      }};
   }

   @Test
   public void delegateCausingConcurrentMockInvocation(@Mocked final Collaborator mock)
   {
      final Collaborator collaborator = new Collaborator();
      final Thread t = new Thread(new Runnable() {
         @Override
         public void run() { collaborator.doSomething(false, null, ""); }
      });

      new Expectations() {{
         mock.getValue(); times = 1;
         result = new Delegate() {
            int executeInAnotherThread() throws Exception
            {
               t.start();
               t.join();
               return 1;
            }
         };
      }};

      assertEquals(1, collaborator.getValue());
   }

   @Test
   public void delegateWhichCallsTheSameMockedMethod(@Mocked final Collaborator mock)
   {
      new Expectations() {{
         mock.getValue();
         result = new Delegate() {
            int count;
            // Would result in a StackOverflowError without a termination condition.
            int delegate() { return count++ > 1 ? 123 : 1 + mock.getValue(); }
         };
      }};

      assertEquals(125, mock.getValue());
   }

   @Test
   public void delegateWhichCallsAnotherMockedMethod_fullMocking(@Mocked final Collaborator mock)
   {
      new Expectations() {{
         mock.getValue();
         result = new Delegate() {
            int delegate() { return mock.finalMethod(); }
         };

         mock.finalMethod(); result = 'A';
      }};

      assertEquals('A', mock.getValue());
   }

   @Test
   public void delegateWhichCallsAnotherMockedMethod_partialMockingOfClass()
   {
      final Collaborator collaborator = new Collaborator();

      new Expectations(Collaborator.class) {{
         Collaborator.staticMethod(); result = false;

         collaborator.getValue();
         result = new Delegate() {
            int delegate() { return Collaborator.staticMethod() ? 1 : collaborator.finalMethod(); }
         };

         collaborator.finalMethod(); result = 'A';
      }};

      assertEquals('A', collaborator.getValue());
   }

   @Test
   public void delegateWhichCallsAnotherMockedMethod_partialMockingOfInstance()
   {
      final Collaborator collaborator = new Collaborator();

      new Expectations(collaborator) {{
         collaborator.getValue();
         result = new Delegate() {
            int delegate() { return collaborator.finalMethod(); }
         };

         collaborator.finalMethod(); result = 'A';
      }};

      assertEquals('A', collaborator.getValue());
   }

   @Test
   public void delegateWhichCallsAnotherMockedMethod_injectableMocking(@Injectable final Collaborator mock)
   {
      new Expectations() {{
         mock.getValue();
         result = new Delegate() {
            int delegate() { return mock.finalMethod(); }
         };

         mock.finalMethod(); result = 'A';
      }};

      assertEquals('A', mock.getValue());
   }

   @Test
   public void delegateWhichCallsAnotherMockedMethodProducingACascadedInstance(@Mocked final Collaborator mock)
   {
      new Expectations() {{
         mock.getFoo().doSomething(); result = 123;

         mock.getValue();
         result = new Delegate() {
            int delegate() { return mock.getFoo().doSomething(); }
         };
      }};

      assertEquals(123, mock.getFoo().doSomething());
      assertEquals(123, mock.getValue());
   }

   @Test
   public void delegateCallingMockedMethodLaterVerified(
      @Mocked final Collaborator collaborator, @Mocked final Runnable action)
   {
      new Expectations() {{
         collaborator.getFoo();
         result = new Delegate() {
            void delegate() { action.run(); }
         };
      }};

      collaborator.getFoo();

      new Verifications() {{ action.run(); }};
   }

   @Test
   public void convertValueReturnedFromDelegateWhenReturnsTypesDiffer(@Mocked final Collaborator mock)
   {
      new Expectations() {{
         mock.getValue();
         result = new Delegate() {
            byte delegate() { return (byte) 123; }
         };
      }};

      int value = mock.getValue();

      assertEquals(123, value);
   }

   @Test
   public void returnInconvertibleValueFromDelegateWhenReturnsTypesDiffer(@Mocked final Collaborator mock)
   {
      new Expectations() {{
         mock.getValue();
         result = new Delegate() {
            String delegate() { return "abc"; }
         };
      }};

      thrown.expect(IllegalArgumentException.class);
      thrown.expectMessage("Value of type String incompatible with return type int");

      mock.getValue();
   }

   @Test
   public void returnVoidFromDelegateMethodForRecordedMethodHavingPrimitiveReturnType(@Mocked final Collaborator mock)
   {
      new Expectations() {{
         mock.getValue();
         result = new Delegate() { void delegate() {} };
      }};

      thrown.expect(IllegalArgumentException.class);
      thrown.expectMessage("void return type incompatible with return type int");

      mock.getValue();
   }

   @Test
   public void returnByteArrayFromDelegateMethod(@Mocked final Collaborator mock)
   {
      final byte[] bytes = "test".getBytes();
      new Expectations() {{ mock.getArray(); result = new Delegate() { byte[] delegate() { return bytes; } }; }};

      assertSame(bytes, mock.getArray());
   }
}
