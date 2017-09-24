/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.expectations;

import java.util.*;
import javax.annotation.*;

import mockit.internal.expectations.invocation.*;
import mockit.internal.util.*;

final class Expectation
{
   @Nullable final RecordPhase recordPhase;
   @Nonnull final ExpectedInvocation invocation;
   @Nonnull final InvocationConstraints constraints;
   @Nullable private InvocationResults results;
   boolean executedRealImplementation;

   Expectation(@Nonnull ExpectedInvocation invocation)
   {
      recordPhase = null;
      this.invocation = invocation;
      constraints = new InvocationConstraints(true);
   }

   Expectation(@Nullable RecordPhase recordPhase, @Nonnull ExpectedInvocation invocation, boolean nonStrict)
   {
      this.recordPhase = recordPhase;
      this.invocation = invocation;
      constraints = new InvocationConstraints(nonStrict);
   }

   @Nonnull
   InvocationResults getResults()
   {
      if (results == null) {
         results = new InvocationResults(invocation, constraints);
      }

      return results;
   }

   @Nullable
   Object produceResult(@Nullable Object invokedObject, @Nonnull Object[] invocationArgs) throws Throwable
   {
      if (results == null) {
         return invocation.getDefaultValueForReturnType();
      }

      return results.produceResult(invokedObject, invocationArgs);
   }

   @Nonnull
   Class<?> getReturnType()
   {
      String resolvedReturnType = invocation.getSignatureWithResolvedReturnType();
      return TypeDescriptor.getReturnType(resolvedReturnType);
   }

   void addSequenceOfReturnValues(@Nonnull Object[] values)
   {
      int n = values.length - 1;
      Object firstValue = values[0];
      Object[] remainingValues = new Object[n];
      System.arraycopy(values, 1, remainingValues, 0, n);

      InvocationResults sequence = getResults();

      if (!new SequenceOfReturnValues(this, firstValue, remainingValues).addResultWithSequenceOfValues()) {
         sequence.addReturnValue(firstValue);
         sequence.addReturnValues(remainingValues);
      }
   }

   @SuppressWarnings("UnnecessaryFullyQualifiedName")
   void addResult(@Nullable Object value)
   {
      if (value == null) {
         getResults().addReturnValueResult(null);
      }
      else if (isReplacementInstance(value)) {
         if (recordPhase != null) {
            Map<Object, Object> replacementMap = recordPhase.getReplacementMap();
            replacementMap.put(invocation.instance, value);
         }

         invocation.replacementInstance = value;
      }
      else if (value instanceof Throwable) {
         getResults().addThrowable((Throwable) value);
      }
      else if (value instanceof mockit.Delegate) {
         getResults().addDelegatedResult((mockit.Delegate<?>) value);
      }
      else {
         Class<?> rt = getReturnType();

         if (rt.isInstance(value)) {
            getResults().addReturnValueResult(value);
         }
         else {
            new ReturnTypeConversion(this, rt, value).addConvertedValue();
         }
      }
   }

   private boolean isReplacementInstance(@Nonnull Object value)
   {
      return invocation.isConstructor() && value.getClass().isInstance(invocation.instance);
   }

   @Nullable
   Error verifyConstraints(
      @Nonnull ExpectedInvocation replayInvocation, @Nonnull Object[] replayArgs,
      int minInvocations, int maxInvocations)
   {
      Error error = verifyConstraints(minInvocations);

      if (error != null) {
         return error;
      }

      return constraints.verifyUpperLimit(replayInvocation, replayArgs, maxInvocations);
   }

   @Nullable
   Error verifyConstraints(int minInvocations) { return constraints.verifyLowerLimit(invocation, minInvocations); }

   @Nullable
   Object executeRealImplementation(@Nonnull Object replacementInstance, @Nonnull Object[] args) throws Throwable
   {
      return getResults().executeRealImplementation(replacementInstance, args);
   }
}
