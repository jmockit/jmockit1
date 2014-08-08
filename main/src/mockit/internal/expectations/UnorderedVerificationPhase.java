/*
 * Copyright (c) 2006-2014 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.expectations;

import java.util.*;

import org.jetbrains.annotations.*;

import mockit.internal.expectations.invocation.*;

final class UnorderedVerificationPhase extends BaseVerificationPhase
{
   @NotNull final List<VerifiedExpectation> verifiedExpectations;

   UnorderedVerificationPhase(
      @NotNull RecordAndReplayExecution recordAndReplay,
      @NotNull List<Expectation> expectationsInReplayOrder,
      @NotNull List<Object> invocationInstancesInReplayOrder,
      @NotNull List<Object[]> invocationArgumentsInReplayOrder)
   {
      super(
         recordAndReplay, expectationsInReplayOrder,
         invocationInstancesInReplayOrder, invocationArgumentsInReplayOrder);
      verifiedExpectations = new ArrayList<VerifiedExpectation>();
   }

   @Override
   protected void findNonStrictExpectation(
      @Nullable Object mock, @NotNull String mockClassDesc, @NotNull String mockNameAndDesc, @NotNull Object[] args)
   {
      if (!matchInstance && recordAndReplay.executionState.isToBeMatchedOnInstance(mock, mockNameAndDesc)) {
         matchInstance = true;
      }

      replayIndex = -1;

      for (int i = 0, n = expectationsInReplayOrder.size(); i < n; i++) {
         Expectation replayExpectation = expectationsInReplayOrder.get(i);
         Object replayInstance = invocationInstancesInReplayOrder.get(i);
         Object[] replayArgs = invocationArgumentsInReplayOrder.get(i);

         if (matches(mock, mockClassDesc, mockNameAndDesc, args, replayExpectation, replayInstance, replayArgs)) {
            replayIndex = i;
            expectationBeingVerified().constraints.invocationCount++;
            currentExpectation = replayExpectation;
            mapNewInstanceToReplacementIfApplicable(mock);
         }
      }

      if (replayIndex >= 0) {
         pendingError = verifyConstraints();
      }
   }

   @Nullable
   private Error verifyConstraints()
   {
      ExpectedInvocation lastInvocation = expectationsInReplayOrder.get(replayIndex).invocation;
      Object[] lastArgs = invocationArgumentsInReplayOrder.get(replayIndex);
      int minInvocations = numberOfIterations > 0 ? numberOfIterations : 1;
      int maxInvocations = numberOfIterations > 0 ? numberOfIterations : -1;

      return expectationBeingVerified().verifyConstraints(lastInvocation, lastArgs, minInvocations, maxInvocations);
   }

   @Override
   void addVerifiedExpectation(@NotNull VerifiedExpectation verifiedExpectation)
   {
      super.addVerifiedExpectation(verifiedExpectation);
      verifiedExpectations.add(verifiedExpectation);
   }

   @Override
   public void handleInvocationCountConstraint(int minInvocations, int maxInvocations)
   {
      pendingError = null;

      Expectation verifying = expectationBeingVerified();
      int multiplier = numberOfIterations <= 1 ? 1 : numberOfIterations;
      int iteratedMin = multiplier * minInvocations;
      Error errorThrown;

      if (replayIndex >= 0) {
         ExpectedInvocation replayInvocation = expectationsInReplayOrder.get(replayIndex).invocation;
         Object[] replayArgs = invocationArgumentsInReplayOrder.get(replayIndex);
         int iteratedMax = multiplier * maxInvocations;
         errorThrown = verifying.verifyConstraints(replayInvocation, replayArgs, iteratedMin, iteratedMax);
      }
      else {
         errorThrown = verifying.verifyConstraints(iteratedMin);
      }

      if (errorThrown != null) {
         throw errorThrown;
      }
   }

   @Nullable
   VerifiedExpectation firstExpectationVerified()
   {
      VerifiedExpectation first = null;

      for (VerifiedExpectation expectation : verifiedExpectations) {
         if (first == null || expectation.replayIndex < first.replayIndex) {
            first = expectation;
         }
      }

      return first;
   }
}
