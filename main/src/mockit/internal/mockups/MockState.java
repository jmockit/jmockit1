/*
 * Copyright (c) 2006-2015 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.mockups;

import java.lang.reflect.*;
import javax.annotation.*;

import mockit.internal.*;
import mockit.internal.mockups.MockMethods.*;
import mockit.internal.util.*;

final class MockState
{
   @Nonnull final MockMethod mockMethod;
   @Nullable private Method actualMockMethod;
   @Nullable private Member realMethodOrConstructor;

   // Expectations on the number of invocations of the mock as specified by the @Mock annotation,
   // initialized with the default values as specified in the annotation's definition.
   int expectedInvocations;
   int minExpectedInvocations;
   int maxExpectedInvocations;

   // Current mock invocation state:
   private int invocationCount;
   @Nullable private ThreadLocal<MockInvocation> proceedingInvocation;

   // Helper field just for synchronization:
   @Nonnull private final Object invocationCountLock;

   MockState(@Nonnull MockMethod mockMethod)
   {
      this.mockMethod = mockMethod;
      expectedInvocations = -1;
      maxExpectedInvocations = -1;
      invocationCountLock = new Object();
   }

   MockState(@Nonnull MockState mockState)
   {
      mockMethod = mockState.mockMethod;
      actualMockMethod = mockState.actualMockMethod;
      realMethodOrConstructor = mockState.realMethodOrConstructor;
      expectedInvocations = mockState.expectedInvocations;
      minExpectedInvocations = mockState.minExpectedInvocations;
      maxExpectedInvocations = mockState.maxExpectedInvocations;
      invocationCountLock = new Object();

      if (mockState.proceedingInvocation != null) {
         makeReentrant();
      }
   }

   @Nonnull Class<?> getRealClass() { return mockMethod.getRealClass(); }

   void makeReentrant() { proceedingInvocation = new ThreadLocal<MockInvocation>(); }

   boolean isWithExpectations()
   {
      return expectedInvocations >= 0 || minExpectedInvocations > 0 || maxExpectedInvocations >= 0;
   }

   boolean update()
   {
      if (proceedingInvocation != null) {
         MockInvocation invocation = proceedingInvocation.get();

         if (invocation != null && invocation.proceeding) {
            invocation.proceeding = false;
            return false;
         }
      }

      int timesInvoked;

      synchronized (invocationCountLock) {
         timesInvoked = ++invocationCount;
      }

      verifyUnexpectedInvocation(timesInvoked);
      return true;
   }

   private void verifyUnexpectedInvocation(int timesInvoked)
   {
      if (expectedInvocations >= 0 && timesInvoked > expectedInvocations) {
         String message = mockMethod.errorMessage("exactly", expectedInvocations, timesInvoked);
         throw new UnexpectedInvocation(message);
      }

      if (maxExpectedInvocations >= 0 && timesInvoked > maxExpectedInvocations) {
         String message = mockMethod.errorMessage("at most", maxExpectedInvocations, timesInvoked);
         throw new UnexpectedInvocation(message);
      }
   }

   void verifyMissingInvocations()
   {
      int timesInvoked = getTimesInvoked();

      if (timesInvoked < expectedInvocations) {
         String message = mockMethod.errorMessage("exactly", expectedInvocations, timesInvoked);
         throw new MissingInvocation(message);
      }

      if (timesInvoked < minExpectedInvocations) {
         String message = mockMethod.errorMessage("at least", minExpectedInvocations, timesInvoked);
         throw new MissingInvocation(message);
      }
   }

   int getMinInvocations() { return expectedInvocations >= 0 ? expectedInvocations : minExpectedInvocations; }
   int getMaxInvocations() { return expectedInvocations >= 0 ? expectedInvocations : maxExpectedInvocations; }

   int getTimesInvoked()
   {
      synchronized (invocationCountLock) {
         return invocationCount;
      }
   }

   void reset()
   {
      synchronized (invocationCountLock) {
         invocationCount = 0;
      }
   }

   @Nonnull
   Member getRealMethodOrConstructor(
      @Nonnull String mockedClassDesc, @Nonnull String mockedMethodName, @Nonnull String mockedMethodDesc)
   {
      if (realMethodOrConstructor == null) {
         String memberName = "$init".equals(mockedMethodName) ? "<init>" : mockedMethodName;

         RealMethodOrConstructor realMember;
         try { realMember = new RealMethodOrConstructor(mockedClassDesc, memberName, mockedMethodDesc); }
         catch (NoSuchMethodException e) { throw new RuntimeException(e); }

         Member member = realMember.getMember();

         if (mockMethod.isAdvice) {
            return member;
         }

         realMethodOrConstructor = member;
      }

      return realMethodOrConstructor;
   }

   public boolean shouldProceedIntoRealImplementation(@Nullable Object mock, @Nonnull String classDesc)
   {
      if (proceedingInvocation != null) {
         MockInvocation pendingInvocation = proceedingInvocation.get();

         if (pendingInvocation != null && pendingInvocation.isMethodInSuperclass(mock, classDesc)) {
            return true;
         }
      }

      return false;
   }

   void prepareToProceed(@Nonnull MockInvocation invocation)
   {
      if (proceedingInvocation == null) {
         throw new UnsupportedOperationException("Cannot proceed into abstract/interface method");
      }

      if (mockMethod.isForNativeMethod()) {
         throw new UnsupportedOperationException("Cannot proceed into real implementation of native method");
      }

      MockInvocation previousInvocation = proceedingInvocation.get();

      if (previousInvocation != null) {
         invocation.setPrevious(previousInvocation);
      }

      proceedingInvocation.set(invocation);
   }

   void prepareToProceedFromNonRecursiveMock(@Nonnull MockInvocation invocation)
   {
      assert proceedingInvocation != null;
      proceedingInvocation.set(invocation);
   }

   void clearProceedIndicator()
   {
      assert proceedingInvocation != null;
      MockInvocation currentInvocation = proceedingInvocation.get();
      MockInvocation previousInvocation = (MockInvocation) currentInvocation.getPrevious();
      proceedingInvocation.set(previousInvocation);
   }

   @Nonnull
   Method getMockMethod()
   {
      assert actualMockMethod != null;
      return actualMockMethod;
   }

   @Nonnull
   Method getMockMethod(@Nonnull Class<?> mockUpClass, @Nonnull Class<?>[] parameterTypes)
   {
      if (actualMockMethod == null) {
         actualMockMethod = MethodReflection.findCompatibleMethod(mockUpClass, mockMethod.name, parameterTypes);
      }

      return actualMockMethod;
   }

   @Override
   public boolean equals(@Nonnull Object other) { return mockMethod.equals(((MockState) other).mockMethod); }

   @Override
   public int hashCode() { return mockMethod.hashCode(); }
}
