/*
 * Copyright (c) 2006-2014 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.expectations;

import java.util.*;
import java.util.concurrent.locks.*;

import mockit.*;
import mockit.internal.expectations.invocation.*;
import mockit.internal.expectations.mocking.*;
import mockit.internal.startup.*;
import mockit.internal.state.*;
import mockit.internal.util.*;
import static mockit.internal.util.Utilities.*;

import org.jetbrains.annotations.*;

public final class RecordAndReplayExecution
{
   public static final ReentrantLock RECORD_OR_REPLAY_LOCK = new ReentrantLock();
   public static final ReentrantLock TEST_ONLY_PHASE_LOCK = new ReentrantLock();

   @Nullable private final DynamicPartialMocking dynamicPartialMocking;

   @NotNull final PhasedExecutionState executionState;
   final int lastExpectationIndexInPreviousReplayPhase;

   @NotNull final FailureState failureState;

   @Nullable private RecordPhase recordPhase;
   @Nullable private ReplayPhase replayPhase;
   @Nullable private BaseVerificationPhase verificationPhase;

   public RecordAndReplayExecution()
   {
      validateRecordingContext(false);
      executionState = new PhasedExecutionState();
      lastExpectationIndexInPreviousReplayPhase = 0;
      dynamicPartialMocking = null;
      discoverMockedTypesAndInstancesForMatchingOnInstance();
      failureState = new FailureState();
      replayPhase = new ReplayPhase(this);
   }

   private int getLastExpectationIndexInPreviousReplayPhase()
   {
      return replayPhase == null ? -1 : replayPhase.currentStrictExpectationIndex;
   }

   private static void validateRecordingContext(boolean recordingExpectations)
   {
      if (TestRun.getSharedFieldTypeRedefinitions() == null) {
         String msg;

         if (Startup.wasInitializedOnDemand()) {
            msg =
               "JMockit wasn't properly initialized; check that jmockit.jar precedes junit.jar in the classpath " +
               "(if using JUnit; if not, check the documentation)";
         }
         else if (recordingExpectations) {
            msg = "Invalid place to record expectations";
         }
         else {
            msg = "Invalid place to verify expectations";
         }

         IllegalStateException failure = new IllegalStateException(msg);
         StackTrace.filterStackTrace(failure);
         throw failure;
      }
   }

   public RecordAndReplayExecution(
      @NotNull Expectations targetObject, @Nullable Object... classesOrInstancesToBePartiallyMocked)
   {
      validateRecordingContext(true);

      TestRun.enterNoMockingZone();
      ExecutingTest executingTest = TestRun.getExecutingTest();
      executingTest.setShouldIgnoreMockingCallbacks(true);

      try {
         RecordAndReplayExecution previous = executingTest.getPreviousRecordAndReplay();

         if (previous == null) {
            executionState = new PhasedExecutionState();
            lastExpectationIndexInPreviousReplayPhase = 0;
         }
         else {
            executionState = previous.executionState;
            lastExpectationIndexInPreviousReplayPhase = previous.getLastExpectationIndexInPreviousReplayPhase();
         }

         failureState = new FailureState();

         boolean nonStrict = targetObject instanceof NonStrictExpectations;
         recordPhase = new RecordPhase(this, nonStrict);

         executingTest.setRecordAndReplay(this);

         dynamicPartialMocking = applyDynamicPartialMocking(nonStrict, classesOrInstancesToBePartiallyMocked);

         discoverMockedTypesAndInstancesForMatchingOnInstance();

         //noinspection LockAcquiredButNotSafelyReleased
         TEST_ONLY_PHASE_LOCK.lock();
      }
      catch (RuntimeException e) {
         executingTest.setRecordAndReplay(null);
         throw e;
      }
      finally {
         executingTest.setShouldIgnoreMockingCallbacks(false);
         TestRun.exitNoMockingZone();
      }
   }

   private void discoverMockedTypesAndInstancesForMatchingOnInstance()
   {
      SharedFieldTypeRedefinitions fieldTypeRedefinitions = TestRun.getSharedFieldTypeRedefinitions();
      assert fieldTypeRedefinitions != null;
      List<Class<?>> fields = fieldTypeRedefinitions.getTargetClasses();
      List<Class<?>> targetClasses = new ArrayList<Class<?>>(fields);

      ParameterTypeRedefinitions paramTypeRedefinitions = TestRun.getExecutingTest().getParameterTypeRedefinitions();

      if (paramTypeRedefinitions != null) {
         targetClasses.addAll(paramTypeRedefinitions.getTargetClasses());
      }

      executionState.discoverMockedTypesToMatchOnInstances(targetClasses);

      if (dynamicPartialMocking != null) {
         executionState.setDynamicMockInstancesToMatch(dynamicPartialMocking.targetInstances);
      }
   }

   @Nullable private static DynamicPartialMocking applyDynamicPartialMocking(
      boolean nonStrict, @Nullable Object... classesOrInstances)
   {
      if (classesOrInstances == null || classesOrInstances.length == 0) {
         return null;
      }

      DynamicPartialMocking mocking = new DynamicPartialMocking(nonStrict);
      mocking.redefineTypes(classesOrInstances);
      return mocking;
   }

   @NotNull public RecordPhase getRecordPhase()
   {
      if (recordPhase == null) {
         throw new IllegalStateException("Not in the recording phase");
      }

      return recordPhase;
   }

   @Nullable Error getErrorThrown() { return failureState.getErrorThrown(); }
   void setErrorThrown(@Nullable Error error) { failureState.setErrorThrown(error); }

   /**
    * Only to be called from generated bytecode or from the Mocking Bridge.
    */
   @Nullable
   public static Object recordOrReplay(
      @Nullable Object mock, int mockAccess, @NotNull String classDesc, @NotNull String mockDesc,
      @Nullable String genericSignature, int executionMode, @Nullable Object[] args)
      throws Throwable
   {
      if (args == null) {
         //noinspection AssignmentToMethodParameter
         args = NO_ARGS;
      }

      if (
         RECORD_OR_REPLAY_LOCK.isHeldByCurrentThread() ||
         TEST_ONLY_PHASE_LOCK.isLocked() && !TEST_ONLY_PHASE_LOCK.isHeldByCurrentThread()
      ) {
         // This occurs if called from a custom argument matching method, in a call to an overridden Object method
         // (equals, hashCode, toString), or from a different thread during a recording/verification.
         return defaultReturnValue(mock, classDesc, mockDesc, genericSignature, executionMode, args);
      }

      ExecutingTest executingTest = TestRun.getExecutingTest();

      if (executingTest.isShouldIgnoreMockingCallbacks()) {
         // This occurs when called from a reentrant delegate method, or during static initialization of a mocked class.
         return defaultReturnValue(executingTest, mock, classDesc, mockDesc, genericSignature, executionMode, args);
      }

      if (
         executingTest.shouldProceedIntoRealImplementation(mock, classDesc) ||
         executionMode == 2 && (mock == null || !executingTest.isMockedInstance(mock)) ||
         executionMode == 3 && mock != null && !TestRun.mockFixture().isInstanceOfMockedClass(mock)
      ) {
         return Void.class;
      }

      RECORD_OR_REPLAY_LOCK.lock();

      try {
         boolean isConstructor = mock != null && mockDesc.startsWith("<init>");
         RecordAndReplayExecution instance = executingTest.getOrCreateRecordAndReplay();

         if (isConstructor && handleCallToConstructor(instance, mock, classDesc)) {
            return
               executionMode == 3 || executionMode == 1 && instance.replayPhase == null ||
               executingTest.isInjectableMock(mock) ?
                  null : Void.class;
         }

         Phase currentPhase = instance.getCurrentPhase();
         instance.failureState.clearErrorThrown();

         boolean withRealImpl = executionMode == 1;
         Object result =
            currentPhase.handleInvocation(mock, mockAccess, classDesc, mockDesc, genericSignature, withRealImpl, args);

         instance.failureState.reportErrorThrownIfAny();

         return result;
      }
      finally {
         RECORD_OR_REPLAY_LOCK.unlock();
      }
   }

   @Nullable
   public static Object defaultReturnValue(
      @Nullable Object mock, @NotNull String classDesc, @NotNull String nameAndDesc,
      @Nullable String genericSignature, int executionMode, @NotNull Object[] args)
   {
      if (executionMode == 1) {
         return Void.class;
      }

      if (mock != null) {
         Object rv = ObjectMethods.evaluateOverride(mock, nameAndDesc, args);

         if (rv != null) {
            return rv;
         }
      }

      String returnTypeDesc = DefaultValues.getReturnTypeDesc(nameAndDesc);
      Object cascadedInstance =
         MockedTypeCascade.getMock(classDesc, nameAndDesc, mock, returnTypeDesc, genericSignature);

      if (cascadedInstance != null) {
         return cascadedInstance;
      }

      return Void.class;
   }

   @Nullable
   private static Object defaultReturnValue(
      @NotNull ExecutingTest executingTest, @Nullable Object mock,
      @NotNull String classDesc, @NotNull String nameAndDesc, @Nullable String genericSignature,
      int executionMode, @NotNull Object[] args) throws Throwable
   {
      RecordAndReplayExecution execution = executingTest.getCurrentRecordAndReplay();

      if (execution != null) {
         Expectation recordedExpectation =
            execution.executionState.findNonStrictExpectation(mock, classDesc, nameAndDesc, args);

         if (recordedExpectation != null) {
            return recordedExpectation.produceResult(mock, args);
         }
      }

      return defaultReturnValue(mock, classDesc, nameAndDesc, genericSignature, executionMode, args);
   }

   private static boolean handleCallToConstructor(
      @NotNull RecordAndReplayExecution instance, @NotNull Object mock, @NotNull String classDesc)
   {
      if (instance.replayPhase != null) {
         ParameterTypeRedefinitions paramTypeRedefinitions = TestRun.getExecutingTest().getParameterTypeRedefinitions();

         if (paramTypeRedefinitions != null) {
            CaptureOfNewInstances paramTypeCaptures = paramTypeRedefinitions.getCaptureOfNewInstances();

            if (paramTypeCaptures != null && paramTypeCaptures.captureNewInstance(null, mock)) {
               return true;
            }
         }

         FieldTypeRedefinitions sharedFieldTypeRedefs = TestRun.getSharedFieldTypeRedefinitions();
         assert sharedFieldTypeRedefs != null;

         if (sharedFieldTypeRedefs.captureNewInstanceForApplicableMockField(mock)) {
            return true;
         }
      }

      return isCallToSuperClassConstructor(mock, classDesc);
   }

   private static boolean isCallToSuperClassConstructor(@NotNull Object mock, @NotNull String calledClassDesc)
   {
      Class<?> mockedClass = mock.getClass();

      if (ClassNaming.isAnonymousClass(mockedClass)) {
         // An anonymous class instantiation always invokes the constructor on the super-class,
         // so that is the class we need to consider, not the anonymous one.
         mockedClass = mockedClass.getSuperclass();

         if (mockedClass == Object.class) {
            return false;
         }
      }

      String calledClassName = calledClassDesc.replace('/', '.');

      return !calledClassName.equals(mockedClass.getName());
   }

   @NotNull private Phase getCurrentPhase()
   {
      ReplayPhase replay = replayPhase;

      if (replay == null) {
         assert recordPhase != null;
         return recordPhase;
      }

      BaseVerificationPhase verification = verificationPhase;

      if (verification == null) {
         if (failureState.getErrorThrown() != null) {
            // This can only happen when called from a catch/finally block.
            throw failureState.getErrorThrown();
         }

         return replay;
      }

      return verification;
   }

   @NotNull public BaseVerificationPhase startVerifications(boolean inOrder)
   {
      if (replayPhase == null) {
         throw new IllegalStateException("Not in the replay phase yet");
      }

      List<Expectation> expectations = replayPhase.nonStrictInvocations;
      List<Object> invocationInstances = replayPhase.nonStrictInvocationInstances;
      List<Object[]> invocationArguments = replayPhase.nonStrictInvocationArguments;
      verificationPhase =
         inOrder ?
            new OrderedVerificationPhase(this, expectations, invocationInstances, invocationArguments) :
            new UnorderedVerificationPhase(this, expectations, invocationInstances, invocationArguments);

      return verificationPhase;
   }

   @Nullable public static Error endCurrentReplayIfAny()
   {
      RecordAndReplayExecution instance = TestRun.getRecordAndReplayForRunningTest();
      return instance == null ? null : instance.endExecution();
   }

   @Nullable private Error endExecution()
   {
      if (TEST_ONLY_PHASE_LOCK.isLocked()) {
         TEST_ONLY_PHASE_LOCK.unlock();
      }

      switchFromRecordToReplayIfNotYet();

      assert replayPhase != null;
      Error error = replayPhase.endExecution();

      if (error == null) {
         error = failureState.getErrorThrownInAnotherThreadIfAny();
      }

      if (error == null && verificationPhase != null) {
         error = verificationPhase.endVerification();
         verificationPhase = null;
      }

      return error;
   }

   private void switchFromRecordToReplayIfNotYet()
   {
      if (replayPhase == null) {
         recordPhase = null;
         replayPhase = new ReplayPhase(this);
      }
   }

   @NotNull public TestOnlyPhase getCurrentTestOnlyPhase()
   {
      if (recordPhase != null) return recordPhase;
      assert verificationPhase != null;
      return verificationPhase;
   }

   public void endInvocations()
   {
      TEST_ONLY_PHASE_LOCK.unlock();

      if (verificationPhase == null) {
         switchFromRecordToReplayIfNotYet();
      }
      else {
         Error error = verificationPhase.endVerification();
         verificationPhase = null;

         if (error != null) {
            throw error;
         }
      }
   }
}
