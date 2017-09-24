/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.expectations;

import java.util.*;
import javax.annotation.*;

import mockit.internal.expectations.invocation.*;

final class ReplayPhase extends Phase
{
   @Nonnull final List<Expectation> invocations;
   @Nonnull final List<Object> invocationInstances;
   @Nonnull final List<Object[]> invocationArguments;

   ReplayPhase(@Nonnull RecordAndReplayExecution recordAndReplay)
   {
      super(recordAndReplay);
      invocations = new ArrayList<Expectation>();
      invocationInstances = new ArrayList<Object>();
      invocationArguments = new ArrayList<Object[]>();
   }

   @Override
   @Nullable
   Object handleInvocation(
      @Nullable Object mock, int mockAccess, @Nonnull String mockClassDesc, @Nonnull String mockNameAndDesc,
      @Nullable String genericSignature, boolean withRealImpl, @Nonnull Object[] args)
      throws Throwable
   {
      Expectation expectation =
         recordAndReplay.executionState.findExpectation(mock, mockClassDesc, mockNameAndDesc, args);
      Object replacementInstance = mock == null ?
         null : recordAndReplay.executionState.getReplacementInstanceForMethodInvocation(mock, mockNameAndDesc);

      if (expectation == null) {
         expectation = createExpectation(
            replacementInstance == null ? mock : replacementInstance,
            mockAccess, mockClassDesc, mockNameAndDesc, genericSignature, args);
      }
      else if (expectation.recordPhase != null) {
         registerNewInstanceAsEquivalentToOneFromRecordedConstructorInvocation(mock, expectation.invocation);
      }

      invocations.add(expectation);
      invocationInstances.add(mock);
      invocationArguments.add(args);

      expectation.constraints.incrementInvocationCount();

      if (withRealImpl && replacementInstance != null) {
         return produceResult(expectation, replacementInstance, args);
      }

      return produceResult(expectation, mock, withRealImpl, args);
   }

   @Nonnull
   private Expectation createExpectation(
      @Nullable Object mock, int mockAccess, @Nonnull String mockClassDesc, @Nonnull String mockNameAndDesc,
      @Nullable String genericSignature, @Nonnull Object[] args)
   {
      ExpectedInvocation invocation =
         new ExpectedInvocation(mock, mockAccess, mockClassDesc, mockNameAndDesc, false, genericSignature, args);
      Expectation expectation = new Expectation(invocation);
      recordAndReplay.executionState.addExpectation(expectation);
      return expectation;
   }

   private void registerNewInstanceAsEquivalentToOneFromRecordedConstructorInvocation(
      @Nullable Object mock, @Nonnull ExpectedInvocation invocation)
   {
      if (mock != null && invocation.isConstructor()) {
         Map<Object, Object> instanceMap = getInstanceMap();
         Object recordedInstance = invocation.getRecordedInstance();
         instanceMap.put(mock, recordedInstance);
      }
   }

   @Nullable
   private Object produceResult(
      @Nonnull Expectation expectation, @Nonnull Object replacementInstance, @Nonnull Object[] args)
      throws Throwable
   {
      if (expectation.recordPhase == null) {
         expectation.executedRealImplementation = true;
      }
      else if (expectation.constraints.isInvocationCountMoreThanMaximumExpected()) {
         recordAndReplay.setErrorThrown(expectation.invocation.errorForUnexpectedInvocation(args));
         return null;
      }

      return expectation.executeRealImplementation(replacementInstance, args);
   }

   @Nullable
   private Object produceResult(
      @Nonnull Expectation expectation, @Nullable Object mock, boolean withRealImpl, @Nonnull Object[] args)
      throws Throwable
   {
      boolean executeRealImpl = withRealImpl && expectation.recordPhase == null;

      if (executeRealImpl) {
         expectation.executedRealImplementation = true;
         return Void.class;
      }

      if (expectation.constraints.isInvocationCountMoreThanMaximumExpected()) {
         recordAndReplay.setErrorThrown(expectation.invocation.errorForUnexpectedInvocation(args));
         return null;
      }

      return expectation.produceResult(mock, args);
   }

   @Nullable
   private Object handleUnexpectedInvocation(
      @Nullable Object mock, @Nonnull String mockClassDesc, @Nonnull String mockNameAndDesc, boolean withRealImpl,
      @Nonnull Object[] replayArgs)
   {
      if (withRealImpl) {
         return Void.class;
      }

      recordAndReplay.setErrorThrown(
         new ExpectedInvocation(mock, mockClassDesc, mockNameAndDesc, replayArgs).errorForUnexpectedInvocation());

      return null;
   }

   @Nullable
   Error endExecution()
   {
      Error missingInvocation = getErrorForFirstExpectationThatIsMissing();
      return missingInvocation;
   }

   @Nullable
   private Error getErrorForFirstExpectationThatIsMissing()
   {
      List<Expectation> notStrictExpectations = recordAndReplay.executionState.expectations;

      // New expectations might get added to the list, so a regular loop would cause a CME.
      for (int i = 0, n = notStrictExpectations.size(); i < n; i++) {
         Expectation notStrict = notStrictExpectations.get(i);
         InvocationConstraints constraints = notStrict.constraints;

         if (constraints.isInvocationCountLessThanMinimumExpected()) {
            List<ExpectedInvocation> nonMatchingInvocations = getNonMatchingInvocations(notStrict);
            return constraints.errorForMissingExpectations(notStrict.invocation, nonMatchingInvocations);
         }
      }

      return null;
   }

   @Nonnull
   private List<ExpectedInvocation> getNonMatchingInvocations(@Nonnull Expectation unsatisfiedExpectation)
   {
      ExpectedInvocation unsatisfiedInvocation = unsatisfiedExpectation.invocation;
      List<ExpectedInvocation> nonMatchingInvocations = new ArrayList<ExpectedInvocation>();

      for (Expectation replayedExpectation : invocations) {
         ExpectedInvocation replayedInvocation = replayedExpectation.invocation;

         if (replayedExpectation != unsatisfiedExpectation && replayedInvocation.isMatch(unsatisfiedInvocation)) {
            nonMatchingInvocations.add(replayedInvocation);
         }
      }

      return nonMatchingInvocations;
   }
}
