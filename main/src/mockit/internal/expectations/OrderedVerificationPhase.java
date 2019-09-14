/*
 * Copyright (c) 2006 JMockit developers
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.expectations;

import java.util.*;
import javax.annotation.*;

import static java.util.Collections.emptyList;

import mockit.internal.expectations.invocation.*;

final class OrderedVerificationPhase extends BaseVerificationPhase
{
   @Nonnegative private final int expectationCount;
   @Nonnegative private int indexIncrement;

   OrderedVerificationPhase(@Nonnull ReplayPhase replayPhase) {
      super(replayPhase);
      discardExpectationsAndArgumentsAlreadyVerified(replayPhase.invocations);
      expectationCount = replayPhase.invocations.size();
      indexIncrement = 1;
   }

   private void discardExpectationsAndArgumentsAlreadyVerified(List<Expectation> expectationsInReplayOrder) {
      for (VerifiedExpectation verified : executionState.verifiedExpectations) {
         int i = expectationsInReplayOrder.indexOf(verified.expectation);

         if (i >= 0) {
            expectationsInReplayOrder.set(i, null);
         }
      }
   }

   @Nonnull @Override
   List<ExpectedInvocation> findExpectation(
      @Nullable Object mock, @Nonnull String mockClassDesc, @Nonnull String mockNameAndDesc, @Nonnull Object[] args
   ) {
      Expectation expectation = expectationBeingVerified();
      int i = replayIndex;

      while (i >= 0 && i < expectationCount) {
         Expectation replayExpectation = replayPhase.invocations.get(i);
         Object replayInstance = replayPhase.invocationInstances.get(i);
         Object[] replayArgs = replayPhase.invocationArguments.get(i);

         i += indexIncrement;

         if (replayExpectation == null) {
            continue;
         }

         if (!matchInstance && executionState.isToBeMatchedOnInstance(mock, mockNameAndDesc)) {
            matchInstance = true;
         }

         if (matches(mock, mockClassDesc, mockNameAndDesc, args, replayExpectation, replayInstance, replayArgs)) {
            currentExpectation = replayExpectation;
            i += 1 - indexIncrement;
            indexIncrement = 1;
            replayIndex = i;

            if (expectation != null) {
               expectation.constraints.invocationCount++;
            }

            break;
         }
      }

      return emptyList();
   }

   @Override
   void addVerifiedExpectation(@Nonnull Expectation expectation, @Nonnull Object[] args) {
      VerifiedExpectation verifiedExpectation = new VerifiedExpectation(expectation, args, argMatchers, replayIndex);
      addVerifiedExpectation(verifiedExpectation);
   }

   @Override @SuppressWarnings("OverlyComplexMethod")
   void handleInvocationCountConstraint(int minInvocations, int maxInvocations) {
      Error errorThrown = pendingError;
      pendingError = null;

      if (errorThrown != null && minInvocations > 0) {
         throw errorThrown;
      }

      Expectation verifying = expectationBeingVerified();

      if (verifying == null) {
         return;
      }

      ExpectedInvocation invocation = verifying.invocation;
      argMatchers = invocation.arguments.getMatchers();
      int invocationCount = 1;

      while (replayIndex < expectationCount) {
         Expectation replayExpectation = replayPhase.invocations.get(replayIndex);

         if (replayExpectation != null && matchesCurrentVerification(invocation, replayExpectation)) {
            invocationCount++;
            verifying.constraints.invocationCount++;

            if (invocationCount > maxInvocations) {
               if (maxInvocations >= 0) {
                  throw replayExpectation.invocation.errorForUnexpectedInvocation();
               }

               break;
            }
         }
         else if (invocationCount >= minInvocations) {
            break;
         }

         replayIndex++;
      }

      argMatchers = null;

      int n = minInvocations - invocationCount;

      if (n > 0) {
         throw invocation.errorForMissingInvocations(n, Collections.<ExpectedInvocation>emptyList());
      }

      verifyMaxInvocations(verifying, maxInvocations);
   }

   private boolean matchesCurrentVerification(@Nonnull ExpectedInvocation invocation, @Nonnull Expectation replayExpectation) {
      Object mock = invocation.instance;
      String mockClassDesc = invocation.getClassDesc();
      String mockNameAndDesc = invocation.getMethodNameAndDescription();
      Object[] args = invocation.arguments.getValues();
      matchInstance = invocation.matchInstance;

      if (executionState.isToBeMatchedOnInstance(mock, mockNameAndDesc)) {
         matchInstance = true;
      }

      Object replayInstance = replayPhase.invocationInstances.get(replayIndex);
      Object[] replayArgs = replayPhase.invocationArguments.get(replayIndex);

      return matches(mock, mockClassDesc, mockNameAndDesc, args, replayExpectation, replayInstance, replayArgs);
   }

   private void verifyMaxInvocations(@Nonnull Expectation verifying, int maxInvocations) {
      if (maxInvocations >= 0) {
         int n = verifying.constraints.invocationCount - maxInvocations;

         if (n > 0) {
            Object[] replayArgs = replayPhase.invocationArguments.get(replayIndex - 1);
            throw verifying.invocation.errorForUnexpectedInvocations(replayArgs, n);
         }
      }
   }

   @Nullable @Override
   Error endVerification() {
      if (pendingError != null) {
         return pendingError;
      }

      return super.endVerification();
   }

   @Override
   boolean shouldDiscardInformationAboutVerifiedInvocationOnceUsed() { return true; }
}