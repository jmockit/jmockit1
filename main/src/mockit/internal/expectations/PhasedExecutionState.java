/*
 * Copyright (c) 2006-2014 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.expectations;

import java.util.*;

import org.jetbrains.annotations.*;

import static mockit.internal.util.Utilities.containsReference;

import mockit.internal.expectations.invocation.*;
import mockit.internal.state.*;
import mockit.internal.util.*;

final class PhasedExecutionState
{
   @NotNull final List<Expectation> expectations;
   @NotNull final List<Expectation> nonStrictExpectations;
   @NotNull final List<VerifiedExpectation> verifiedExpectations;
   @NotNull final Map<Object, Object> instanceMap;
   @NotNull final Map<Object, Object> replacementMap;
   @Nullable private List<?> dynamicMockInstancesToMatch;
   @Nullable private List<Class<?>> mockedTypesToMatchOnInstances;

   PhasedExecutionState()
   {
      expectations = new ArrayList<Expectation>();
      nonStrictExpectations = new ArrayList<Expectation>();
      verifiedExpectations = new ArrayList<VerifiedExpectation>();
      instanceMap = new IdentityHashMap<Object, Object>();
      replacementMap = new IdentityHashMap<Object, Object>();
   }

   @SuppressWarnings("NullableProblems")
   void setDynamicMockInstancesToMatch(@NotNull List<?> dynamicMockInstancesToMatch)
   {
      this.dynamicMockInstancesToMatch = dynamicMockInstancesToMatch;
   }

   void discoverMockedTypesToMatchOnInstances(@NotNull List<Class<?>> targetClasses)
   {
      int numClasses = targetClasses.size();

      if (numClasses > 1) {
         for (int i = 0; i < numClasses; i++) {
            Class<?> targetClass = targetClasses.get(i);

            if (targetClasses.lastIndexOf(targetClass) > i) {
               addMockedTypeToMatchOnInstance(targetClass);
            }
         }
      }
   }

   void addMockedTypeToMatchOnInstance(@NotNull Class<?> mockedType)
   {
      if (mockedTypesToMatchOnInstances == null) {
         mockedTypesToMatchOnInstances = new LinkedList<Class<?>>();
      }

      if (!containsReference(mockedTypesToMatchOnInstances, mockedType)) {
         mockedTypesToMatchOnInstances.add(mockedType);
      }
   }

   void addExpectation(@NotNull Expectation expectation, boolean nonStrict)
   {
      ExpectedInvocation invocation = expectation.invocation;
      forceMatchingOnMockInstanceIfRequired(invocation);
      removeMatchingExpectationsCreatedBefore(invocation);

      if (nonStrict) {
         nonStrictExpectations.add(expectation);
      }
      else {
         expectations.add(expectation);
      }
   }

   private void forceMatchingOnMockInstanceIfRequired(@NotNull ExpectedInvocation invocation)
   {
      if (isToBeMatchedOnInstance(invocation.instance, invocation.getMethodNameAndDescription())) {
         invocation.matchInstance = true;
      }
   }

   boolean isToBeMatchedOnInstance(@Nullable Object mock, @NotNull String mockNameAndDesc)
   {
      if (mock == null || mockNameAndDesc.charAt(0) == '<') {
         return false;
      }
      else if (dynamicMockInstancesToMatch != null && containsReference(dynamicMockInstancesToMatch, mock)) {
         return true;
      }
      else if (mockedTypesToMatchOnInstances != null) {
         Class<?> mockedClass = GeneratedClasses.getMockedClass(mock);

         if (containsReference(mockedTypesToMatchOnInstances, mockedClass)) {
            return true;
         }
      }
      else if (TestRun.getExecutingTest().isInjectableMock(mock)) {
         return true;
      }

      return false;
   }

   private void removeMatchingExpectationsCreatedBefore(@NotNull ExpectedInvocation invocation)
   {
      Expectation previousExpectation = findPreviousNonStrictExpectation(invocation);

      if (previousExpectation != null) {
         nonStrictExpectations.remove(previousExpectation);
         invocation.copyDefaultReturnValue(previousExpectation.invocation);
      }
   }

   @Nullable private Expectation findPreviousNonStrictExpectation(@NotNull ExpectedInvocation newInvocation)
   {
      Object mock = newInvocation.instance;
      String mockClassDesc = newInvocation.getClassDesc();
      String mockNameAndDesc = newInvocation.getMethodNameAndDescription();
      InvocationArguments arguments = newInvocation.arguments;
      Object[] argValues = arguments.getValues();

      boolean constructorInvocation = newInvocation.isConstructor();
      boolean newInvocationWithMatchers = arguments.getMatchers() != null;

      for (int i = 0, n = nonStrictExpectations.size(); i < n; i++) {
         Expectation previousExpectation = nonStrictExpectations.get(i);
         ExpectedInvocation previousInvocation = previousExpectation.invocation;

         if (
            previousInvocation.isMatch(mockClassDesc, mockNameAndDesc) &&
            (constructorInvocation || mock == null || isMatchingInstance(mock, previousExpectation)) &&
            (newInvocationWithMatchers && arguments.hasEquivalentMatchers(previousInvocation.arguments) ||
             !newInvocationWithMatchers && previousInvocation.arguments.isMatch(argValues, instanceMap))
         ) {
            return previousExpectation;
         }
      }

      return null;
   }

   @Nullable
   Expectation findNonStrictExpectation(
      @Nullable Object mock, @NotNull String mockClassDesc, @NotNull String mockNameAndDesc, @NotNull Object[] args)
   {
      boolean constructorInvocation = mockNameAndDesc.charAt(0) == '<';
      Expectation replayExpectationFound = null;

      // Note: new expectations might get added to the list, so a regular loop would cause a CME:
      for (int i = 0, n = nonStrictExpectations.size(); i < n; i++) {
         Expectation nonStrict = nonStrictExpectations.get(i);

         if (replayExpectationFound != null && nonStrict.recordPhase == null) {
            continue;
         }

         ExpectedInvocation invocation = nonStrict.invocation;

         if (
            invocation.isMatch(mockClassDesc, mockNameAndDesc) &&
            (constructorInvocation || mock == null || isMatchingInstance(mock, nonStrict)) &&
            invocation.arguments.isMatch(args, instanceMap)
         ) {
            if (nonStrict.recordPhase == null) {
               replayExpectationFound = nonStrict;
               continue;
            }

            if (constructorInvocation && invocation.replacementInstance != null) {
               replacementMap.put(mock, invocation.replacementInstance);
            }

            return nonStrict;
         }
      }

      return replayExpectationFound;
   }

   private boolean isMatchingInstance(@NotNull Object mock, @NotNull Expectation expectation)
   {
      ExpectedInvocation invocation = expectation.invocation;
      assert invocation.instance != null;

      if (invocation.instance == replacementMap.get(mock) || invocation.isEquivalentInstance(mock)) {
         return true;
      }

      if (TestRun.getExecutingTest().isInjectableMock(mock)) {
         return false;
      }

      if (dynamicMockInstancesToMatch != null) {
         if (containsReference(dynamicMockInstancesToMatch, mock)) {
            return false;
         }

         Class<?> invokedClass = invocation.instance.getClass();

         for (Object dynamicMock : dynamicMockInstancesToMatch) {
            if (dynamicMock.getClass() == invokedClass) {
               return false;
            }
         }
      }

      return !invocation.matchInstance && expectation.recordPhase != null;
   }

   @Nullable
   Object getReplacementInstanceForMethodInvocation(@Nullable Object invokedInstance, @NotNull String methodNameAndDesc)
   {
      return methodNameAndDesc.charAt(0) == '<' ? null : replacementMap.get(invokedInstance);
   }
}
