/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit;

import java.io.*;

import org.junit.*;
import static org.junit.Assert.*;

import static mockit.internal.util.Utilities.*;

public final class ReentrantFakeTest
{
   public static class RealClass
   {
      public String foo() { return "real value"; }
      protected static int staticRecursiveMethod(int i) { return i <= 0 ? 0 : 2 + staticRecursiveMethod(i - 1); }
      public int recursiveMethod(int i) { return i <= 0 ? 0 : 2 + recursiveMethod(i - 1); }
      protected static int nonRecursiveStaticMethod(int i) { return -i; }
      public int nonRecursiveMethod(int i) { return -i; }
   }

   public static class AnnotatedFakeClass extends MockUp<RealClass>
   {
      private static Boolean fakeIt;

      @Mock
      public String foo(Invocation inv)
      {
         if (fakeIt == null) {
            throw new IllegalStateException("null fakeIt");
         }
         else if (fakeIt) {
            return "fake value";
         }
         else {
            return inv.proceed();
         }
      }
   }

   @Test
   public void callFakeMethod()
   {
      new AnnotatedFakeClass();
      AnnotatedFakeClass.fakeIt = true;

      String foo = new RealClass().foo();

      assertEquals("fake value", foo);
   }

   @Test
   public void callOriginalMethod()
   {
      new AnnotatedFakeClass();
      AnnotatedFakeClass.fakeIt = false;

      String foo = new RealClass().foo();

      assertEquals("real value", foo);
   }

   @Test(expected = IllegalStateException.class)
   public void calledFakeThrowsException()
   {
      new AnnotatedFakeClass();
      AnnotatedFakeClass.fakeIt = null;

      new RealClass().foo();
   }

   public static class FakeRuntime extends MockUp<Runtime>
   {
      private int runFinalizationCount;

      @Mock
      public void runFinalization(Invocation inv)
      {
         if (runFinalizationCount < 2) {
            inv.proceed();
         }

         runFinalizationCount++;
      }

      @Mock
      public boolean removeShutdownHook(Invocation inv, Thread hook)
      {
         if (hook == null) {
            //noinspection AssignmentToMethodParameter
            hook = Thread.currentThread();
         }

         return inv.proceed(hook);
      }

      @Mock
      public void runFinalizersOnExit(boolean value)
      {
         assertTrue(value);
      }
   }

   @Test
   public void callFakeMethodForJREClass()
   {
      Runtime runtime = Runtime.getRuntime();
      new FakeRuntime();

      runtime.runFinalization();
      runtime.runFinalization();
      runtime.runFinalization();

      assertFalse(runtime.removeShutdownHook(null));

      //noinspection deprecation
      Runtime.runFinalizersOnExit(true);
   }

   public static class ReentrantFakeForNativeMethod extends MockUp<Runtime>
   {
      @Mock
      public int availableProcessors(Invocation inv)
      {
         assertNotNull(inv.getInvokedInstance());
         return 5;
      }
   }

   @Test
   public void setUpReentrantFakeForNativeJREMethod()
   {
      new ReentrantFakeForNativeMethod();

      assertEquals(5, Runtime.getRuntime().availableProcessors());
   }

   static class MultiThreadedFake extends MockUp<RealClass>
   {
      private static boolean nobodyEntered = true;

      @Mock
      public String foo(Invocation inv) throws InterruptedException
      {
         String value = inv.proceed();

         synchronized (MultiThreadedFake.class) {
            if (nobodyEntered) {
               nobodyEntered = false;
               //noinspection WaitNotInLoop
               MultiThreadedFake.class.wait(5000);
            }
            else {
               MultiThreadedFake.class.notifyAll();
            }
         }

         return value.replace("real", "fake");
      }
   }

   @Test(timeout = 1000)
   public void twoConcurrentThreadsCallingTheSameReentrantFake() throws Exception
   {
      new MultiThreadedFake();

      final StringBuilder first = new StringBuilder();
      final StringBuilder second = new StringBuilder();

      Thread thread1 = new Thread(new Runnable() {
         @Override
         public void run() { first.append(new RealClass().foo()); }
      });
      thread1.start();

      Thread thread2 = new Thread(new Runnable() {
         @Override
         public void run() { second.append(new RealClass().foo()); }
      });
      thread2.start();

      thread1.join();
      thread2.join();

      assertEquals("fake value", first.toString());
      assertEquals("fake value", second.toString());
   }

   public static final class RealClass2
   {
      public int firstMethod() { return 1; }
      public int secondMethod() { return 2; }
   }

   @Test
   public void reentrantFakeForNonJREClassWhichCallsAnotherFromADifferentThread()
   {
      new MockUp<RealClass2>() {
         int value;

         @Mock
         int firstMethod(Invocation inv) { return inv.proceed(); }

         @Mock
         int secondMethod(Invocation inv) throws InterruptedException
         {
            final RealClass2 it = inv.getInvokedInstance();

            Thread t = new Thread() {
               @Override
               public void run() { value = it.firstMethod(); }
            };
            t.start();
            t.join();
            return value;
         }
      };

      RealClass2 r = new RealClass2();
      assertEquals(1, r.firstMethod());
      assertEquals(1, r.secondMethod());
   }

   @Test
   public void reentrantFakeForJREClassWhichCallsAnotherFromADifferentThread()
   {
      System.setProperty("a", "1");
      System.setProperty("b", "2");

      if (HOTSPOT_VM) { // causes main thread to hang up on IBM JRE
         new MockUp<System>()
         {
            String property;

            @Mock
            String getProperty(Invocation inv, String key) { return inv.proceed(); }

            @Mock
            String clearProperty(final String key) throws InterruptedException
            {
               Thread t = new Thread()
               {
                  @Override
                  public void run() { property = System.getProperty(key); }
               };
               t.start();
               t.join();
               return property;
            }
         };
      }

      assertEquals("1", System.getProperty("a"));
      assertEquals("2", System.clearProperty("b"));
   }

   @Test
   public void fakeFileAndForceJREToCallReentrantFakedMethod()
   {
      new MockUp<File>() {
         @Mock
         boolean exists(Invocation inv) { boolean exists = inv.proceed(); return !exists; }
      };

      // Cause the JVM/JRE to load a new class, calling the faked File#exists() method in the process:
      new Runnable() { @Override public void run() {} };

      assertTrue(new File("noFile").exists());
   }

   public static final class RealClass3
   {
      public RealClass3 newInstance() { return new RealClass3(); }
   }

   @Test
   public void reentrantFakeForMethodWhichInstantiatesAndReturnsNewInstanceOfTheFakedClass()
   {
      new MockUp<RealClass3>() {
         @Mock
         RealClass3 newInstance(Invocation inv) { return null; }
      };

      assertNull(new RealClass3().newInstance());
   }

   public static final class FakeClassWithReentrantFakeForRecursiveMethod extends MockUp<RealClass>
   {
      @Mock
      int recursiveMethod(Invocation inv, int i) { int j = inv.proceed(); return 1 + j; }

      @Mock
      static int staticRecursiveMethod(Invocation inv, int i) { int j = inv.proceed(); return 1 + j; }
   }

   @Test
   public void reentrantFakeMethodForRecursiveMethods()
   {
      assertEquals(0, RealClass.staticRecursiveMethod(0));
      assertEquals(2, RealClass.staticRecursiveMethod(1));

      RealClass r = new RealClass();
      assertEquals(0, r.recursiveMethod(0));
      assertEquals(2, r.recursiveMethod(1));

      new FakeClassWithReentrantFakeForRecursiveMethod();

      assertEquals(1, RealClass.staticRecursiveMethod(0));
      assertEquals(1 + 2 + 1, RealClass.staticRecursiveMethod(1));
      assertEquals(1, r.recursiveMethod(0));
      assertEquals(4, r.recursiveMethod(1));
   }

   @Test
   public void fakeThatProceedsIntoRecursiveMethod()
   {
      RealClass r = new RealClass();
      assertEquals(0, r.recursiveMethod(0));
      assertEquals(2, r.recursiveMethod(1));

      new MockUp<RealClass>() {
         @Mock
         int recursiveMethod(Invocation inv, int i)
         {
            int ret = inv.proceed();
            return 1 + ret;
         }
      };

      assertEquals(1, r.recursiveMethod(0));
      assertEquals(4, r.recursiveMethod(1));
   }

   @Test
   public void recursiveFakeMethodWithoutInvocationParameter()
   {
      new MockUp<RealClass>() {
         @Mock
         int nonRecursiveStaticMethod(int i)
         {
            if (i > 1) return i;
            return RealClass.nonRecursiveStaticMethod(i + 1);
         }
      };

      int result = RealClass.nonRecursiveStaticMethod(1);
      assertEquals(2, result);
   }

   @Test
   public void recursiveFakeMethodWithInvocationParameterNotUsedForProceeding()
   {
      new MockUp<RealClass>() {
         @Mock
         int nonRecursiveMethod(Invocation inv, int i)
         {
            if (i > 1) return i;
            RealClass it = inv.getInvokedInstance();
            return it.nonRecursiveMethod(i + 1);
         }
      };

      int result = new RealClass().nonRecursiveMethod(1);
      assertEquals(2, result);
   }

   @Test
   public void nonRecursiveFakeMethodWithInvocationParameterUsedForProceeding()
   {
      new MockUp<RealClass>() {
         @Mock
         int nonRecursiveMethod(Invocation inv, int i)
         {
            if (i > 1) return i;
            return inv.proceed(i + 1);
         }
      };

      int result = new RealClass().nonRecursiveMethod(1);
      assertEquals(-2, result);
   }
}
