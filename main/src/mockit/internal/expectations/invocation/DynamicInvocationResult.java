/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.expectations.invocation;

import java.lang.reflect.*;
import java.util.concurrent.locks.*;
import javax.annotation.*;

import mockit.*;
import mockit.external.asm.JavaType;
import mockit.internal.expectations.*;
import mockit.internal.reflection.*;
import mockit.internal.state.*;
import mockit.internal.util.*;

abstract class DynamicInvocationResult extends InvocationResult
{
   private static final Object[] NO_ARGS = {};

   @Nonnull private final ExpectedInvocation invocation;
   @Nonnull private final Object targetObject;
   @Nonnull final Method methodToInvoke;
   @Nonnull private final Class<?> targetReturnType;
   private final boolean hasInvocationParameter;
   private final int numberOfRegularParameters;

   DynamicInvocationResult(
      @Nonnull ExpectedInvocation invocation, @Nonnull Object targetObject, @Nonnull Method methodToInvoke)
   {
      this.invocation = invocation;
      this.targetObject = targetObject;
      this.methodToInvoke = methodToInvoke;
      targetReturnType = getTargetReturnType();

      Class<?>[] parameters = methodToInvoke.getParameterTypes();
      int n = parameters.length;

      hasInvocationParameter = n > 0 && parameters[0] == Invocation.class;
      numberOfRegularParameters = hasInvocationParameter ? n - 1 : n;
   }

   @Nonnull
   private Class<?> getTargetReturnType()
   {
      JavaType returnType = JavaType.getReturnType(invocation.getMethodNameAndDescription());
      Class<?> classForType = TypeDescriptor.getClassForType(returnType);
      return classForType;
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
      Invocation invocation = new DelegateInvocation(mockOrRealObject, invokedArgs, expectedInvocation, constraints);
      Object[] delegateArgsWithInvocation = ParameterReflection.argumentsWithExtraFirstValue(delegateArgs, invocation);

      Object result = executeMethodToInvoke(delegateArgsWithInvocation);

      return
         expectedInvocation.isConstructor() &&
         TestRun.getExecutingTest().isProceedingIntoRealImplementation() ? Void.class : result;
   }

   @Nullable
   protected final Object executeMethodToInvoke(@Nonnull Object[] args)
   {
      ReentrantLock reentrantLock = RecordAndReplayExecution.RECORD_OR_REPLAY_LOCK;

      if (!reentrantLock.isHeldByCurrentThread()) {
         return executeTargetMethod(args);
      }

      reentrantLock.unlock();

      try {
         return executeTargetMethod(args);
      }
      finally {
         //noinspection LockAcquiredButNotSafelyReleased
         reentrantLock.lock();
      }
   }

   @Nullable
   private Object executeTargetMethod(@Nonnull Object[] args)
   {
      Object returnValue = MethodReflection.invoke(targetObject, methodToInvoke, args);
      Class<?> fromReturnType = methodToInvoke.getReturnType();

      if (returnValue == null || targetReturnType.isAssignableFrom(fromReturnType)) {
         if (fromReturnType == void.class && fromReturnType != targetReturnType && targetReturnType.isPrimitive()) {
            String returnTypeName = targetReturnType.getName().replace("java.lang.", "");
            MethodFormatter methodDesc =
               new MethodFormatter(invocation.getClassDesc(), invocation.getMethodNameAndDescription());
            String msg = "void return type incompatible with return type " + returnTypeName + " of " + methodDesc;
            throw new IllegalArgumentException(msg);
         }

         return returnValue;
      }

      ReturnTypeConversion typeConversion = new ReturnTypeConversion(invocation, targetReturnType, returnValue);
      @Nonnull Object convertedReturnValue = typeConversion.getConvertedValue();
      return convertedReturnValue;
   }
}