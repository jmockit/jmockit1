/*
 * Copyright (c) 2006 Rogério Liesenfeld
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

import mockit.internal.expectations.mocking.*;
import mockit.internal.faking.*;
import mockit.internal.startup.*;
import mockit.internal.util.*;

public abstract class MockingBridge implements InvocationHandler
{
   private static final Object[] EMPTY_ARGS = {};
   private static final ReentrantLock LOCK = new ReentrantLock();
   private static boolean fieldsSet;
   public final String id;

   /**
    * The instance is stored in a place directly accessible through the Java SE API, so that it can
    * be recovered from any class loader.
    */
   protected MockingBridge(@Nonnull String id) { this.id = id; }

   protected static boolean notToBeMocked(@Nullable Object mocked, @Nonnull String mockedClassDesc)
   {
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

            if (
               "ClassLoader.java".equals(ste.getFileName()) &&
               "loadClass getResource loadLibrary".contains(ste.getMethodName())
            ) {
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

   public static void setMockingBridgeFields()
   {
      Class<?> hostClass = ClassLoad.loadByInternalName(InstrumentationHolder.hostJREClassName);
      setMockingBridgeField(hostClass, MockedBridge.MB);
      setMockingBridgeField(hostClass, FakeBridge.MB);
      setMockingBridgeField(hostClass, FakeMethodBridge.MB);
   }

   private static void setMockingBridgeField(@Nonnull Class<?> hostClass, @Nonnull MockingBridge mockingBridge)
   {
      try {
         hostClass.getDeclaredField(mockingBridge.id).set(null, mockingBridge);
      }
      catch (NoSuchFieldException ignore) {}
      catch (IllegalAccessException e) { throw new RuntimeException(e); }
   }

   @Nonnull
   public static String getHostClassName()
   {
      if (!fieldsSet) {
         setMockingBridgeFields();
         fieldsSet = true;
      }

      return InstrumentationHolder.hostJREClassName;
   }
}
