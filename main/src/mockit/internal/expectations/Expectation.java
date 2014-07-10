/*
 * Copyright (c) 2006-2014 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.expectations;

import org.jetbrains.annotations.*;

import mockit.internal.expectations.invocation.*;
import mockit.internal.state.*;
import mockit.internal.util.*;

final class Expectation
{
   @Nullable final RecordPhase recordPhase;
   @NotNull final ExpectedInvocation invocation;
   @NotNull final InvocationConstraints constraints;
   @Nullable private InvocationResults results;
   boolean executedRealImplementation;

   Expectation(@Nullable RecordPhase recordPhase, @NotNull ExpectedInvocation invocation, boolean nonStrict)
   {
      this.recordPhase = recordPhase;
      this.invocation = invocation;
      constraints = new InvocationConstraints(nonStrict);
   }

   @NotNull InvocationResults getResults()
   {
      if (results == null) {
         results = new InvocationResults(invocation, constraints);
      }

      return results;
   }

   @Nullable
   Object produceResult(@Nullable Object invokedObject, @NotNull Object[] invocationArgs) throws Throwable
   {
      if (results == null) {
         return invocation.getDefaultValueForReturnType(null);
      }

      return results.produceResult(invokedObject, invocationArgs);
   }

   @NotNull Class<?> getReturnType()
   {
      return TypeDescriptor.getReturnType(invocation.getSignatureWithResolvedReturnType());
   }

   void substituteCascadedMockToBeReturnedIfNeeded()
   {
      Object cascadedMock = invocation.getCascadedMock();

      if (cascadedMock != null) {
         TestRun.getExecutingTest().discardCascadedMockWhenInjectable(cascadedMock);

         if (recordPhase != null) {
            recordPhase.setNextInstanceToMatch(null);
         }
      }
   }

   void addSequenceOfReturnValues(@Nullable Object firstValue, @Nullable Object[] remainingValues)
   {
      InvocationResults sequence = getResults();

      if (remainingValues == null || remainingValues.length == 0) {
         if (firstValue == null) {
            sequence.addReturnValueResult(null);
         }
         else {
            Class<?> returnType = getReturnType();
            new ReturnTypeConversion(this, returnType, firstValue).addConvertedValueOrValues();
         }
      }
      else if (!new SequenceOfReturnValues(this, firstValue, remainingValues).addResultWithSequenceOfValues()) {
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
            substituteCascadedMockToBeReturnedIfNeeded();
            getResults().addReturnValueResult(value);
         }
         else {
            new ReturnTypeConversion(this, rt, value).addConvertedValue();
         }
      }
   }

   private boolean isReplacementInstance(@NotNull Object value)
   {
      return invocation.isConstructor() && value.getClass().isInstance(invocation.instance);
   }

   void setCustomErrorMessage(@Nullable CharSequence message) { invocation.customErrorMessage = message; }

   @Nullable
   Error verifyConstraints(
      @NotNull ExpectedInvocation replayInvocation, @NotNull Object[] replayArgs,
      int minInvocations, int maxInvocations)
   {
      Error error = verifyConstraints(minInvocations);

      if (error != null) {
         return error;
      }

      return constraints.verifyUpperLimit(replayInvocation, replayArgs, maxInvocations);
   }

   @Nullable Error verifyConstraints(int minInvocations)
   {
      return constraints.verifyLowerLimit(invocation, minInvocations);
   }

   @Nullable
   Object executeRealImplementation(@NotNull Object replacementInstance, @NotNull Object[] args) throws Throwable
   {
      return getResults().executeRealImplementation(replacementInstance, args);
   }
}
