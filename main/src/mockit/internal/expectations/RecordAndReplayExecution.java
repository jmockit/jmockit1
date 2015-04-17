/*
 * Copyright (c) 2006-2015 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.expectations;

import java.util.*;
import java.util.concurrent.locks.*;

import javax.annotation.*;

import mockit.*;
import mockit.integration.junit4.internal.*;
import mockit.internal.expectations.invocation.*;
import mockit.internal.expectations.mocking.*;
import mockit.internal.startup.*;
import mockit.internal.state.*;
import mockit.internal.util.*;
import static mockit.internal.util.Utilities.*;

public final class RecordAndReplayExecution
{
//   public static final ReentrantLock RECORD_OR_REPLAY_LOCK = new ReentrantLock();
//   public static final ReentrantLock TEST_ONLY_PHASE_LOCK = new ReentrantLock();

   public static final ThreadLocal<ReentrantLock> RECORD_OR_REPLAY_LOCKS = new ThreadLocal<ReentrantLock>() {
    @Override
    protected ReentrantLock initialValue() {
        return new ReentrantLock();
    }
   };

   public static final ThreadLocal<ReentrantLock> TEST_ONLY_PHASE_LOCKS = new ThreadLocal<ReentrantLock>() {
      @Override
      protected ReentrantLock initialValue() {
          return new ReentrantLock();
      }
   };

   @Nullable private final DynamicPartialMocking dynamicPartialMocking;

   @Nonnull final PhasedExecutionState executionState;
   final int lastExpectationIndexInPreviousReplayPhase;

   @Nonnull final FailureState failureState;

   @Nullable private RecordPhase recordPhase;
   @Nullable private ReplayPhase replayPhase;
   @Nullable private BaseVerificationPhase verificationPhase;

   public RecordAndReplayExecution()
   {
      validateRecordingContext();
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

   private static void validateRecordingContext()
   {
      if (TestRun.getFieldTypeRedefinitions() == null) {
         String msg;

         if (Startup.wasInitializedOnDemand()) {
            msg = "JMockit wasn't properly initialized; please ";

            if (MockFrameworkMethod.hasDependenciesInClasspath()) {
               msg += "ensure that jmockit precedes junit in the runtime classpath, or use @RunWith(JMockit.class)";
            }
            else {
               msg += "check the documentation";
            }
         }
         else {
            msg = "Invalid place to record expectations";
         }

         IllegalStateException failure = new IllegalStateException(msg);
         StackTrace.filterStackTrace(failure);
         throw failure;
      }
   }

   public RecordAndReplayExecution(
      @Nonnull Expectations targetObject, @Nullable Object... classesOrInstancesToBePartiallyMocked)
   {
      validateRecordingContext();

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

         boolean strict = targetObject instanceof StrictExpectations;
         boolean nonStrict = targetObject instanceof NonStrictExpectations;
         recordPhase = new RecordPhase(this, strict, nonStrict);

         executingTest.setRecordAndReplay(this);
         dynamicPartialMocking = applyDynamicPartialMocking(classesOrInstancesToBePartiallyMocked);
         discoverMockedTypesAndInstancesForMatchingOnInstance();

         //noinspection LockAcquiredButNotSafelyReleased
         TEST_ONLY_PHASE_LOCKS.get().lock();
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
      TypeRedefinitions fieldTypeRedefinitions = TestRun.getFieldTypeRedefinitions();
      assert fieldTypeRedefinitions != null;
      List<Class<?>> fields = fieldTypeRedefinitions.getTargetClasses();
      List<Class<?>> targetClasses = new ArrayList<Class<?>>(fields);

      TypeRedefinitions paramTypeRedefinitions = TestRun.getExecutingTest().getParameterRedefinitions();

      if (paramTypeRedefinitions != null) {
         targetClasses.addAll(paramTypeRedefinitions.getTargetClasses());
      }

      executionState.discoverMockedTypesToMatchOnInstances(targetClasses);

      if (dynamicPartialMocking != null && !dynamicPartialMocking.targetInstances.isEmpty()) {
         executionState.setDynamicMockInstancesToMatch(dynamicPartialMocking.targetInstances);
      }
   }

   @Nullable
   private static DynamicPartialMocking applyDynamicPartialMocking(@Nullable Object... classesOrInstances)
   {
      if (classesOrInstances == null || classesOrInstances.length == 0) {
         return null;
      }

      DynamicPartialMocking mocking = new DynamicPartialMocking();
      mocking.redefineTypes(classesOrInstances);
      return mocking;
   }

   @Nonnull
   public RecordPhase getRecordPhase()
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
      @Nullable Object mock, int mockAccess, @Nonnull String classDesc, @Nonnull String mockDesc,
      @Nullable String genericSignature, int executionModeOrdinal, @Nullable Object[] args)
      throws Throwable
   {
      if (args == null) {
         //noinspection AssignmentToMethodParameter
         args = NO_ARGS;
      }

      ExecutionMode executionMode = ExecutionMode.values()[executionModeOrdinal];

      if (
         RECORD_OR_REPLAY_LOCKS.get().isHeldByCurrentThread() ||
         TEST_ONLY_PHASE_LOCKS.get().isLocked() && !TEST_ONLY_PHASE_LOCKS.get().isHeldByCurrentThread() ||
         !TestRun.mockFixture().isStillMocked(mock, classDesc)
      ) {
         // This occurs if called from a custom argument matching method, in a call to an overridden Object method
         // (equals, hashCode, toString), from a different thread during recording/verification, or during replay but
         // between tests.
         return defaultReturnValue(mock, classDesc, mockDesc, genericSignature, executionMode, args);
      }

      ExecutingTest executingTest = TestRun.getExecutingTest();

      if (executingTest.isShouldIgnoreMockingCallbacks()) {
         // This occurs when called from a reentrant delegate method, or during static initialization of a mocked class.
         return defaultReturnValue(executingTest, mock, classDesc, mockDesc, genericSignature, executionMode, args);
      }

      if (
         executingTest.shouldProceedIntoRealImplementation(mock, classDesc) ||
         executionMode.isToExecuteRealImplementation(mock)
      ) {
         return Void.class;
      }

      RECORD_OR_REPLAY_LOCKS.get().lock();

      try {
         boolean isConstructor = mock != null && mockDesc.startsWith("<init>");
         RecordAndReplayExecution instance = executingTest.getOrCreateRecordAndReplay();

         if (isConstructor && handleCallToConstructor(instance, mock, classDesc)) {
            return
               executionMode == ExecutionMode.Regular ||
               executionMode == ExecutionMode.Partial && instance.replayPhase == null ||
               executingTest.isInjectableMock(mock) ?
                  null : Void.class;
         }

         Phase currentPhase = instance.getCurrentPhase();
         instance.failureState.clearErrorThrown();

         boolean withRealImpl = executionMode.isWithRealImplementation(mock);
         Object result =
            currentPhase.handleInvocation(mock, mockAccess, classDesc, mockDesc, genericSignature, withRealImpl, args);

         instance.failureState.reportErrorThrownIfAny();

         return result;
      }
      finally {
          RECORD_OR_REPLAY_LOCKS.get().unlock();
      }
   }

   @Nullable
   private static Object defaultReturnValue(
      @Nullable Object mock, @Nonnull String classDesc, @Nonnull String nameAndDesc,
      @Nullable String genericSignature, @Nonnull ExecutionMode executionMode, @Nonnull Object[] args)
   {
      if (executionMode.isToExecuteRealImplementation(mock)) {
         return Void.class;
      }

      if (mock != null) {
         Object rv = ObjectMethods.evaluateOverride(mock, nameAndDesc, args);

         if (rv != null) {
            return executionMode.isToExecuteRealObjectOverride(mock) ? Void.class : rv;
         }
      }

      String returnTypeDesc = DefaultValues.getReturnTypeDesc(nameAndDesc);

      if (returnTypeDesc.charAt(0) == 'L') {
         ExpectedInvocation invocation = new ExpectedInvocation(mock, classDesc, nameAndDesc, genericSignature, args);
         Object cascadedInstance = invocation.getDefaultValueForReturnType(null);

         if (cascadedInstance != null) {
            return cascadedInstance;
         }
      }

      return Void.class;
   }

   @Nullable
   private static Object defaultReturnValue(
      @Nonnull ExecutingTest executingTest, @Nullable Object mock,
      @Nonnull String classDesc, @Nonnull String nameAndDesc, @Nullable String genericSignature,
      @Nonnull ExecutionMode executionMode, @Nonnull Object[] args) throws Throwable
   {
      RecordAndReplayExecution execution = executingTest.getCurrentRecordAndReplay();

      if (execution != null) {
         Expectation recordedExpectation =
            execution.executionState.findNotStrictExpectation(mock, classDesc, nameAndDesc, args);

         if (recordedExpectation != null) {
            return recordedExpectation.produceResult(mock, args);
         }
      }

      return defaultReturnValue(mock, classDesc, nameAndDesc, genericSignature, executionMode, args);
   }

   private static boolean handleCallToConstructor(
      @Nonnull RecordAndReplayExecution instance, @Nonnull Object mock, @Nonnull String classDesc)
   {
      if (instance.replayPhase != null) {
         TypeRedefinitions paramTypeRedefinitions = TestRun.getExecutingTest().getParameterRedefinitions();

         if (paramTypeRedefinitions != null) {
            CaptureOfNewInstances paramTypeCaptures = paramTypeRedefinitions.getCaptureOfNewInstances();

            if (paramTypeCaptures != null && paramTypeCaptures.captureNewInstance(null, mock)) {
               return true;
            }
         }

         FieldTypeRedefinitions fieldTypeRedefinitions = TestRun.getFieldTypeRedefinitions();
         assert fieldTypeRedefinitions != null;

         if (fieldTypeRedefinitions.captureNewInstanceForApplicableMockField(mock)) {
            return true;
         }
      }

      return isCallToSuperClassConstructor(mock, classDesc);
   }

   private static boolean isCallToSuperClassConstructor(@Nonnull Object mock, @Nonnull String calledClassDesc)
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

   @Nonnull
   private Phase getCurrentPhase()
   {
      ReplayPhase replay = replayPhase;

      if (replay == null) {
         RecordPhase record = recordPhase;
         assert record != null;
         return record;
      }

      BaseVerificationPhase verification = verificationPhase;

      if (verification != null) {
         return verification;
      }

      return replay;
   }

   @Nonnull
   public BaseVerificationPhase startVerifications(boolean inOrder)
   {
      assert replayPhase != null;
      List<Expectation> expectations = replayPhase.invocations;
      List<Object> invocationInstances = replayPhase.invocationInstances;
      List<Object[]> invocationArguments = replayPhase.invocationArguments;

      verificationPhase =
         inOrder ?
            new OrderedVerificationPhase(this, expectations, invocationInstances, invocationArguments) :
            new UnorderedVerificationPhase(this, expectations, invocationInstances, invocationArguments);

      return verificationPhase;
   }

   @Nullable
   public static Error endCurrentReplayIfAny()
   {
      RecordAndReplayExecution instance = TestRun.getRecordAndReplayForRunningTest();
      return instance == null ? null : instance.endExecution();
   }

   @Nullable
   private Error endExecution()
   {
      if (TEST_ONLY_PHASE_LOCKS.get().isLocked()) {
         TEST_ONLY_PHASE_LOCKS.get().unlock();
      }

      ReplayPhase replay = switchFromRecordToReplayIfNotYet();
      Error error = replay.endExecution();

      if (error == null) {
         error = failureState.getErrorThrownInAnotherThreadIfAny();
      }

      if (error == null && verificationPhase != null) {
         error = verificationPhase.endVerification();
         verificationPhase = null;
      }

      return error;
   }

   @Nonnull
   private ReplayPhase switchFromRecordToReplayIfNotYet()
   {
      if (replayPhase == null) {
         recordPhase = null;
         replayPhase = new ReplayPhase(this);
      }

      return replayPhase;
   }

   @Nonnull
   public TestOnlyPhase getCurrentTestOnlyPhase()
   {
      if (recordPhase != null) return recordPhase;
      assert verificationPhase != null;
      return verificationPhase;
   }

   public void endInvocations()
   {
      TEST_ONLY_PHASE_LOCKS.get().unlock();

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
