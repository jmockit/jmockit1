/*
 * Copyright (c) 2006-2014 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.expectations;

import java.util.*;

import org.jetbrains.annotations.*;

import mockit.internal.expectations.invocation.*;
import mockit.internal.state.*;

final class ReplayPhase extends Phase
{
   // Fields for the handling of strict invocations:
   private int initialStrictExpectationIndexForCurrentBlock;
   int currentStrictExpectationIndex;
   @Nullable private Expectation strictExpectation;

   // Fields for the handling of non-strict invocations:
   @NotNull final List<Expectation> nonStrictInvocations;
   @NotNull final List<Object> nonStrictInvocationInstances;
   @NotNull final List<Object[]> nonStrictInvocationArguments;

   ReplayPhase(@NotNull RecordAndReplayExecution recordAndReplay)
   {
      super(recordAndReplay);
      nonStrictInvocations = new ArrayList<Expectation>();
      nonStrictInvocationInstances = new ArrayList<Object>();
      nonStrictInvocationArguments = new ArrayList<Object[]>();
      initialStrictExpectationIndexForCurrentBlock =
         Math.max(recordAndReplay.lastExpectationIndexInPreviousReplayPhase, 0);
      positionOnFirstStrictExpectation();
   }

   private void positionOnFirstStrictExpectation()
   {
      List<Expectation> expectations = getExpectations();

      if (expectations.isEmpty()) {
         currentStrictExpectationIndex = -1;
         strictExpectation = null ;
      }
      else {
         currentStrictExpectationIndex = initialStrictExpectationIndexForCurrentBlock;
         strictExpectation =
            currentStrictExpectationIndex < expectations.size() ?
               expectations.get(currentStrictExpectationIndex) : null;
      }
   }

   @NotNull private List<Expectation> getExpectations() { return recordAndReplay.executionState.expectations; }

   @Override
   @Nullable
   Object handleInvocation(
      @Nullable Object mock, int mockAccess, @NotNull String mockClassDesc, @NotNull String mockNameAndDesc,
      @Nullable String genericSignature, boolean withRealImpl, @NotNull Object[] args)
      throws Throwable
   {
      Expectation nonStrictExpectation =
         recordAndReplay.executionState.findNonStrictExpectation(mock, mockClassDesc, mockNameAndDesc, args);
      Object replacementInstance =
         recordAndReplay.executionState.getReplacementInstanceForMethodInvocation(mock, mockNameAndDesc);

      if (nonStrictExpectation == null) {
         nonStrictExpectation = createExpectationIfNonStrictInvocation(
            replacementInstance == null ? mock : replacementInstance,
            mockAccess, mockClassDesc, mockNameAndDesc, genericSignature, args);
      }
      else if (nonStrictExpectation.recordPhase != null) {
         registerNewInstanceAsEquivalentToOneFromRecordedConstructorInvocation(mock, nonStrictExpectation.invocation);
      }

      if (nonStrictExpectation != null) {
         nonStrictInvocations.add(nonStrictExpectation);
         nonStrictInvocationInstances.add(mock);
         nonStrictInvocationArguments.add(args);

         nonStrictExpectation.constraints.incrementInvocationCount();

         if (withRealImpl && replacementInstance != null) {
            return produceResult(nonStrictExpectation, replacementInstance, args);
         }

         return produceResult(nonStrictExpectation, mock, withRealImpl, args);
      }

      return handleStrictInvocation(mock, mockClassDesc, mockNameAndDesc, withRealImpl, args);
   }

   @Nullable
   private Expectation createExpectationIfNonStrictInvocation(
      @Nullable Object mock, int mockAccess, @NotNull String mockClassDesc, @NotNull String mockNameAndDesc,
      @Nullable String genericSignature, @NotNull Object[] args)
   {
      Expectation expectation = null;

      if (!TestRun.getExecutingTest().isStrictInvocation(mock, mockClassDesc, mockNameAndDesc)) {
         ExpectedInvocation invocation =
            new ExpectedInvocation(mock, mockAccess, mockClassDesc, mockNameAndDesc, false, genericSignature, args);
         expectation = new Expectation(null, invocation, true);
         recordAndReplay.executionState.addExpectation(expectation, true);
      }

      return expectation;
   }

   private void registerNewInstanceAsEquivalentToOneFromRecordedConstructorInvocation(
      @Nullable Object mock, @NotNull ExpectedInvocation invocation)
   {
      if (mock != null && invocation.isConstructor()) {
         Map<Object, Object> instanceMap = getInstanceMap();
         Object recordedInstance = invocation.getRecordedInstance();
         instanceMap.put(mock, recordedInstance);
      }
   }

   @Nullable
   private Object produceResult(
      @NotNull Expectation expectation, @NotNull Object replacementInstance, @NotNull Object[] args)
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
      @NotNull Expectation expectation, @Nullable Object mock, boolean withRealImpl, @NotNull Object[] args)
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
   private Object handleStrictInvocation(
      @Nullable Object mock, @NotNull String mockClassDesc, @NotNull String mockNameAndDesc, boolean withRealImpl,
      @NotNull Object[] replayArgs)
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

         if (invocation.isMatch(mock, mockClassDesc, mockNameAndDesc)) {
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
      @Nullable Object mock, @NotNull String mockClassDesc, @NotNull String mockNameAndDesc, boolean withRealImpl,
      @NotNull Object[] replayArgs)
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
      List<Expectation> expectations = getExpectations();

      assert strictExpectation != null;
      RecordPhase expectationBlock = strictExpectation.recordPhase;
      assert expectationBlock != null;

      currentStrictExpectationIndex++;

      strictExpectation =
         currentStrictExpectationIndex < expectations.size() ? expectations.get(currentStrictExpectationIndex) : null;

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

   private void resetInvocationCountsForStrictExpectations(@NotNull RecordPhase expectationBlock)
   {
      for (Expectation expectation : getExpectations()) {
         if (expectation.recordPhase == expectationBlock) {
            expectation.constraints.invocationCount = 0;
         }
      }
   }

   @Nullable Error endExecution()
   {
      Expectation strict = strictExpectation;
      strictExpectation = null;

      if (strict != null && strict.constraints.isInvocationCountLessThanMinimumExpected()) {
         return strict.invocation.errorForMissingInvocation();
      }

      List<Expectation> nonStrictExpectations = recordAndReplay.executionState.nonStrictExpectations;

      // New expectations might get added to the list, so a regular loop would cause a CME.
      //noinspection ForLoopReplaceableByForEach
      for (int i = 0, n = nonStrictExpectations.size(); i < n; i++) {
         Expectation nonStrict = nonStrictExpectations.get(i);
         InvocationConstraints constraints = nonStrict.constraints;

         if (constraints.isInvocationCountLessThanMinimumExpected()) {
            return constraints.errorForMissingExpectations(nonStrict.invocation);
         }
      }

      int nextStrictExpectationIndex = currentStrictExpectationIndex + 1;

      if (nextStrictExpectationIndex < getExpectations().size()) {
         Expectation nextStrictExpectation = getExpectations().get(nextStrictExpectationIndex);

         if (nextStrictExpectation.constraints.isInvocationCountLessThanMinimumExpected()) {
            return nextStrictExpectation.invocation.errorForMissingInvocation();
         }
      }

      return null;
   }
}
