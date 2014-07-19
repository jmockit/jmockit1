/*
 * Copyright (c) 2006-2014 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.expectations;

import java.util.*;

import org.jetbrains.annotations.*;

import mockit.internal.*;
import mockit.internal.expectations.argumentMatching.*;
import mockit.internal.expectations.invocation.*;
import mockit.internal.util.*;

public abstract class BaseVerificationPhase extends TestOnlyPhase
{
   @NotNull final List<Expectation> expectationsInReplayOrder;
   @NotNull final List<Object> invocationInstancesInReplayOrder;
   @NotNull final List<Object[]> invocationArgumentsInReplayOrder;
   private boolean allMockedInvocationsDuringReplayMustBeVerified;
   @Nullable private Object[] mockedTypesAndInstancesToFullyVerify;
   @NotNull private final List<VerifiedExpectation> currentVerifiedExpectations;
   @Nullable private Expectation currentVerification;
   protected int replayIndex;
   @Nullable protected Error pendingError;

   protected BaseVerificationPhase(
      @NotNull RecordAndReplayExecution recordAndReplay,
      @NotNull List<Expectation> expectationsInReplayOrder,
      @NotNull List<Object> invocationInstancesInReplayOrder,
      @NotNull List<Object[]> invocationArgumentsInReplayOrder)
   {
      super(recordAndReplay);
      this.expectationsInReplayOrder = expectationsInReplayOrder;
      this.invocationInstancesInReplayOrder = invocationInstancesInReplayOrder;
      this.invocationArgumentsInReplayOrder = invocationArgumentsInReplayOrder;
      currentVerifiedExpectations = new ArrayList<VerifiedExpectation>();
   }

   public final void setAllInvocationsMustBeVerified() { allMockedInvocationsDuringReplayMustBeVerified = true; }

   public final void setMockedTypesToFullyVerify(@NotNull Object[] mockedTypesAndInstancesToFullyVerify)
   {
      this.mockedTypesAndInstancesToFullyVerify = mockedTypesAndInstancesToFullyVerify;
   }

   @NotNull
   protected final Expectation expectationBeingVerified()
   {
      if (currentVerification == null) {
         throw new IllegalStateException(
            "Missing invocation to mocked type at this point; please make sure there is an associated " +
            "mock field or mock parameter in scope");
      }

      return currentVerification;
   }

   @Override
   @Nullable final Object handleInvocation(
      @Nullable Object mock, int mockAccess, @NotNull String mockClassDesc, @NotNull String mockNameAndDesc,
      @Nullable String genericSignature, boolean withRealImpl, @NotNull Object[] args)
   {
      if (pendingError != null) {
         recordAndReplay.setErrorThrown(pendingError);
         pendingError = null;
         return null;
      }

      matchInstance = nextInstanceToMatch != null && mock == nextInstanceToMatch;

      ExpectedInvocation currentInvocation = new ExpectedInvocation(
         mock, mockAccess, mockClassDesc, mockNameAndDesc, matchInstance, genericSignature, args);
      currentInvocation.arguments.setMatchers(argMatchers);
      currentVerification = new Expectation(null, currentInvocation, true);

      currentExpectation = null;
      currentVerifiedExpectations.clear();
      findNonStrictExpectation(mock, mockClassDesc, mockNameAndDesc, args);
      argMatchers = null;

      if (matchInstance) {
         nextInstanceToMatch = null;
      }

      if (recordAndReplay.getErrorThrown() != null) {
         return null;
      }

      if (currentExpectation == null) {
         pendingError = currentVerification.invocation.errorForMissingInvocation();
         currentExpectation = currentVerification;
      }

      return currentExpectation.invocation.getDefaultValueForReturnType(this);
   }

   abstract void findNonStrictExpectation(
      @Nullable Object mock, @NotNull String mockClassDesc, @NotNull String mockNameAndDesc, @NotNull Object[] args);

   final boolean matches(
      @Nullable Object mock, @NotNull String mockClassDesc, @NotNull String mockNameAndDesc, @NotNull Object[] args,
      @NotNull Expectation replayExpectation, @Nullable Object replayInstance, @NotNull Object[] replayArgs)
   {
      ExpectedInvocation invocation = replayExpectation.invocation;
      Map<Object, Object> instanceMap = getInstanceMap();

      if (
         invocation.isMatch(mock, mockClassDesc, mockNameAndDesc) &&
         (!matchInstance || invocation.isEquivalentInstance(mock))
      ) {
         Object[] originalArgs = invocation.arguments.prepareForVerification(args, argMatchers);
         boolean argumentsMatch = invocation.arguments.isMatch(replayArgs, instanceMap);
         invocation.arguments.setValuesWithNoMatchers(originalArgs);

         if (argumentsMatch) {
            if (mockNameAndDesc.charAt(0) == '<') {
               instanceMap.put(replayInstance, mock);
            }

            addVerifiedExpectation(replayExpectation, replayArgs, argMatchers);
            return true;
         }
      }

      return false;
   }

   private void addVerifiedExpectation(
      @NotNull Expectation expectation, @NotNull Object[] args, @Nullable List<ArgumentMatcher> matchers)
   {
      int i = expectationsInReplayOrder.indexOf(expectation);
      addVerifiedExpectation(new VerifiedExpectation(expectation, args, matchers, i));
   }

   void addVerifiedExpectation(@NotNull VerifiedExpectation verifiedExpectation)
   {
      recordAndReplay.executionState.verifiedExpectations.add(verifiedExpectation);
      currentVerifiedExpectations.add(verifiedExpectation);
   }

   @Override
   public final void setMaxInvocationCount(int maxInvocations)
   {
      if (maxInvocations == 0 || pendingError == null) {
         super.setMaxInvocationCount(maxInvocations);
      }
   }

   @Override
   public final void setCustomErrorMessage(@Nullable CharSequence customMessage)
   {
      Expectation expectation = getCurrentExpectation();

      if (pendingError == null) {
         expectation.setCustomErrorMessage(customMessage);
      }
      else if (customMessage != null) {
         String finalMessage = customMessage + "\n" + pendingError.getMessage();
         StackTraceElement[] previousStackTrace = pendingError.getStackTrace();
         pendingError = pendingError instanceof MissingInvocation ?
            new MissingInvocation(finalMessage) : new UnexpectedInvocation(finalMessage);
         pendingError.setStackTrace(previousStackTrace);
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

   private static boolean isEligibleForFullVerification(@NotNull Expectation replayExpectation)
   {
      return !replayExpectation.executedRealImplementation && replayExpectation.constraints.minInvocations <= 0;
   }

   private boolean wasVerified(@NotNull Expectation replayExpectation, @NotNull Object[] replayArgs)
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
   private Error validateThatUnverifiedInvocationsAreAllowed(@NotNull List<Expectation> unverified)
   {
      for (Expectation expectation : unverified) {
         ExpectedInvocation invocation = expectation.invocation;

         if (isInvocationToBeVerified(invocation)) {
            return invocation.errorForUnexpectedInvocation();
         }
      }

      return null;
   }

   private boolean isInvocationToBeVerified(@NotNull ExpectedInvocation unverifiedInvocation)
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

   @NotNull
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
