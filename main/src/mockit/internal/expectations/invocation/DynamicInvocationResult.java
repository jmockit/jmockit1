/*
 * Copyright (c) 2006-2015 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.expectations.invocation;

import java.lang.reflect.*;
import java.util.concurrent.locks.*;
import javax.annotation.*;

import mockit.*;
import mockit.internal.expectations.*;
import mockit.internal.state.*;
import mockit.internal.util.*;

abstract class DynamicInvocationResult extends InvocationResult
{
   private static final Object[] NO_ARGS = {};

   @Nonnull private final Object targetObject;
   @Nonnull final Method methodToInvoke;
   private int numberOfRegularParameters;
   private boolean hasInvocationParameter;

   DynamicInvocationResult(@Nonnull Object targetObject, @Nonnull Method methodToInvoke)
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
      @Nullable Object mockOrRealObject, @Nonnull ExpectedInvocation invocation,
      @Nonnull InvocationConstraints constraints, @Nonnull Object[] args)
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
      @Nullable Object mockOrRealObject, @Nonnull ExpectedInvocation expectedInvocation,
      @Nonnull InvocationConstraints constraints, @Nonnull Object[] invokedArgs, @Nonnull Object[] delegateArgs)
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

   @Nullable
   protected final Object executeMethodToInvoke(@Nonnull Object[] args)
   {
      ReentrantLock reentrantLock = RecordAndReplayExecution.RECORD_OR_REPLAY_LOCK;

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