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
      constraints = new InvocationConstraints(false, true);
   }

   Expectation(
      @Nullable RecordPhase recordPhase, @Nonnull ExpectedInvocation invocation, boolean strict, boolean nonStrict)
   {
      this.recordPhase = recordPhase;
      this.invocation = invocation;
      constraints = new InvocationConstraints(strict, nonStrict);
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

   void clearNextInstanceToMatchIfRecording()
   {
      if (recordPhase != null) {
         recordPhase.setNextInstanceToMatch(null);
      }
   }

   void addSequenceOfReturnValues(@Nonnull Object[] values)
   {
      if (invocation.isConstructor()) {
         throw new IllegalArgumentException("Invalid recording for a constructor");
      }

      if (invocation.getMethodNameAndDescription().endsWith(")V")) {
         throw new IllegalArgumentException("Invalid recording for a void method");
      }

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

   void addSequenceOfReturnValues(@Nullable Object firstValue, @Nullable Object[] remainingValues)
   {
      InvocationResults sequence = getResults();

      if (remainingValues == null) {
         addFirstReturnValue(firstValue);
         sequence.addReturnValue(null);
      }
      else if (remainingValues.length == 0) {
         addFirstReturnValue(firstValue);
      }
      else if (!new SequenceOfReturnValues(this, firstValue, remainingValues).addResultWithSequenceOfValues()) {
         sequence.addReturnValue(firstValue);
         sequence.addReturnValues(remainingValues);
      }
   }

   private void addFirstReturnValue(@Nullable Object firstValue)
   {
      if (firstValue == null) {
         getResults().addReturnValueResult(null);
      }
      else {
         Class<?> returnType = getReturnType();
         new ReturnTypeConversion(this, returnType, firstValue).addConvertedValueOrValues();
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
            clearNextInstanceToMatchIfRecording();
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
   Error verifyConstraints(int minInvocations)
   {
      return constraints.verifyLowerLimit(invocation, minInvocations);
   }

   @Nullable
   Object executeRealImplementation(@Nonnull Object replacementInstance, @Nonnull Object[] args) throws Throwable
   {
      return getResults().executeRealImplementation(replacementInstance, args);
   }

   boolean isRedundantRecordedExpectation(@Nonnull Expectation verification)
   {
      return recordPhase != null && constraints.minInvocations > 0 && invocation.isRedundant(verification.invocation);
   }
}
