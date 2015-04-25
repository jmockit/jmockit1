/*
 * Copyright (c) 2006-2015 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.expectations.invocation;

import java.lang.reflect.*;
import java.util.*;
import javax.annotation.*;

import mockit.*;
import mockit.internal.expectations.invocation.InvocationResult.*;
import mockit.internal.util.*;

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
         addReturnValueResult(value);
      }
   }

   public void addDelegatedResult(@Nonnull Delegate<?> delegate)
   {
      InvocationResult result = new DelegatedResult(delegate);
      addResult(result);
   }

   public void addReturnValueResult(@Nullable Object value)
   {
      InvocationResult result = new ReturnValueResult(value);
      addResult(result);
   }

   public void addReturnValues(@Nonnull Object array)
   {
      int n = validateMultiValuedResult(array);

      for (int i = 0; i < n; i++) {
         Object value = Array.get(array, i);
         addReturnValue(value);
      }
   }

   private int validateMultiValuedResult(@Nonnull Object array)
   {
      int n = Array.getLength(array);

      if (n == 0) {
         reportInvalidReturnValue();
      }
      
      return n;
   }

   private void reportInvalidReturnValue()
   {
      Class<?> returnType = TypeDescriptor.getReturnType(invocation.getMethodNameAndDescription());
      throw new IllegalArgumentException("Invalid return value for method returning " + returnType);
   }

   public void addReturnValues(@Nonnull Iterable<?> values)
   {
      validateMultiValuedResult(values.iterator());

      for (Object value : values) {
         addReturnValue(value);
      }
   }

   private void validateMultiValuedResult(@Nullable Iterator<?> values)
   {
      if (values == null || !values.hasNext()) {
         reportInvalidReturnValue();
      }
   }

   public void addReturnValues(@Nonnull Object... values)
   {
      for (Object value : values) {
         addReturnValue(value);
      }
   }

   public void addResults(@Nonnull Object array)
   {
      int n = validateMultiValuedResult(array);

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
      validateMultiValuedResult(values.iterator());

      for (Object value : values) {
         addConsecutiveResult(value);
      }
   }

   public void addDeferredReturnValues(@Nonnull Iterator<?> values)
   {
      validateMultiValuedResult(values);

      InvocationResult result = new DeferredReturnValues(values);
      addResult(result);
      constraints.setUnlimitedMaxInvocations();
   }

   public void addDeferredResults(@Nonnull Iterator<?> values)
   {
      validateMultiValuedResult(values);

      InvocationResult result = new DeferredResults(values);
      addResult(result);
      constraints.setUnlimitedMaxInvocations();
   }

   @Nullable
   public Object executeRealImplementation(@Nonnull Object instanceToInvoke, @Nonnull Object[] invocationArgs)
      throws Throwable
   {
      if (currentResult == null) {
         currentResult = new RealImplementationResult(instanceToInvoke, invocation.getMethodNameAndDescription());
      }

      return currentResult.produceResult(invocationArgs);
   }

   public void addThrowable(@Nonnull Throwable t)
   {
      addResult(new ThrowableResult(t));
   }

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
      assert resultToBeProduced != null;
      InvocationResult nextResult = resultToBeProduced.next;

      if (nextResult != null) {
         currentResult = nextResult;
      }

      Object result = resultToBeProduced.produceResult(invokedObject, invocation, constraints, invocationArgs);

      return result;
   }
}
