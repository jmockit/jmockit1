/*
 * Copyright (c) 2006-2014 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.state;

import java.util.*;

import mockit.external.asm.*;
import mockit.internal.*;
import mockit.internal.expectations.*;
import mockit.internal.expectations.mocking.*;
import static mockit.internal.util.Utilities.*;

import org.jetbrains.annotations.*;

@SuppressWarnings("ClassWithTooManyFields")
public final class ExecutingTest
{
   @Nullable private RecordAndReplayExecution currentRecordAndReplay;
   @Nullable private RecordAndReplayExecution recordAndReplayForLastTestMethod;

   @NotNull private final ThreadLocal<Boolean> shouldIgnoreMockingCallbacks;
   @NotNull private final ThreadLocal<BaseInvocation> proceedingInvocation;
   private boolean proceeding;

   @Nullable private ParameterTypeRedefinitions parameterTypeRedefinitions;

   @NotNull private final List<Object> regularMocks;
   @NotNull private final List<Object> injectableMocks;
   @NotNull private final List<Object> nonStrictMocks;
   @NotNull private final List<Object> strictMocks;
   @NotNull private final Map<Object, Object> originalToCapturedInstance;
   @NotNull private final CascadingTypes cascadingTypes;

   ExecutingTest()
   {
      shouldIgnoreMockingCallbacks = new ThreadLocal<Boolean>() {
         @Override protected Boolean initialValue() { return false; }
      };
      proceedingInvocation = new ThreadLocal<BaseInvocation>();
      regularMocks = new ArrayList<Object>();
      injectableMocks = new ArrayList<Object>();
      nonStrictMocks = new ArrayList<Object>();
      strictMocks = new ArrayList<Object>();
      originalToCapturedInstance = new IdentityHashMap<Object, Object>(4);
      cascadingTypes = new CascadingTypes();
   }

   @NotNull public RecordAndReplayExecution getOrCreateRecordAndReplay()
   {
      if (currentRecordAndReplay == null) {
         setRecordAndReplay(new RecordAndReplayExecution());
      }

      return currentRecordAndReplay;
   }

   @Nullable public RecordAndReplayExecution getPreviousRecordAndReplay()
   {
      RecordAndReplayExecution previous = currentRecordAndReplay;
      currentRecordAndReplay = null;
      return previous;
   }

   public void setRecordAndReplay(@Nullable RecordAndReplayExecution newRecordAndReplay)
   {
      recordAndReplayForLastTestMethod = null;
      currentRecordAndReplay = newRecordAndReplay;
   }

   @Nullable public RecordAndReplayExecution getCurrentRecordAndReplay() { return currentRecordAndReplay; }

   public boolean isShouldIgnoreMockingCallbacks() { return shouldIgnoreMockingCallbacks.get(); }

   public boolean setShouldIgnoreMockingCallbacks(boolean flag)
   {
      boolean previousFlag = shouldIgnoreMockingCallbacks.get();
      shouldIgnoreMockingCallbacks.set(flag);
      return previousFlag;
   }

   public boolean isProceedingIntoRealImplementation()
   {
      boolean result = proceeding;
      proceeding = false;
      return result;
   }

   public void markAsProceedingIntoRealImplementation() { proceeding = true; }

   @Nullable public BaseInvocation getProceedingInvocation() { return proceedingInvocation.get(); }

   public void markAsProceedingIntoRealImplementation(@NotNull BaseInvocation invocation)
   {
      BaseInvocation previousInvocation = proceedingInvocation.get();

      if (previousInvocation != null) {
         invocation.setPreviousInvocation(previousInvocation);
      }

      proceedingInvocation.set(invocation);
      proceeding = true;
   }

   public boolean shouldProceedIntoRealImplementation(@Nullable Object mock, @NotNull String classDesc)
   {
      BaseInvocation pendingInvocation = proceedingInvocation.get();

      if (pendingInvocation != null && pendingInvocation.isMethodInSuperclass(mock, classDesc)) {
         return true;
      }

      return isProceedingIntoRealImplementation();
   }

   public void clearProceedingState()
   {
      BaseInvocation pendingInvocation = proceedingInvocation.get();
      BaseInvocation previousInvocation = pendingInvocation.getPreviousInvocation();
      proceedingInvocation.set(previousInvocation);
   }

   @NotNull RecordAndReplayExecution getRecordAndReplayForVerifications()
   {
      if (currentRecordAndReplay == null) {
         if (recordAndReplayForLastTestMethod != null) {
            currentRecordAndReplay = recordAndReplayForLastTestMethod;
         }
         else {
            // This should only happen if no expectations at all were created by the whole test, but
            // there is one (probably empty) verification block.
            currentRecordAndReplay = new RecordAndReplayExecution();
         }
      }

      //noinspection LockAcquiredButNotSafelyReleased
      RecordAndReplayExecution.TEST_ONLY_PHASE_LOCK.lock();

      return currentRecordAndReplay;
   }

   @Nullable public ParameterTypeRedefinitions getParameterTypeRedefinitions() { return parameterTypeRedefinitions; }

   public void setParameterTypeRedefinitions(
      @SuppressWarnings("NullableProblems") @NotNull ParameterTypeRedefinitions redefinitions)
   {
      parameterTypeRedefinitions = redefinitions;
   }

   public void clearInjectableAndNonStrictMocks()
   {
      regularMocks.clear();
      injectableMocks.clear();
      nonStrictMocks.clear();
      originalToCapturedInstance.clear();
   }

   public void addInjectableMock(@NotNull Object mock)
   {
      if (!isInjectableMock(mock)) {
         injectableMocks.add(mock);
      }
   }

   public boolean isInjectableMock(@NotNull Object instance)
   {
      return containsReference(injectableMocks, instance);
   }

   public boolean isClassWithInjectableMocks(@NotNull Class<?> aClass)
   {
      for (int i = 0, n = injectableMocks.size(); i < n; i++) {
         Object injectableMock = injectableMocks.get(i);

         if (injectableMock.getClass() == aClass) {
            return true;
         }
      }

      return false;
   }

   public boolean isMockedInstance(@NotNull Object instance)
   {
      return containsReference(regularMocks, instance) || isInjectableMock(instance);
   }

   public void addCapturedInstanceForInjectableMock(@Nullable Object originalInstance, @NotNull Object capturedInstance)
   {
      injectableMocks.add(capturedInstance);
      addCapturedInstance(originalInstance, capturedInstance);
   }

   public void addCapturedInstance(@Nullable Object originalInstance, @NotNull Object capturedInstance)
   {
      originalToCapturedInstance.put(capturedInstance, originalInstance);
   }

   public boolean isInvokedInstanceEquivalentToCapturedInstance(
      @Nullable Object invokedInstance, @Nullable Object capturedInstance)
   {
      return
         invokedInstance == originalToCapturedInstance.get(capturedInstance) ||
         capturedInstance == originalToCapturedInstance.get(invokedInstance);
   }

   public void discardCascadedMockWhenInjectable(@NotNull Object oldMock)
   {
      for (int i = 0, n = injectableMocks.size(); i < n; i++) {
         if (injectableMocks.get(i) == oldMock) {
            injectableMocks.remove(i);
            return;
         }
      }
   }

   private boolean containsNonStrictMock(@NotNull Object mockOrClassDesc)
   {
      return containsReference(nonStrictMocks, mockOrClassDesc);
   }

   public void addStrictMock(@Nullable Object mock, @Nullable String mockClassDesc)
   {
      addStrictMock(mock);

      if (mockClassDesc != null) {
         String uniqueMockClassDesc = mockClassDesc.intern();

         if (!containsStrictMock(uniqueMockClassDesc) && !containsNonStrictMock(uniqueMockClassDesc)) {
            strictMocks.add(uniqueMockClassDesc);
         }
      }
   }

   private void addStrictMock(@Nullable Object mock)
   {
      if (mock != null && !containsStrictMock(mock)) {
         strictMocks.add(mock);
      }
   }

   private boolean containsStrictMock(@NotNull Object mockOrClassDesc)
   {
      return containsReference(strictMocks, mockOrClassDesc);
   }

   public void registerAsNotStrictlyMocked(@NotNull Class<?> mockedClass)
   {
      String toBeRegistered = Type.getInternalName(mockedClass).intern();
      registerAsNotStrictMock(toBeRegistered, mockedClass);
   }

   public void registerAsNotStrictlyMocked(@NotNull Object mockedObject)
   {
      registerAsNotStrictMock(mockedObject, mockedObject);
   }

   private void registerAsNotStrictMock(@NotNull Object toBeRegistered, @NotNull Object mockedObjectOrClass)
   {
      if (containsStrictMock(toBeRegistered)) {
         throw new IllegalArgumentException("Already mocked strictly: " + mockedObjectOrClass);
      }

      if (!containsNonStrictMock(toBeRegistered)) {
         nonStrictMocks.add(toBeRegistered);
      }
   }

   public static boolean isInstanceMethodWithStandardBehavior(@Nullable Object mock, @NotNull String nameAndDesc)
   {
      return
         mock != null && nameAndDesc.charAt(0) != '<' &&
         ("equals(Ljava/lang/Object;)Z hashCode()I toString()Ljava/lang/String;".contains(nameAndDesc) ||
          mock instanceof Comparable<?> && nameAndDesc.startsWith("compareTo(L") && nameAndDesc.endsWith(";)I"));
   }

   public void registerMock(@NotNull MockedType typeMetadata, @NotNull Object mock)
   {
      if (typeMetadata.injectable) {
         addInjectableMock(mock);
      }
      else if (!containsReference(regularMocks, mock)) {
         regularMocks.add(mock);
      }
   }

   public boolean isStrictInvocation(
      @Nullable Object mock, @NotNull String mockClassDesc, @NotNull String mockNameAndDesc)
   {
      if (isInstanceMethodWithStandardBehavior(mock, mockNameAndDesc)) {
         return false;
      }

      for (Object strictMock : strictMocks) {
         if (strictMock == mock) {
            return true;
         }
         else if (strictMock == mockClassDesc) {
            addStrictMock(mock);
            return true;
         }
      }

      return false;
   }

   @NotNull public CascadingTypes getCascadingTypes() { return cascadingTypes; }

   void finishExecution(boolean clearSharedMocks)
   {
      recordAndReplayForLastTestMethod = currentRecordAndReplay;
      currentRecordAndReplay = null;

      if (parameterTypeRedefinitions != null) {
         parameterTypeRedefinitions.cleanUp();
         parameterTypeRedefinitions = null;
      }

      if (clearSharedMocks) {
         nonStrictMocks.clear();
      }

      strictMocks.clear();
      cascadingTypes.clearNonSharedCascadingTypes();
   }
}
