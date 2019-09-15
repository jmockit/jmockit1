/*
 * Copyright (c) 2006 JMockit developers
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.expectations;

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

   Expectation(@Nonnull ExpectedInvocation invocation) {
      recordPhase = null;
      this.invocation = invocation;
      constraints = new InvocationConstraints(true);
   }

   Expectation(@Nonnull RecordPhase recordPhase, @Nonnull ExpectedInvocation invocation, boolean nonStrict) {
      this.recordPhase = recordPhase;
      this.invocation = invocation;
      constraints = new InvocationConstraints(nonStrict);
   }

   @Nonnull
   InvocationResults getResults() {
      if (results == null) {
         results = new InvocationResults(invocation, constraints);
      }

      return results;
   }

   @Nullable
   Object produceResult(@Nullable Object invokedObject, @Nonnull Object[] invocationArgs) throws Throwable {
      if (results == null) {
         return invocation.getDefaultValueForReturnType();
      }

      return results.produceResult(invokedObject, invocationArgs);
   }

   @Nonnull
   Class<?> getReturnType() {
      String resolvedReturnType = invocation.getSignatureWithResolvedReturnType();
      return TypeDescriptor.getReturnType(resolvedReturnType);
   }

   void addSequenceOfReturnValues(@Nonnull Object[] values) {
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
   void addResult(@Nullable Object value) {
      InvocationResults invocationResults = getResults();

      if (value == null) {
         invocationResults.addReturnValueResult(null);
      }
      else if (value instanceof Throwable) {
         invocationResults.addThrowable((Throwable) value);
      }
      else if (value instanceof mockit.Delegate) {
         invocationResults.addDelegatedResult((mockit.Delegate<?>) value);
      }
      else if (invocation.isConstructor()) {
         throw new IllegalArgumentException("Invalid assignment to result field for constructor expectation");
      }
      else {
         Class<?> rt = getReturnType();

         if (rt.isInstance(value)) {
            invocationResults.addReturnValueResult(value);
         }
         else {
            new ReturnTypeConversion(invocation, invocationResults, rt, value).addConvertedValue();
         }
      }
   }

   @Nullable
   Error verifyConstraints(
      @Nonnull ExpectedInvocation replayInvocation, @Nonnull Object[] replayArgs, int minInvocations, int maxInvocations
   ) {
      Error error = verifyConstraints(minInvocations);

      if (error != null) {
         return error;
      }

      return constraints.verifyUpperLimit(replayInvocation, replayArgs, maxInvocations);
   }

   @Nullable
   Error verifyConstraints(int minInvocations) { return constraints.verifyLowerLimit(invocation, minInvocations); }
}