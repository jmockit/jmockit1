/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.expectations.invocation;

import java.lang.reflect.*;
import java.util.*;
import javax.annotation.*;

import mockit.*;
import mockit.internal.expectations.invocation.InvocationResult.*;

public final class InvocationResults
{
   @Nonnull private final ExpectedInvocation invocation;
   @Nonnull private final InvocationConstraints constraints;
   @Nullable private InvocationResult currentResult;
   private InvocationResult lastResult;
   private int resultCount;

   public InvocationResults(@Nonnull ExpectedInvocation invocation, @Nonnull InvocationConstraints constraints)
   {
      this.invocation = invocation;
      this.constraints = constraints;
   }

   public void addReturnValue(@Nullable Object value)
   {
      if (value instanceof Delegate) {
         addDelegatedResult((Delegate<?>) value);
      }
      else {
         addNewReturnValueResult(value);
      }
   }

   public void addDelegatedResult(@Nonnull Delegate<?> delegate)
   {
      InvocationResult result = new DelegatedResult(invocation, delegate);
      addResult(result);
   }

   private void addNewReturnValueResult(@Nullable Object value)
   {
      InvocationResult result = new ReturnValueResult(value);
      addResult(result);
   }

   public void addReturnValueResult(@Nullable Object value) { addNewReturnValueResult(value); }

   public void addReturnValues(@Nonnull Object... values)
   {
      for (Object value : values) {
         addReturnValue(value);
      }
   }

   public void addResults(@Nonnull Object array)
   {
      int n = Array.getLength(array);

      for (int i = 0; i < n; i++) {
         Object value = Array.get(array, i);
         addConsecutiveResult(value);
      }
   }

   private void addConsecutiveResult(@Nullable Object result)
   {
      if (result instanceof Throwable) {
         addThrowable((Throwable) result);
      }
      else {
         addReturnValue(result);
      }
   }

   public void addResults(@Nonnull Iterable<?> values)
   {
      for (Object value : values) {
         addConsecutiveResult(value);
      }
   }

   public void addDeferredResults(@Nonnull Iterator<?> values)
   {
      InvocationResult result = new DeferredResults(values);
      addResult(result);
      constraints.setUnlimitedMaxInvocations();
   }

   @Nullable
   public Object executeRealImplementation(@Nonnull Object instanceToInvoke, @Nonnull Object[] invocationArgs)
      throws Throwable
   {
      if (currentResult == null) {
         currentResult = new RealImplementationResult(invocation, instanceToInvoke);
      }

      return currentResult.produceResult(invocationArgs);
   }

   public void addThrowable(@Nonnull Throwable t) { addResult(new ThrowableResult(t)); }

   private void addResult(@Nonnull InvocationResult result)
   {
      resultCount++;
      constraints.adjustMaxInvocations(resultCount);

      if (currentResult == null) {
         currentResult = result;
         lastResult = result;
      }
      else {
         lastResult.next = result;
         lastResult = result;
      }
   }

   @Nullable
   public Object produceResult(@Nullable Object invokedObject, @Nonnull Object[] invocationArgs) throws Throwable
   {
      InvocationResult resultToBeProduced = currentResult;

      if (resultToBeProduced == null) {
         return null;
      }

      InvocationResult nextResult = resultToBeProduced.next;

      if (nextResult != null) {
         currentResult = nextResult;
      }

      Object result = resultToBeProduced.produceResult(invokedObject, invocation, constraints, invocationArgs);

      return result;
   }
}
