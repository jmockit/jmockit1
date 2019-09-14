/*
 * Copyright (c) 2006 JMockit developers
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.expectations;

import java.util.*;
import javax.annotation.*;

import mockit.internal.expectations.invocation.*;

class UnorderedVerificationPhase extends BaseVerificationPhase
{
   @Nonnull private final List<VerifiedExpectation> verifiedExpectations;

   UnorderedVerificationPhase(@Nonnull ReplayPhase replayPhase) {
      super(replayPhase);
      verifiedExpectations = new ArrayList<>();
   }

   @Nonnull @Override
   final List<ExpectedInvocation> findExpectation(
      @Nullable Object mock, @Nonnull String mockClassDesc, @Nonnull String mockNameAndDesc, @Nonnull Object[] args
   ) {
      if (!matchInstance && executionState.isToBeMatchedOnInstance(mock, mockNameAndDesc)) {
         matchInstance = true;
      }

      replayIndex = -1;
      List<Expectation> expectationsInReplayOrder = replayPhase.invocations;
      Expectation verification = currentVerification;
      List<ExpectedInvocation> matchingInvocationsWithDifferentArgs = new ArrayList<>();

      for (int i = 0, n = expectationsInReplayOrder.size(); i < n; i++) {
         Expectation replayExpectation = expectationsInReplayOrder.get(i);
         Object replayInstance = replayPhase.invocationInstances.get(i);
         Object[] replayArgs = replayPhase.invocationArguments.get(i);

         if (matches(mock, mockClassDesc, mockNameAndDesc, args, replayExpectation, replayInstance, replayArgs)) {
            replayIndex = i;

            if (verification != null) {
               verification.constraints.invocationCount++;
            }

            currentExpectation = replayExpectation;
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
   private Error verifyConstraints(@Nonnull Expectation verification) {
      ExpectedInvocation lastInvocation = replayPhase.invocations.get(replayIndex).invocation;
      Object[] lastArgs = replayPhase.invocationArguments.get(replayIndex);
      return verification.verifyConstraints(lastInvocation, lastArgs, 1, -1);
   }

   @Override
   final void addVerifiedExpectation(@Nonnull Expectation expectation, @Nonnull Object[] args) {
      VerifiedExpectation verifiedExpectation = new VerifiedExpectation(expectation, args, argMatchers, -1);
      addVerifiedExpectation(verifiedExpectation);
      verifiedExpectations.add(verifiedExpectation);
   }

   @Override
   final void handleInvocationCountConstraint(int minInvocations, int maxInvocations) {
      pendingError = null;

      Expectation verifying = currentVerification;

      if (verifying == null) {
         return;
      }

      Error errorThrown;

      if (replayIndex >= 0) {
         ExpectedInvocation replayInvocation = replayPhase.invocations.get(replayIndex).invocation;
         Object[] replayArgs = replayPhase.invocationArguments.get(replayIndex);
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