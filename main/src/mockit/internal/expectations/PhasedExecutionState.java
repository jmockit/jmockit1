/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.expectations;

import java.util.*;
import java.util.Map.*;
import javax.annotation.*;

import static mockit.internal.util.Utilities.containsReference;

import mockit.internal.expectations.invocation.*;
import mockit.internal.state.*;
import mockit.internal.util.*;

final class PhasedExecutionState
{
   @Nonnull final List<Expectation> expectations;
   @Nonnull final List<VerifiedExpectation> verifiedExpectations;
   @Nonnull final Map<Object, Object> instanceMap;
   @Nonnull final Map<Object, Object> replacementMap;
   @Nullable private List<?> dynamicMockInstancesToMatch;
   @Nullable private List<Class<?>> mockedTypesToMatchOnInstances;

   PhasedExecutionState()
   {
      expectations = new ArrayList<Expectation>();
      verifiedExpectations = new ArrayList<VerifiedExpectation>();
      instanceMap = new IdentityHashMap<Object, Object>();
      replacementMap = new IdentityHashMap<Object, Object>();
   }

   void setDynamicMockInstancesToMatch(@Nonnull List<?> dynamicMockInstancesToMatch)
   {
      this.dynamicMockInstancesToMatch = dynamicMockInstancesToMatch;
   }

   void discoverMockedTypesToMatchOnInstances(@Nonnull List<Class<?>> targetClasses)
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

   private void addMockedTypeToMatchOnInstance(@Nonnull Class<?> mockedType)
   {
      if (mockedTypesToMatchOnInstances == null) {
         mockedTypesToMatchOnInstances = new LinkedList<Class<?>>();
      }

      if (!containsReference(mockedTypesToMatchOnInstances, mockedType)) {
         mockedTypesToMatchOnInstances.add(mockedType);
      }
   }

   void addExpectation(@Nonnull Expectation expectation)
   {
      ExpectedInvocation invocation = expectation.invocation;
      forceMatchingOnMockInstanceIfRequired(invocation);
      removeMatchingExpectationsCreatedBefore(invocation);
      expectations.add(expectation);
   }

   private void forceMatchingOnMockInstanceIfRequired(@Nonnull ExpectedInvocation invocation)
   {
      if (
         !invocation.matchInstance &&
         isToBeMatchedOnInstance(invocation.instance, invocation.getMethodNameAndDescription())
      ) {
         invocation.matchInstance = true;
      }
   }

   boolean isToBeMatchedOnInstance(@Nullable Object mock, @Nonnull String mockNameAndDesc)
   {
      if (mock == null || mockNameAndDesc.charAt(0) == '<') {
         return false;
      }

      if (dynamicMockInstancesToMatch != null && containsReference(dynamicMockInstancesToMatch, mock)) {
         return true;
      }

      if (mockedTypesToMatchOnInstances != null) {
         Class<?> mockedClass = GeneratedClasses.getMockedClass(mock);

         if (containsReference(mockedTypesToMatchOnInstances, mockedClass)) {
            return true;
         }
      }

      return TestRun.getExecutingTest().isInjectableMock(mock);
   }

   private void removeMatchingExpectationsCreatedBefore(@Nonnull ExpectedInvocation invocation)
   {
      Expectation previousExpectation = findPreviousExpectation(invocation);

      if (previousExpectation != null) {
         expectations.remove(previousExpectation);
         invocation.copyDefaultReturnValue(previousExpectation.invocation);
      }
   }

   @Nullable
   private Expectation findPreviousExpectation(@Nonnull ExpectedInvocation newInvocation)
   {
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
      @Nullable Object mock, @Nullable Boolean matchInstance, @Nonnull String mockClassDesc,
      @Nonnull String mockNameAndDesc, boolean constructorInvocation, @Nonnull Expectation expectation)
   {
      ExpectedInvocation invocation = expectation.invocation;

      return
         invocation.isMatch(mock, mockClassDesc, mockNameAndDesc) &&
         isSameMockedClass(mock, invocation.instance) &&
         (constructorInvocation || mock == null || isMatchingInstance(mock, matchInstance, expectation));
   }

   private boolean isSameMockedClass(@Nullable Object mock1, @Nullable Object mock2)
   {
      if (mock1 == mock2) {
         return true;
      }

      if (mock1 != null && mock2 != null) {
         Class<?> mockedClass1 = mock1.getClass();
         Class<?> mockedClass2 = GeneratedClasses.getMockedClass(mock2);

         return
            mockedClass2.isAssignableFrom(mockedClass1) ||
            TestRun.getExecutingTest().isInvokedInstanceEquivalentToCapturedInstance(mock1, mock2) ||
            TestRun.mockFixture().areCapturedClasses(mockedClass1, mockedClass2);
      }

      return false;
   }

   private boolean isWithMatchingArguments(
      @Nonnull ExpectedInvocation newInvocation, @Nonnull ExpectedInvocation previousInvocation)
   {
      InvocationArguments newArguments = newInvocation.arguments;
      InvocationArguments previousArguments = previousInvocation.arguments;

      if (newArguments.getMatchers() == null) {
         return previousArguments.isMatch(newArguments.getValues(), instanceMap);
      }

      return newArguments.hasEquivalentMatchers(previousArguments);
   }

   @Nullable
   Expectation findExpectation(
      @Nullable Object mock, @Nonnull String mockClassDesc, @Nonnull String mockNameAndDesc, @Nonnull Object[] args)
   {
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
            expectation.invocation.arguments.isMatch(args, instanceMap)
         ) {
            if (expectation.recordPhase == null) {
               replayExpectationFound = expectation;
               continue;
            }

            if (isConstructor) {
               registerReplacementInstanceIfApplicable(mock, expectation.invocation);
            }

            return expectation;
         }
      }

      return replayExpectationFound;
   }

   private void registerReplacementInstanceIfApplicable(@Nullable Object mock, @Nonnull ExpectedInvocation invocation)
   {
      Object replacementInstance = invocation.replacementInstance;

      if (replacementInstance != null && replacementInstance != invocation.instance) {
         replacementMap.put(mock, replacementInstance);
      }
   }

   private boolean isMatchingInstance(
      @Nonnull Object invokedInstance, @Nullable Boolean matchInstance, @Nonnull Expectation expectation)
   {
      ExpectedInvocation invocation = expectation.invocation;
      Object invocationInstance = invocation.instance;
      assert invocationInstance != null;

      if (isEquivalentInstance(invocationInstance, invokedInstance)) {
         return true;
      }

      //noinspection SimplifiableIfStatement
      if (
         TestRun.getExecutingTest().isInjectableMock(invokedInstance) ||
         isDynamicMockInstanceOrClass(invokedInstance, invocationInstance) ||
         areNonEquivalentInstances(invocationInstance, invokedInstance)
      ) {
         return false;
      }

      return
         (matchInstance == null || !matchInstance) && !invocation.matchInstance && expectation.recordPhase != null &&
         !replacementMap.containsValue(invocationInstance);
   }

   boolean isEquivalentInstance(@Nonnull Object invocationInstance, @Nonnull Object invokedInstance)
   {
      return
         invocationInstance == invokedInstance ||
         invocationInstance == replacementMap.get(invokedInstance) ||
         invocationInstance == instanceMap.get(invokedInstance) ||
         invokedInstance == instanceMap.get(invocationInstance) ||
         TestRun.getExecutingTest().isInvokedInstanceEquivalentToCapturedInstance(invocationInstance, invokedInstance);
   }

   private boolean isDynamicMockInstanceOrClass(@Nonnull Object invokedInstance, @Nonnull Object invocationInstance)
   {
      if (dynamicMockInstancesToMatch != null) {
         if (containsReference(dynamicMockInstancesToMatch, invokedInstance)) {
            return true;
         }

         Class<?> invokedClass = invocationInstance.getClass();

         for (Object dynamicMock : dynamicMockInstancesToMatch) {
            if (dynamicMock.getClass() == invokedClass) {
               return true;
            }
         }
      }

      return false;
   }

   private boolean areNonEquivalentInstances(@Nonnull Object invocationInstance, @Nonnull Object invokedInstance)
   {
      boolean recordedInstanceMatchingAnyInstance = !isMatchingInstance(invocationInstance);
      boolean invokedInstanceMatchingSpecificInstance = isMatchingInstance(invokedInstance);
      return recordedInstanceMatchingAnyInstance && invokedInstanceMatchingSpecificInstance;
   }

   private boolean isMatchingInstance(@Nonnull Object instance)
   {
      return
         instanceMap.containsKey(instance)    || instanceMap.containsValue(instance) ||
         replacementMap.containsKey(instance) || replacementMap.containsValue(instance);
   }

   boolean areInDifferentEquivalenceSets(@Nonnull Object mock1, @Nonnull Object mock2)
   {
      if (mock1 == mock2 || instanceMap.isEmpty()) {
         return false;
      }

      Object mock1Equivalent = instanceMap.get(mock1);
      Object mock2Equivalent = instanceMap.get(mock2);

      if (mock1Equivalent == mock2 || mock2Equivalent == mock1) {
         return false;
      }

      //noinspection SimplifiableIfStatement
      if (mock1Equivalent != null && mock2Equivalent != null) {
         return true;
      }

      return instanceMapHasMocksInSeparateEntries(mock1, mock2);
   }

   private boolean instanceMapHasMocksInSeparateEntries(@Nonnull Object mock1, @Nonnull Object mock2)
   {
      boolean found1 = false;
      boolean found2 = false;

      for (Entry<Object, Object> entry : instanceMap.entrySet()) {
         if (!found1 && isInMapEntry(entry, mock1)) {
            found1 = true;
         }

         if (!found2 && isInMapEntry(entry, mock2)) {
            found2 = true;
         }

         if (found1 && found2) {
            return true;
         }
      }

      return false;
   }

   private static boolean isInMapEntry(@Nonnull Entry<Object, Object> mapEntry, @Nonnull Object mock)
   {
      return mapEntry.getKey() == mock || mapEntry.getValue() == mock;
   }

   @Nullable
   Object getReplacementInstanceForMethodInvocation(@Nonnull Object invokedInstance, @Nonnull String methodNameAndDesc)
   {
      return methodNameAndDesc.charAt(0) == '<' ? null : replacementMap.get(invokedInstance);
   }

   boolean isReplacementInstance(@Nonnull Object invokedInstance, @Nonnull String methodNameAndDesc)
   {
      return
         methodNameAndDesc.charAt(0) != '<' && (
            replacementMap.containsKey(invokedInstance) ||
            replacementMap.containsValue(invokedInstance)
         );
   }
}
