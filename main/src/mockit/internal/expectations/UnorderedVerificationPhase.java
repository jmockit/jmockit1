/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.expectations;

import java.util.*;
import javax.annotation.*;

import mockit.internal.expectations.invocation.*;

final class UnorderedVerificationPhase extends BaseVerificationPhase
{
   @Nonnull final List<VerifiedExpectation> verifiedExpectations;

   UnorderedVerificationPhase(
      @Nonnull RecordAndReplayExecution recordAndReplay,
      @Nonnull List<Expectation> expectationsInReplayOrder,
      @Nonnull List<Object> invocationInstancesInReplayOrder,
      @Nonnull List<Object[]> invocationArgumentsInReplayOrder)
   {
      super(
         recordAndReplay, expectationsInReplayOrder,
         invocationInstancesInReplayOrder, invocationArgumentsInReplayOrder);
      verifiedExpectations = new ArrayList<VerifiedExpectation>();
   }

   @Nonnull @Override
   protected List<ExpectedInvocation> findExpectation(
      @Nullable Object mock, @Nonnull String mockClassDesc, @Nonnull String mockNameAndDesc, @Nonnull Object[] args)
   {
      if (!matchInstance && recordAndReplay.executionState.isToBeMatchedOnInstance(mock, mockNameAndDesc)) {
         matchInstance = true;
      }

      replayIndex = -1;
      Expectation verification = expectationBeingVerified();
      List<ExpectedInvocation> matchingInvocationsWithDifferentArgs = new ArrayList<ExpectedInvocation>();

      for (int i = 0, n = expectationsInReplayOrder.size(); i < n; i++) {
         Expectation replayExpectation = expectationsInReplayOrder.get(i);
         Object replayInstance = invocationInstancesInReplayOrder.get(i);
         Object[] replayArgs = invocationArgumentsInReplayOrder.get(i);

         if (matches(mock, mockClassDesc, mockNameAndDesc, args, replayExpectation, replayInstance, replayArgs)) {
            replayIndex = i;

            if (verification != null) {
               verification.constraints.invocationCount++;
            }

            currentExpectation = replayExpectation;
            mapNewInstanceToReplacementIfApplicable(mock);
         }
         else if (matchingInvocationWithDifferentArgs != null) {
            matchingInvocationsWithDifferentArgs.add(matchingInvocationWithDifferentArgs);
         }
      }

      if (verification != null && replayIndex >= 0) {
         pendingError = verifyConstraints(verification);
      }

      return matchingInvocationsWithDifferentArgs;
   }

   @Nullable
   private Error verifyConstraints(@Nonnull Expectation verification)
   {
      ExpectedInvocation lastInvocation = expectationsInReplayOrder.get(replayIndex).invocation;
      Object[] lastArgs = invocationArgumentsInReplayOrder.get(replayIndex);
      return verification.verifyConstraints(lastInvocation, lastArgs, 1, -1);
   }

   @Override
   void addVerifiedExpectation(@Nonnull VerifiedExpectation verifiedExpectation)
   {
      super.addVerifiedExpectation(verifiedExpectation);
      verifiedExpectations.add(verifiedExpectation);
   }

   @Override
   public void handleInvocationCountConstraint(int minInvocations, int maxInvocations)
   {
      pendingError = null;

      Expectation verifying = expectationBeingVerified();

      if (verifying == null) {
         return;
      }

      Error errorThrown;

      if (replayIndex >= 0) {
         ExpectedInvocation replayInvocation = expectationsInReplayOrder.get(replayIndex).invocation;
         Object[] replayArgs = invocationArgumentsInReplayOrder.get(replayIndex);
         errorThrown = verifying.verifyConstraints(replayInvocation, replayArgs, minInvocations, maxInvocations);
      }
      else {
         errorThrown = verifying.verifyConstraints(minInvocations);
      }

      if (errorThrown != null) {
         throw errorThrown;
      }
   }
}
