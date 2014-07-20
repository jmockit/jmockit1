/*
 * Copyright (c) 2006-2014 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.expectations;

import static java.lang.reflect.Modifier.*;

import mockit.internal.state.*;

import org.jetbrains.annotations.*;

public enum ExecutionMode
{
   Regular
   {
      @Override
      boolean isNativeMethodToBeIgnored(int access) { return false; }

      @Override
      boolean isToExecuteRealImplementation(@Nullable Object mockedInstance)
      {
         return mockedInstance != null && !TestRun.mockFixture().isInstanceOfMockedClass(mockedInstance);
      }
   },

   Partial
   {
      @Override
      boolean isWithRealImplementation(@Nullable Object mockedInstance)
      {
         return mockedInstance == null || !TestRun.getExecutingTest().isInjectableMock(mockedInstance);
      }
   },

   PerInstance
   {
      @Override
      boolean isStaticMethodToBeIgnored(int access) { return isStatic(access); }

      @Override
      boolean isToExecuteRealImplementation(@Nullable Object mockedInstance)
      {
         return mockedInstance == null || !TestRun.getExecutingTest().isMockedInstance(mockedInstance);
      }
   };

   public final boolean isMethodToBeIgnored(int access)
   {
      return isStaticMethodToBeIgnored(access) || isNativeMethodToBeIgnored(access);
   }

   boolean isStaticMethodToBeIgnored(int access) { return false; }
   boolean isNativeMethodToBeIgnored(int access) { return isNative(access); }

   boolean isToExecuteRealImplementation(@Nullable Object mockedInstance) { return false; }
   boolean isWithRealImplementation(@Nullable Object mockedInstance) { return false; }
}
