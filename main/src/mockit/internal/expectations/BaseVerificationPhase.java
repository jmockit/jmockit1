/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.expectations;

import java.util.*;
import javax.annotation.*;

import mockit.internal.expectations.argumentMatching.*;
import mockit.internal.expectations.invocation.*;
import mockit.internal.util.*;

public abstract class BaseVerificationPhase extends TestOnlyPhase
{
   @Nonnull final List<Expectation> expectationsInReplayOrder;
   @Nonnull final List<Object> invocationInstancesInReplayOrder;
   @Nonnull final List<Object[]> invocationArgumentsInReplayOrder;
   private boolean allMockedInvocationsDuringReplayMustBeVerified;
   @Nullable private Object[] mockedTypesAndInstancesToFullyVerify;
   @Nonnull private final List<VerifiedExpectation> currentVerifiedExpectations;
   @Nullable private Expectation currentVerification;
   protected int replayIndex;
   @Nullable protected Error pendingError;
   @Nullable protected ExpectedInvocation matchingInvocationWithDifferentArgs;

   protected BaseVerificationPhase(
      @Nonnull RecordAndReplayExecution recordAndReplay,
      @Nonnull List<Expectation> expectationsInReplayOrder,
      @Nonnull List<Object> invocationInstancesInReplayOrder,
      @Nonnull List<Object[]> invocationArgumentsInReplayOrder)
   {
      super(recordAndReplay);
      this.expectationsInReplayOrder = expectationsInReplayOrder;
      this.invocationInstancesInReplayOrder = invocationInstancesInReplayOrder;
      this.invocationArgumentsInReplayOrder = invocationArgumentsInReplayOrder;
      currentVerifiedExpectations = new ArrayList<VerifiedExpectation>();
   }

   public final void setAllInvocationsMustBeVerified() { allMockedInvocationsDuringReplayMustBeVerified = true; }

   public final void setMockedTypesToFullyVerify(@Nonnull Object[] mockedTypesAndInstancesToFullyVerify)
   {
      this.mockedTypesAndInstancesToFullyVerify = mockedTypesAndInstancesToFullyVerify;
   }

   @Nonnull
   protected final Expectation expectationBeingVerified()
   {
      //noinspection ConstantConditions
      return currentVerification;
   }

   @Nullable @Override
   final Object handleInvocation(
      @Nullable Object mock, int mockAccess, @Nonnull String mockClassDesc, @Nonnull String mockNameAndDesc,
      @Nullable String genericSignature, boolean withRealImpl, @Nonnull Object[] args)
   {
      if (pendingError != null) {
         recordAndReplay.setErrorThrown(pendingError);
         pendingError = null;
         return null;
      }

      matchInstance =
         mock != null && (
            recordAndReplay.executionState.isReplacementInstance(mock, mockNameAndDesc) ||
            isEnumElement(mock)
         );

      ExpectedInvocation currentInvocation = new ExpectedInvocation(
         mock, mockAccess, mockClassDesc, mockNameAndDesc, matchInstance, genericSignature, args);
      currentInvocation.arguments.setMatchers(argMatchers);
      currentVerification = new Expectation(currentInvocation);

      currentExpectation = null;
      currentVerifiedExpectations.clear();
      List<ExpectedInvocation> matchingInvocationsWithDifferentArgs =
         findNonStrictExpectation(mock, mockClassDesc, mockNameAndDesc, args);
      argMatchers = null;

      if (recordAndReplay.getErrorThrown() != null) {
         return null;
      }

      if (currentExpectation == null) {
         pendingError = currentVerification.invocation.errorForMissingInvocation(matchingInvocationsWithDifferentArgs);
         currentExpectation = currentVerification;
      }

      return currentExpectation.invocation.getDefaultValueForReturnType();
   }

   @Nonnull
   abstract List<ExpectedInvocation> findNonStrictExpectation(
      @Nullable Object mock, @Nonnull String mockClassDesc, @Nonnull String mockNameAndDesc, @Nonnull Object[] args);

   final boolean matches(
      @Nullable Object mock, @Nonnull String mockClassDesc, @Nonnull String mockNameAndDesc, @Nonnull Object[] args,
      @Nonnull Expectation replayExpectation, @Nullable Object replayInstance, @Nonnull Object[] replayArgs)
   {
      ExpectedInvocation invocation = replayExpectation.invocation;
      boolean constructor = invocation.isConstructor();
      Map<Object, Object> replacementMap = getReplacementMap();
      matchingInvocationWithDifferentArgs = null;

      if (invocation.isMatch(mock, mockClassDesc, mockNameAndDesc, replacementMap)) {
         boolean matching;

         if (mock == null || invocation.instance == null) {
            matching = true;
         }
         else if (matchInstance) {
            matching = recordAndReplay.executionState.isEquivalentInstance(invocation.instance, mock);
         }
         else {
            matching =
               constructor || !recordAndReplay.executionState.areInDifferentEquivalenceSets(invocation.instance, mock);
         }

         if (matching) {
            matchingInvocationWithDifferentArgs = invocation;

            InvocationArguments invocationArguments = invocation.arguments;
            List<ArgumentMatcher<?>> originalMatchers = invocationArguments.getMatchers();
            Object[] originalArgs = invocationArguments.prepareForVerification(args, argMatchers);
            Map<Object, Object> instanceMap = getInstanceMap();
            boolean argumentsMatch = invocationArguments.isMatch(replayArgs, instanceMap);
            invocationArguments.setValuesAndMatchers(originalArgs, originalMatchers);

            if (argumentsMatch) {
               if (constructor) {
                  instanceMap.put(replayInstance, mock);
               }

               addVerifiedExpectation(replayExpectation, replayArgs, argMatchers);
               return true;
            }
         }
      }

      return false;
   }

   private void addVerifiedExpectation(
      @Nonnull Expectation expectation, @Nonnull Object[] args, @Nullable List<ArgumentMatcher<?>> matchers)
   {
      int i = expectationsInReplayOrder.indexOf(expectation);
      addVerifiedExpectation(new VerifiedExpectation(expectation, args, matchers, i));
   }

   void addVerifiedExpectation(@Nonnull VerifiedExpectation verifiedExpectation)
   {
      recordAndReplay.executionState.verifiedExpectations.add(verifiedExpectation);
      currentVerifiedExpectations.add(verifiedExpectation);
   }

   final void mapNewInstanceToReplacementIfApplicable(@Nullable Object mock)
   {
      if (mock != null && !matchInstance) {
         assert currentExpectation != null;
         ExpectedInvocation invocation = currentExpectation.invocation;

         if (invocation.isConstructor()) {
            Object replacementInstance = invocation.replacementInstance;

            if (replacementInstance != null) {
               getReplacementMap().put(mock, replacementInstance);
            }
         }
      }
   }

   @Override
   public final void setMaxInvocationCount(int maxInvocations)
   {
      if (maxInvocations == 0 || pendingError == null) {
         super.setMaxInvocationCount(maxInvocations);
      }
   }

   @Nullable
   protected Error endVerification()
   {
      if (pendingError != null) {
         return pendingError;
      }

      if (allMockedInvocationsDuringReplayMustBeVerified) {
         return validateThatAllInvocationsWereVerified();
      }

      return null;
   }

   @Nullable
   private Error validateThatAllInvocationsWereVerified()
   {
      List<Expectation> notVerified = new ArrayList<Expectation>();

      for (int i = 0; i < expectationsInReplayOrder.size(); i++) {
         Expectation replayExpectation = expectationsInReplayOrder.get(i);

         if (replayExpectation != null && isEligibleForFullVerification(replayExpectation)) {
            Object[] replayArgs = invocationArgumentsInReplayOrder.get(i);

            if (!wasVerified(replayExpectation, replayArgs)) {
               notVerified.add(replayExpectation);
            }
         }
      }

      if (!notVerified.isEmpty()) {
         if (mockedTypesAndInstancesToFullyVerify == null) {
            Expectation firstUnexpected = notVerified.get(0);
            return firstUnexpected.invocation.errorForUnexpectedInvocation();
         }

         return validateThatUnverifiedInvocationsAreAllowed(notVerified);
      }

      return null;
   }

   private static boolean isEligibleForFullVerification(@Nonnull Expectation replayExpectation)
   {
      return !replayExpectation.executedRealImplementation && replayExpectation.constraints.minInvocations <= 0;
   }

   private boolean wasVerified(@Nonnull Expectation replayExpectation, @Nonnull Object[] replayArgs)
   {
      InvocationArguments invokedArgs = replayExpectation.invocation.arguments;
      List<VerifiedExpectation> expectationsVerified = recordAndReplay.executionState.verifiedExpectations;

      for (int j = 0; j < expectationsVerified.size(); j++) {
         VerifiedExpectation verified = expectationsVerified.get(j);

         if (verified.expectation == replayExpectation) {
            Object[] storedArgs = invokedArgs.prepareForVerification(verified.arguments, verified.argMatchers);
            boolean argumentsMatch = invokedArgs.isMatch(replayArgs, getInstanceMap());
            invokedArgs.setValuesWithNoMatchers(storedArgs);

            if (argumentsMatch) {
               if (shouldDiscardInformationAboutVerifiedInvocationOnceUsed()) {
                  expectationsVerified.remove(j);
               }

               return true;
            }
         }
      }

      invokedArgs.setValuesWithNoMatchers(replayArgs);
      return false;
   }

   boolean shouldDiscardInformationAboutVerifiedInvocationOnceUsed() { return false; }

   @Nullable
   private Error validateThatUnverifiedInvocationsAreAllowed(@Nonnull List<Expectation> unverified)
   {
      for (Expectation expectation : unverified) {
         ExpectedInvocation invocation = expectation.invocation;

         if (isInvocationToBeVerified(invocation)) {
            return invocation.errorForUnexpectedInvocation();
         }
      }

      return null;
   }

   private boolean isInvocationToBeVerified(@Nonnull ExpectedInvocation unverifiedInvocation)
   {
      String invokedClassName = unverifiedInvocation.getClassName();
      Object invokedInstance = unverifiedInvocation.instance;
      assert mockedTypesAndInstancesToFullyVerify != null;

      for (Object mockedTypeOrInstance : mockedTypesAndInstancesToFullyVerify) {
         if (mockedTypeOrInstance instanceof Class) {
            Class<?> mockedType = (Class<?>) mockedTypeOrInstance;

            if (invokedClassName.equals(mockedType.getName())) {
               return true;
            }
         }
         else if (invokedInstance == null) {
            ClassLoader loader = mockedTypeOrInstance.getClass().getClassLoader();
            Class<?> invokedClass = ClassLoad.loadFromLoader(loader, invokedClassName);

            if (invokedClass.isInstance(mockedTypeOrInstance)) {
               return true;
            }
         }
         else if (unverifiedInvocation.matchInstance) {
            if (mockedTypeOrInstance == invokedInstance) {
               return true;
            }
         }
         else if (invokedInstance.getClass().isInstance(mockedTypeOrInstance)) {
            return true;
         }
      }

      return false;
   }

   @Nullable
   public final Object getArgumentValueForCurrentVerification(int parameterIndex)
   {
      List<VerifiedExpectation> verifiedExpectations = recordAndReplay.executionState.verifiedExpectations;

      if (verifiedExpectations.isEmpty()) {
         return expectationBeingVerified().invocation.getArgumentValues()[parameterIndex];
      }

      VerifiedExpectation lastMatched = verifiedExpectations.get(verifiedExpectations.size() - 1);
      return lastMatched.arguments[parameterIndex];
   }

   public final void discardReplayedInvocations()
   {
      if (mockedTypesAndInstancesToFullyVerify == null) {
         expectationsInReplayOrder.clear();
         invocationArgumentsInReplayOrder.clear();
      }
      else {
         for (int i = expectationsInReplayOrder.size() - 1; i >= 0; i--) {
            Expectation expectation = expectationsInReplayOrder.get(i);

            if (isInvocationToBeVerified(expectation.invocation)) {
               expectationsInReplayOrder.remove(i);
               invocationArgumentsInReplayOrder.remove(i);
            }
         }
      }
   }

   @Nonnull
   public final <T> List<T> getNewInstancesMatchingVerifiedConstructorInvocation()
   {
      List<T> newInstances = new ArrayList<T>();

      for (VerifiedExpectation verifiedExpectation : currentVerifiedExpectations) {
         //noinspection unchecked
         T newInstance = (T) verifiedExpectation.captureNewInstance();
         newInstances.add(newInstance);
      }

      return newInstances;
   }
}
