/*
 * Copyright (c) 2006-2015 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal;

import java.io.*;
import java.lang.reflect.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.locks.*;
import java.util.jar.*;
import javax.annotation.*;

import mockit.internal.util.*;

public abstract class MockingBridge implements InvocationHandler
{
   private static final Object[] EMPTY_ARGS = {};
   private static final ReentrantLock LOCK = new ReentrantLock();

   public static void preventEventualClassLoadingConflicts()
   {
      // Pre-load certain JMockit classes to avoid NoClassDefFoundError's or re-entrancy loops during class loading
      // when certain JRE classes are mocked, such as ArrayList or Thread.
      try {
         Class.forName("mockit.Capturing");
         Class.forName("mockit.Delegate");
         Class.forName("mockit.Injectable");
         Class.forName("mockit.Invocation");
         Class.forName("mockit.Mocked");
         Class.forName("mockit.MockUp");
         Class.forName("mockit.Tested");
         Class.forName("mockit.internal.RedefinitionEngine");
         Class.forName("mockit.internal.util.GeneratedClasses");
         Class.forName("mockit.internal.util.MethodReflection");
         Class.forName("mockit.internal.util.ObjectMethods");
         Class.forName("mockit.internal.util.TypeDescriptor");
         Class.forName("mockit.internal.state.MockedTypeCascade");
         Class.forName("mockit.internal.expectations.RecordAndReplayExecution");
         Class.forName("mockit.internal.expectations.invocation.InvocationResults");
         Class.forName("mockit.internal.expectations.mocking.BaseTypeRedefinition$MockedClass");
         Class.forName("mockit.internal.expectations.mocking.FieldTypeRedefinitions");
         Class.forName("mockit.internal.expectations.argumentMatching.EqualityMatcher");
      }
      catch (ClassNotFoundException ignore) {}

      wasCalledDuringClassLoading();
      DefaultValues.computeForReturnType("()J");
   }

   public final String id;

   /**
    * The instance is stored in a place directly accessible through the Java SE API, so that it can
    * be recovered from any class loader.
    */
   protected MockingBridge(@Nonnull String id) { this.id = id; }

   protected static boolean notToBeMocked(@Nullable Object mocked, @Nonnull String mockedClassDesc)
   {
      Thread currentThread = Thread.currentThread();

      if ("java.awt.EventDispatchThread".equals(currentThread.getClass().getName())) {
         return true;
      }

      if ("Finalizer".equals(currentThread.getName())) {
         return true;
      }

      return
         (mocked == null && "java/lang/System".equals(mockedClassDesc) ||
          mocked != null && instanceOfClassThatParticipatesInClassLoading(mocked.getClass())
         ) && wasCalledDuringClassLoading();
   }

   public static boolean instanceOfClassThatParticipatesInClassLoading(@Nonnull Class<?> mockedClass)
   {
      return
         mockedClass == System.class || mockedClass == File.class || mockedClass == URL.class ||
         mockedClass == FileInputStream.class || mockedClass == Manifest.class ||
         JarFile.class.isAssignableFrom(mockedClass) || JarEntry.class.isAssignableFrom(mockedClass) ||
         Vector.class.isAssignableFrom(mockedClass) || Hashtable.class.isAssignableFrom(mockedClass);
   }

   private static boolean wasCalledDuringClassLoading()
   {
      if (LOCK.isHeldByCurrentThread()) {
         return true;
      }

      LOCK.lock();

      try {
         StackTrace st = new StackTrace(new Throwable());
         int n = st.getDepth();

         for (int i = 3; i < n; i++) {
            StackTraceElement ste = st.getElement(i);

            if ("ClassLoader.java".equals(ste.getFileName()) && "loadClass".equals(ste.getMethodName())) {
               return true;
            }
         }

         return false;
      }
      finally {
         LOCK.unlock();
      }
   }

   @Nonnull
   protected static Object[] extractMockArguments(int startingIndex, @Nonnull Object[] args)
   {
      if (args.length > startingIndex) {
         Object[] mockArgs = new Object[args.length - startingIndex];
         System.arraycopy(args, startingIndex, mockArgs, 0, mockArgs.length);
         return mockArgs;
      }

      return EMPTY_ARGS;
   }
}
