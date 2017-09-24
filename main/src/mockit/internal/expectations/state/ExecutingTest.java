/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.expectations.state;

import java.lang.reflect.*;
import java.util.*;
import javax.annotation.*;

import mockit.internal.*;
import mockit.internal.expectations.*;
import mockit.internal.expectations.mocking.*;
import static mockit.internal.util.Utilities.*;

public final class ExecutingTest
{
   @Nullable private RecordAndReplayExecution currentRecordAndReplay;
   @Nullable private RecordAndReplayExecution recordAndReplayForLastTestMethod;

   @Nonnull private final ThreadLocal<Boolean> shouldIgnoreMockingCallbacks;
   @Nonnull private final ThreadLocal<BaseInvocation> proceedingInvocation;
   private boolean proceeding;

   @Nullable private ParameterTypeRedefinitions parameterTypeRedefinitions;

   @Nonnull private final List<Object> regularMocks;
   @Nonnull private final List<Object> injectableMocks;
   @Nonnull private final Map<Object, Object> originalToCapturedInstance;
   @Nonnull private final CascadingTypes cascadingTypes;

   public ExecutingTest()
   {
      shouldIgnoreMockingCallbacks = new ThreadLocal<Boolean>() {
         @Override protected Boolean initialValue() { return false; }
      };
      proceedingInvocation = new ThreadLocal<BaseInvocation>();
      regularMocks = new ArrayList<Object>();
      injectableMocks = new ArrayList<Object>();
      originalToCapturedInstance = new IdentityHashMap<Object, Object>(4);
      cascadingTypes = new CascadingTypes();
   }

   @Nonnull
   public RecordAndReplayExecution getOrCreateRecordAndReplay()
   {
      if (currentRecordAndReplay == null) {
         setRecordAndReplay(new RecordAndReplayExecution());
      }

      return currentRecordAndReplay;
   }

   @Nullable
   public RecordAndReplayExecution getPreviousRecordAndReplay()
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

   public void markAsProceedingIntoRealImplementation(@Nonnull BaseInvocation invocation)
   {
      BaseInvocation previousInvocation = proceedingInvocation.get();

      if (previousInvocation != null) {
         invocation.setPrevious(previousInvocation);
      }

      proceedingInvocation.set(invocation);
      proceeding = true;
   }

   public boolean shouldProceedIntoRealImplementation(@Nullable Object mock, @Nonnull String classDesc)
   {
      BaseInvocation pendingInvocation = proceedingInvocation.get();

      //noinspection SimplifiableIfStatement
      if (pendingInvocation != null && pendingInvocation.isMethodInSuperclass(mock, classDesc)) {
         return true;
      }

      return isProceedingIntoRealImplementation();
   }

   public void clearProceedingState()
   {
      BaseInvocation pendingInvocation = proceedingInvocation.get();
      BaseInvocation previousInvocation = pendingInvocation.getPrevious();
      proceedingInvocation.set(previousInvocation);
   }

   @Nonnull
   public RecordAndReplayExecution getRecordAndReplayForVerifications()
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

   @Nullable public ParameterTypeRedefinitions getParameterRedefinitions() { return parameterTypeRedefinitions; }

   public void setParameterRedefinitions(@Nonnull ParameterTypeRedefinitions redefinitions)
   {
      parameterTypeRedefinitions = redefinitions;
   }

   public void clearInjectableAndNonStrictMocks()
   {
      regularMocks.clear();
      injectableMocks.clear();
      originalToCapturedInstance.clear();
   }

   void addInjectableMock(@Nonnull Object mock)
   {
      if (!isInjectableMock(mock)) {
         injectableMocks.add(mock);
      }
   }

   public boolean isInjectableMock(@Nonnull Object instance) { return containsReference(injectableMocks, instance); }

   public boolean isMockedInstance(@Nonnull Object instance)
   {
      return containsReference(regularMocks, instance) || isInjectableMock(instance);
   }

   public void addCapturedInstanceForInjectableMock(@Nullable Object originalInstance, @Nonnull Object capturedInstance)
   {
      injectableMocks.add(capturedInstance);
      addCapturedInstance(originalInstance, capturedInstance);
   }

   public void addCapturedInstance(@Nullable Object originalInstance, @Nonnull Object capturedInstance)
   {
      originalToCapturedInstance.put(capturedInstance, originalInstance);
   }

   public boolean isInvokedInstanceEquivalentToCapturedInstance(
      @Nonnull Object invokedInstance, @Nonnull Object capturedInstance)
   {
      return
         invokedInstance == originalToCapturedInstance.get(capturedInstance) ||
         capturedInstance == originalToCapturedInstance.get(invokedInstance);
   }

   public static boolean isInstanceMethodWithStandardBehavior(@Nullable Object mock, @Nonnull String nameAndDesc)
   {
      return
         mock != null && nameAndDesc.charAt(0) != '<' &&
         ("equals(Ljava/lang/Object;)Z hashCode()I toString()Ljava/lang/String;".contains(nameAndDesc) ||
          mock instanceof Comparable<?> && nameAndDesc.startsWith("compareTo(L") && nameAndDesc.endsWith(";)I"));
   }

   public void registerMock(@Nonnull MockedType mockedType, @Nonnull Object mock)
   {
      if (mockedType.injectable) {
         addInjectableMock(mock);
      }
      else if (!containsReference(regularMocks, mock)) {
         regularMocks.add(mock);
      }

      Type declaredType = mockedType.getDeclaredType();
      cascadingTypes.addInstance(declaredType, mock);
   }

   @Nonnull public CascadingTypes getCascadingTypes() { return cascadingTypes; }

   public void finishExecution()
   {
      recordAndReplayForLastTestMethod = currentRecordAndReplay;
      currentRecordAndReplay = null;

      if (parameterTypeRedefinitions != null) {
         parameterTypeRedefinitions.cleanUp();
         parameterTypeRedefinitions = null;
      }

      cascadingTypes.clearNonSharedCascadingTypes();
   }
}
