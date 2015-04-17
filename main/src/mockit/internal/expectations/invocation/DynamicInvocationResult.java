/*
 * Copyright (c) 2006-2014 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.expectations.invocation;

import java.lang.reflect.*;
import java.util.concurrent.locks.*;

import org.jetbrains.annotations.*;

import mockit.*;
import mockit.internal.expectations.*;
import mockit.internal.state.*;
import mockit.internal.util.*;

abstract class DynamicInvocationResult extends InvocationResult
{
   private static final Object[] NO_ARGS = {};

   @NotNull private final Object targetObject;
   @NotNull final Method methodToInvoke;
   private int numberOfRegularParameters;
   private boolean hasInvocationParameter;

   DynamicInvocationResult(@NotNull Object targetObject, @NotNull Method methodToInvoke)
   {
      this.targetObject = targetObject;
      this.methodToInvoke = methodToInvoke;
      determineWhetherMethodToInvokeHasInvocationParameter();
   }

   private void determineWhetherMethodToInvokeHasInvocationParameter()
   {
      Class<?>[] parameters = methodToInvoke.getParameterTypes();
      int n = parameters.length;

      for (int i = 0; i < n; i++) {
         if (parameters[i] == Invocation.class) {
            if (i > 0) {
               throw new IllegalArgumentException(
                  "Delegate method with Invocation parameter not as first one:\n" + methodToInvoke);
            }

            hasInvocationParameter = true;
         }
      }

      numberOfRegularParameters = hasInvocationParameter ? n - 1 : n;
   }

   @Nullable
   public final Object invokeMethodOnTargetObject(
      @Nullable Object mockOrRealObject, @NotNull ExpectedInvocation invocation,
      @NotNull InvocationConstraints constraints, @NotNull Object[] args)
   {
      Object[] delegateArgs = numberOfRegularParameters == 0 ? NO_ARGS : args;
      Object result;

      if (hasInvocationParameter) {
         result = invokeMethodWithContext(mockOrRealObject, invocation, constraints, args, delegateArgs);
      }
      else {
         result = executeMethodToInvoke(delegateArgs);
      }

      return result;
   }

   @Nullable
   private Object invokeMethodWithContext(
      @Nullable Object mockOrRealObject, @NotNull ExpectedInvocation expectedInvocation,
      @NotNull InvocationConstraints constraints, @NotNull Object[] invokedArgs, @NotNull Object[] delegateArgs)
   {
      DelegateInvocation invocation =
         new DelegateInvocation(mockOrRealObject, invokedArgs, expectedInvocation, constraints);
      Object[] delegateArgsWithInvocation = ParameterReflection.argumentsWithExtraFirstValue(delegateArgs, invocation);

      try {
         Object result = executeMethodToInvoke(delegateArgsWithInvocation);
         return
            expectedInvocation.isConstructor() &&
            TestRun.getExecutingTest().isProceedingIntoRealImplementation() ?
               Void.class : result;
      }
      finally {
         constraints.setLimits(invocation.getMinInvocations(), invocation.getMaxInvocations());
      }
   }

   @Nullable protected final Object executeMethodToInvoke(@NotNull Object[] args)
   {
      ReentrantLock reentrantLock = RecordAndReplayExecution.RECORD_OR_REPLAY_LOCKS.get();

      if (!reentrantLock.isHeldByCurrentThread()) {
         return MethodReflection.invoke(targetObject, methodToInvoke, args);
      }

      reentrantLock.unlock();

      try {
         return MethodReflection.invoke(targetObject, methodToInvoke, args);
      }
      finally {
         //noinspection LockAcquiredButNotSafelyReleased
         reentrantLock.lock();
      }
   }
}