/*
 * Copyright (c) 2006 JMockit developers
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.expectations;

import java.util.*;
import javax.annotation.*;

import mockit.internal.expectations.invocation.*;
import mockit.internal.state.*;
import mockit.internal.util.*;

final class PhasedExecutionState
{
   @Nonnull final List<Expectation> expectations;
   @Nonnull final List<VerifiedExpectation> verifiedExpectations;
   @Nonnull final EquivalentInstances equivalentInstances;
   @Nonnull final InstanceBasedMatching instanceBasedMatching;
   @Nullable PartiallyMockedInstances partiallyMockedInstances;

   PhasedExecutionState() {
      expectations = new ArrayList<>();
      verifiedExpectations = new ArrayList<>();
      equivalentInstances = new EquivalentInstances();
      instanceBasedMatching = new InstanceBasedMatching();
   }

   void addExpectation(@Nonnull Expectation expectation) {
      ExpectedInvocation invocation = expectation.invocation;
      forceMatchingOnMockInstanceIfRequired(invocation);
      removeMatchingExpectationsCreatedBefore(invocation);
      expectations.add(expectation);
   }

   private void forceMatchingOnMockInstanceIfRequired(@Nonnull ExpectedInvocation invocation) {
      if (!invocation.matchInstance && isToBeMatchedOnInstance(invocation.instance, invocation.getMethodNameAndDescription())) {
         invocation.matchInstance = true;
      }
   }

   boolean isToBeMatchedOnInstance(@Nullable Object mock, @Nonnull String mockNameAndDesc) {
      if (mock == null || mockNameAndDesc.charAt(0) == '<') {
         return false;
      }

      if (
         instanceBasedMatching.isToBeMatchedOnInstance(mock) ||
         partiallyMockedInstances != null && partiallyMockedInstances.isToBeMatchedOnInstance(mock)
      ) {
         return true;
      }

      return TestRun.getExecutingTest().isInjectableMock(mock);
   }

   private void removeMatchingExpectationsCreatedBefore(@Nonnull ExpectedInvocation invocation) {
      Expectation previousExpectation = findPreviousExpectation(invocation);

      if (previousExpectation != null) {
         expectations.remove(previousExpectation);
         invocation.copyDefaultReturnValue(previousExpectation.invocation);
      }
   }

   @Nullable
   private Expectation findPreviousExpectation(@Nonnull ExpectedInvocation newInvocation) {
      int n = expectations.size();

      if (n == 0) {
         return null;
      }

      Object mock = newInvocation.instance;
      @Nonnull Boolean matchInstance = newInvocation.matchInstance;
      String mockClassDesc = newInvocation.getClassDesc();
      String mockNameAndDesc = newInvocation.getMethodNameAndDescription();
      boolean isConstructor = newInvocation.isConstructor();

      for (int i = 0; i < n; i++) {
         Expectation previous = expectations.get(i);

         if (
            isMatchingInvocation(mock, matchInstance, mockClassDesc, mockNameAndDesc, isConstructor, previous) &&
            isWithMatchingArguments(newInvocation, previous.invocation)
         ) {
            return previous;
         }
      }

      return null;
   }

   private boolean isMatchingInvocation(
      @Nullable Object mock, @Nullable Boolean matchInstance, @Nonnull String mockClassDesc, @Nonnull String mockNameAndDesc,
      boolean constructorInvocation, @Nonnull Expectation expectation
   ) {
      ExpectedInvocation invocation = expectation.invocation;

      return
         invocation.isMatch(mock, mockClassDesc, mockNameAndDesc) &&
         isSameMockedClass(mock, invocation.instance) &&
         (constructorInvocation || mock == null || isMatchingInstance(mock, matchInstance, expectation));
   }

   private static boolean isSameMockedClass(@Nullable Object mock1, @Nullable Object mock2) {
      if (mock1 == mock2) {
         return true;
      }

      if (mock1 != null && mock2 != null) {
         Class<?> mockedClass1 = mock1.getClass();
         Class<?> mockedClass2 = GeneratedClasses.getMockedClass(mock2);
         return mockedClass2.isAssignableFrom(mockedClass1) || TestRun.mockFixture().areCapturedClasses(mockedClass1, mockedClass2);
      }

      return false;
   }

   private boolean isWithMatchingArguments(@Nonnull ExpectedInvocation newInvocation, @Nonnull ExpectedInvocation previousInvocation) {
      InvocationArguments newArguments = newInvocation.arguments;
      InvocationArguments previousArguments = previousInvocation.arguments;

      if (newArguments.getMatchers() == null) {
         return previousArguments.isMatch(newArguments.getValues(), equivalentInstances.instanceMap);
      }

      return newArguments.hasEquivalentMatchers(previousArguments);
   }

   @Nullable
   Expectation findExpectation(
      @Nullable Object mock, @Nonnull String mockClassDesc, @Nonnull String mockNameAndDesc, @Nonnull Object[] args
   ) {
      boolean isConstructor = mockNameAndDesc.charAt(0) == '<';
      Expectation replayExpectationFound = null;

      // Note: new expectations might get added to the list, so a regular loop would cause a CME:
      for (int i = 0, n = expectations.size(); i < n; i++) {
         Expectation expectation = expectations.get(i);

         if (replayExpectationFound != null && expectation.recordPhase == null) {
            continue;
         }

         if (
            isMatchingInvocation(mock, null, mockClassDesc, mockNameAndDesc, isConstructor, expectation) &&
            expectation.invocation.arguments.isMatch(args, equivalentInstances.instanceMap)
         ) {
            if (expectation.recordPhase == null) {
               replayExpectationFound = expectation;
               continue;
            }

            if (isConstructor) {
               equivalentInstances.registerReplacementInstanceIfApplicable(mock, expectation.invocation);
            }

            return expectation;
         }
      }

      return replayExpectationFound;
   }

   private boolean isMatchingInstance(@Nonnull Object invokedInstance, @Nullable Boolean matchInstance, @Nonnull Expectation expectation) {
      ExpectedInvocation invocation = expectation.invocation;
      Object invocationInstance = invocation.instance;
      assert invocationInstance != null;

      if (equivalentInstances.isEquivalentInstance(invocationInstance, invokedInstance)) {
         return true;
      }

      if (
         TestRun.getExecutingTest().isInjectableMock(invokedInstance) ||
         partiallyMockedInstances != null && partiallyMockedInstances.isDynamicMockInstanceOrClass(invokedInstance, invocationInstance) ||
         equivalentInstances.areNonEquivalentInstances(invocationInstance, invokedInstance)
      ) {
         return false;
      }

      return
         (matchInstance == null || !matchInstance) && !invocation.matchInstance && expectation.recordPhase != null &&
         !equivalentInstances.replacementMap.containsValue(invocationInstance);
   }
}
