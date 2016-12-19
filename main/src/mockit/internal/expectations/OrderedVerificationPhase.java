/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.expectations;

import java.util.*;
import javax.annotation.*;

import static java.util.Collections.emptyList;

import mockit.internal.expectations.invocation.*;

public final class OrderedVerificationPhase extends BaseVerificationPhase
{
   private final int expectationCount;
   private ExpectedInvocation unverifiedInvocationLeftBehind;
   private ExpectedInvocation unverifiedInvocationPrecedingVerifiedOnesLeftBehind;
   private boolean unverifiedExpectationsFixed;
   private int indexIncrement;

   OrderedVerificationPhase(
      @Nonnull RecordAndReplayExecution recordAndReplay,
      @Nonnull List<Expectation> expectationsInReplayOrder,
      @Nonnull List<Object> invocationInstancesInReplayOrder,
      @Nonnull List<Object[]> invocationArgumentsInReplayOrder)
   {
      super(
         recordAndReplay, new ArrayList<Expectation>(expectationsInReplayOrder),
         invocationInstancesInReplayOrder, invocationArgumentsInReplayOrder);
      discardExpectationsAndArgumentsAlreadyVerified();
      expectationCount = expectationsInReplayOrder.size();
      indexIncrement = 1;
   }

   private void discardExpectationsAndArgumentsAlreadyVerified()
   {
      for (VerifiedExpectation verified : recordAndReplay.executionState.verifiedExpectations) {
         int i = expectationsInReplayOrder.indexOf(verified.expectation);

         if (i >= 0) {
            expectationsInReplayOrder.set(i, null);
         }
      }
   }

   @Nonnull @Override
   protected List<ExpectedInvocation> findNonStrictExpectation(
      @Nullable Object mock, @Nonnull String mockClassDesc, @Nonnull String mockNameAndDesc, @Nonnull Object[] args)
   {
      int i = replayIndex;

      while (i >= 0 && i < expectationCount) {
         Expectation replayExpectation = expectationsInReplayOrder.get(i);
         Object replayInstance = invocationInstancesInReplayOrder.get(i);
         Object[] replayArgs = invocationArgumentsInReplayOrder.get(i);

         i += indexIncrement;

         if (replayExpectation == null) {
            continue;
         }

         if (!matchInstance && recordAndReplay.executionState.isToBeMatchedOnInstance(mock, mockNameAndDesc)) {
            matchInstance = true;
         }

         if (matches(mock, mockClassDesc, mockNameAndDesc, args, replayExpectation, replayInstance, replayArgs)) {
            currentExpectation = replayExpectation;
            i += 1 - indexIncrement;
            indexIncrement = 1;
            replayIndex = i;
            expectationBeingVerified().constraints.invocationCount++;
            mapNewInstanceToReplacementIfApplicable(mock);
            break;
         }

         if (!unverifiedExpectationsFixed) {
            unverifiedInvocationLeftBehind = replayExpectation.invocation;
         }
         else if (indexIncrement > 0) {
            recordAndReplay.setErrorThrown(replayExpectation.invocation.errorForUnexpectedInvocation());
            replayIndex = i;
            break;
         }
      }

      return emptyList();
   }

   public void fixPositionOfUnverifiedExpectations()
   {
      if (unverifiedInvocationLeftBehind != null) {
         throw
            currentExpectation == null ?
               unverifiedInvocationLeftBehind.errorForUnexpectedInvocation() :
               unverifiedInvocationLeftBehind.errorForUnexpectedInvocationBeforeAnother(currentExpectation.invocation);
      }

      int indexOfLastUnverified = indexOfLastUnverifiedExpectation();

      if (indexOfLastUnverified >= 0) {
         replayIndex = indexOfLastUnverified;
         indexIncrement = -1;
         unverifiedExpectationsFixed = true;
      }
   }

   private int indexOfLastUnverifiedExpectation()
   {
      for (int i = expectationCount - 1; i >= 0; i--) {
         if (expectationsInReplayOrder.get(i) != null) {
            return i;
         }
      }

      return -1;
   }

   @SuppressWarnings("OverlyComplexMethod")
   @Override
   public void handleInvocationCountConstraint(int minInvocations, int maxInvocations)
   {
      Error errorThrown = pendingError;
      pendingError = null;

      if (errorThrown != null && minInvocations > 0) {
         throw errorThrown;
      }

      Expectation verifying = expectationBeingVerified();
      ExpectedInvocation invocation = verifying.invocation;
      argMatchers = invocation.arguments.getMatchers();
      int invocationCount = 1;

      while (replayIndex < expectationCount) {
         Expectation replayExpectation = expectationsInReplayOrder.get(replayIndex);

         if (replayExpectation != null && matchesCurrentVerification(replayExpectation)) {
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

      verifyMaxInvocations(maxInvocations);
   }

   private boolean matchesCurrentVerification(@Nonnull Expectation replayExpectation)
   {
      ExpectedInvocation invocation = expectationBeingVerified().invocation;
      Object mock = invocation.instance;
      String mockClassDesc = invocation.getClassDesc();
      String mockNameAndDesc = invocation.getMethodNameAndDescription();
      Object[] args = invocation.arguments.getValues();
      matchInstance = invocation.matchInstance;

      if (recordAndReplay.executionState.isToBeMatchedOnInstance(mock, mockNameAndDesc)) {
         matchInstance = true;
      }

      Object replayInstance = invocationInstancesInReplayOrder.get(replayIndex);
      Object[] replayArgs = invocationArgumentsInReplayOrder.get(replayIndex);

      return matches(mock, mockClassDesc, mockNameAndDesc, args, replayExpectation, replayInstance, replayArgs);
   }

   private void verifyMaxInvocations(int maxInvocations)
   {
      if (maxInvocations >= 0) {
         Expectation verifying = expectationBeingVerified();
         int n = verifying.constraints.invocationCount - maxInvocations;

         if (n > 0) {
            Object[] replayArgs = invocationArgumentsInReplayOrder.get(replayIndex - 1);
            throw verifying.invocation.errorForUnexpectedInvocations(replayArgs, n);
         }
      }
   }

   @Nullable @Override
   protected Error endVerification()
   {
      if (pendingError != null) {
         return pendingError;
      }

      if (
         unverifiedExpectationsFixed && indexIncrement > 0 && currentExpectation != null &&
         replayIndex <= indexOfLastUnverifiedExpectation()
      ) {
         ExpectedInvocation unexpectedInvocation = expectationsInReplayOrder.get(replayIndex).invocation;
         return unexpectedInvocation.errorForUnexpectedInvocationAfterAnother(currentExpectation.invocation);
      }

      if (unverifiedInvocationPrecedingVerifiedOnesLeftBehind != null) {
         return unverifiedInvocationPrecedingVerifiedOnesLeftBehind.errorForUnexpectedInvocation();
      }

      return super.endVerification();
   }

   @Override
   boolean shouldDiscardInformationAboutVerifiedInvocationOnceUsed() { return true; }

   public void checkOrderOfVerifiedInvocations(@Nonnull BaseVerificationPhase verificationPhase)
   {
      if (verificationPhase instanceof OrderedVerificationPhase) {
         throw new IllegalArgumentException("Invalid use of ordered verification block");
      }

      UnorderedVerificationPhase previousVerification = (UnorderedVerificationPhase) verificationPhase;

      if (previousVerification.verifiedExpectations.isEmpty()) {
         return;
      }

      if (indexIncrement > 0) {
         checkForwardOrderOfVerifiedInvocations(previousVerification);
      }
      else {
         checkBackwardOrderOfVerifiedInvocations(previousVerification);
      }
   }

   private void checkForwardOrderOfVerifiedInvocations(@Nonnull UnorderedVerificationPhase previousVerification)
   {
      int maxReplayIndex = findUnexpectedInvocationAmongVerifiedOnes(previousVerification);

      for (int i = replayIndex; i < maxReplayIndex; i++) {
         Expectation expectation = expectationsInReplayOrder.get(i);

         if (expectation != null) {
            unverifiedInvocationPrecedingVerifiedOnesLeftBehind = expectation.invocation;
            break;
         }
      }

      replayIndex = maxReplayIndex + 1;
      currentExpectation = replayIndex < expectationCount ? expectationsInReplayOrder.get(replayIndex) : null;
   }

   private int findUnexpectedInvocationAmongVerifiedOnes(@Nonnull UnorderedVerificationPhase previousVerification)
   {
      int maxReplayIndex = replayIndex - 1;

      for (VerifiedExpectation verified : previousVerification.verifiedExpectations) {
         if (verified.replayIndex < replayIndex) {
            ExpectedInvocation unexpectedInvocation = verified.expectation.invocation;
            throw currentExpectation == null ?
               unexpectedInvocation.errorForUnexpectedInvocationFoundBeforeAnother() :
               unexpectedInvocation.errorForUnexpectedInvocationFoundBeforeAnother(currentExpectation.invocation);
         }

         if (verified.replayIndex > maxReplayIndex) {
            maxReplayIndex = verified.replayIndex;
         }
      }

      return maxReplayIndex;
   }

   private void checkBackwardOrderOfVerifiedInvocations(@Nonnull UnorderedVerificationPhase previousVerification)
   {
      int indexOfLastUnverified = indexOfLastUnverifiedExpectation();

      if (indexOfLastUnverified >= 0) {
         VerifiedExpectation firstVerified = previousVerification.firstExpectationVerified();
         assert firstVerified != null;

         if (firstVerified.replayIndex != indexOfLastUnverified + 1) {
            Expectation lastUnverified = expectationsInReplayOrder.get(indexOfLastUnverified);
            Expectation after = firstVerified.expectation;
            throw lastUnverified.invocation.errorForUnexpectedInvocationAfterAnother(after.invocation);
         }
      }
   }
}
