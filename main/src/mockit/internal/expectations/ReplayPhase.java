/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.expectations;

import java.util.*;
import javax.annotation.*;

import mockit.internal.expectations.invocation.*;
import mockit.internal.state.*;

final class ReplayPhase extends Phase
{
   // Fields for the handling of strict invocations:
   private int initialStrictExpectationIndexForCurrentBlock;
   int currentStrictExpectationIndex;
   @Nullable private Expectation strictExpectation;

   // Fields for the handling of invocations that are not strict:
   @Nonnull final List<Expectation> invocations;
   @Nonnull final List<Object> invocationInstances;
   @Nonnull final List<Object[]> invocationArguments;

   ReplayPhase(@Nonnull RecordAndReplayExecution recordAndReplay)
   {
      super(recordAndReplay);
      invocations = new ArrayList<Expectation>();
      invocationInstances = new ArrayList<Object>();
      invocationArguments = new ArrayList<Object[]>();
      initialStrictExpectationIndexForCurrentBlock =
         Math.max(recordAndReplay.lastExpectationIndexInPreviousReplayPhase, 0);
      positionOnFirstStrictExpectation();
   }

   private void positionOnFirstStrictExpectation()
   {
      List<Expectation> strictExpectations = getStrictExpectations();

      if (strictExpectations.isEmpty()) {
         currentStrictExpectationIndex = -1;
         strictExpectation = null ;
      }
      else {
         currentStrictExpectationIndex = initialStrictExpectationIndexForCurrentBlock;
         strictExpectation =
            currentStrictExpectationIndex < strictExpectations.size() ?
               strictExpectations.get(currentStrictExpectationIndex) : null;
      }
   }

   @Nonnull
   private List<Expectation> getStrictExpectations() { return recordAndReplay.executionState.strictExpectations; }

   @Override
   @Nullable
   Object handleInvocation(
      @Nullable Object mock, int mockAccess, @Nonnull String mockClassDesc, @Nonnull String mockNameAndDesc,
      @Nullable String genericSignature, boolean withRealImpl, @Nonnull Object[] args)
      throws Throwable
   {
      Expectation notStrictExpectation =
         recordAndReplay.executionState.findNotStrictExpectation(mock, mockClassDesc, mockNameAndDesc, args);
      Object replacementInstance = mock == null ?
         null : recordAndReplay.executionState.getReplacementInstanceForMethodInvocation(mock, mockNameAndDesc);

      if (notStrictExpectation == null) {
         notStrictExpectation = createExpectationIfNotStrictInvocation(
            replacementInstance == null ? mock : replacementInstance,
            mockAccess, mockClassDesc, mockNameAndDesc, genericSignature, args);
      }
      else if (notStrictExpectation.recordPhase != null) {
         registerNewInstanceAsEquivalentToOneFromRecordedConstructorInvocation(mock, notStrictExpectation.invocation);
      }

      if (notStrictExpectation != null) {
         invocations.add(notStrictExpectation);
         invocationInstances.add(mock);
         invocationArguments.add(args);

         notStrictExpectation.constraints.incrementInvocationCount();

         if (withRealImpl && replacementInstance != null) {
            return produceResult(notStrictExpectation, replacementInstance, args);
         }

         return produceResult(notStrictExpectation, mock, withRealImpl, args);
      }

      return handleStrictInvocation(mock, mockClassDesc, mockNameAndDesc, withRealImpl, args);
   }

   @Nullable
   private Expectation createExpectationIfNotStrictInvocation(
      @Nullable Object mock, int mockAccess, @Nonnull String mockClassDesc, @Nonnull String mockNameAndDesc,
      @Nullable String genericSignature, @Nonnull Object[] args)
   {
      Expectation expectation = null;

      if (!TestRun.getExecutingTest().isStrictInvocation(mock, mockClassDesc, mockNameAndDesc)) {
         ExpectedInvocation invocation =
            new ExpectedInvocation(mock, mockAccess, mockClassDesc, mockNameAndDesc, false, genericSignature, args);
         expectation = new Expectation(invocation);
         recordAndReplay.executionState.addExpectation(expectation, false);
      }

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

   @Nullable @SuppressWarnings({"OverlyComplexMethod", "OverlyLongMethod"})
   private Object handleStrictInvocation(
      @Nullable Object mock, @Nonnull String mockClassDesc, @Nonnull String mockNameAndDesc, boolean withRealImpl,
      @Nonnull Object[] replayArgs)
      throws Throwable
   {
      Map<Object, Object> instanceMap = getInstanceMap();

      while (true) {
         Expectation expectation = strictExpectation;

         if (expectation == null) {
            return handleUnexpectedInvocation(mock, mockClassDesc, mockNameAndDesc, withRealImpl, replayArgs);
         }

         ExpectedInvocation invocation = expectation.invocation;
         InvocationConstraints constraints = expectation.constraints;

         if (invocation.isMatch(mock, mockClassDesc, mockNameAndDesc, null)) {
            registerNewInstanceAsEquivalentToOneFromRecordedConstructorInvocation(mock, invocation);

            Error error = invocation.assertThatArgumentsMatch(replayArgs, instanceMap);

            if (error != null) {
               if (constraints.isInvocationCountInExpectedRange()) {
                  moveToNextExpectation();
                  continue;
               }

               if (withRealImpl) {
                  return Void.class;
               }

               recordAndReplay.setErrorThrown(error);
               return null;
            }

            constraints.incrementInvocationCount();

            if (constraints.isInvocationCountAtMaximumAllowed()) {
               moveToNextExpectation();
            }
            else if (constraints.isInvocationCountMoreThanMaximumExpected()) {
               recordAndReplay.setErrorThrown(invocation.errorForUnexpectedInvocation(replayArgs));
               return null;
            }

            return expectation.produceResult(mock, replayArgs);
         }
         else if (constraints.isInvocationCountInExpectedRange()) {
            moveToNextExpectation();
         }
         else if (withRealImpl) {
            return Void.class;
         }
         else {
            recordAndReplay.setErrorThrown(
               invocation.errorForUnexpectedInvocation(mock, mockClassDesc, mockNameAndDesc, replayArgs));
            return null;
         }
      }
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

   private void moveToNextExpectation()
   {
      List<Expectation> strictExpectations = getStrictExpectations();

      assert strictExpectation != null;
      RecordPhase expectationBlock = strictExpectation.recordPhase;
      assert expectationBlock != null;

      currentStrictExpectationIndex++;

      strictExpectation =
         currentStrictExpectationIndex < strictExpectations.size() ?
            strictExpectations.get(currentStrictExpectationIndex) : null;

      if (expectationBlock.numberOfIterations <= 1) {
         if (strictExpectation != null && strictExpectation.recordPhase != expectationBlock) {
            initialStrictExpectationIndexForCurrentBlock = currentStrictExpectationIndex;
         }
      }
      else if (strictExpectation == null || strictExpectation.recordPhase != expectationBlock) {
         expectationBlock.numberOfIterations--;
         positionOnFirstStrictExpectation();
         resetInvocationCountsForStrictExpectations(expectationBlock);
      }
   }

   private void resetInvocationCountsForStrictExpectations(@Nonnull RecordPhase expectationBlock)
   {
      for (Expectation expectation : getStrictExpectations()) {
         if (expectation.recordPhase == expectationBlock) {
            expectation.constraints.invocationCount = 0;
         }
      }
   }

   @Nullable
   Error endExecution()
   {
      Expectation strict = strictExpectation;
      strictExpectation = null;

      Error missingInvocation = getErrorIfStrictExpectationIsMissing(strict);

      if (missingInvocation == null) {
         missingInvocation = getErrorForFirstNotStrictExpectationThatIsMissing();

         if (missingInvocation == null) {
            missingInvocation = getErrorIfNextStrictExpectationIsMissing();
         }
      }

      return missingInvocation;
   }

   @Nullable
   private Error getErrorIfStrictExpectationIsMissing(@Nullable Expectation strict)
   {
      if (strict != null && strict.constraints.isInvocationCountLessThanMinimumExpected()) {
         return strict.invocation.errorForMissingInvocation(Collections.<ExpectedInvocation>emptyList());
      }

      return null;
   }

   @Nullable
   private Error getErrorForFirstNotStrictExpectationThatIsMissing()
   {
      List<Expectation> notStrictExpectations = recordAndReplay.executionState.notStrictExpectations;

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

   @Nullable
   private Error getErrorIfNextStrictExpectationIsMissing()
   {
      int nextStrictExpectationIndex = currentStrictExpectationIndex + 1;
      List<Expectation> strictExpectations = getStrictExpectations();

      if (nextStrictExpectationIndex < strictExpectations.size()) {
         Expectation nextStrictExpectation = strictExpectations.get(nextStrictExpectationIndex);

         if (nextStrictExpectation.constraints.isInvocationCountLessThanMinimumExpected()) {
            return
               nextStrictExpectation.invocation.errorForMissingInvocation(Collections.<ExpectedInvocation>emptyList());
         }
      }

      return null;
   }
}
