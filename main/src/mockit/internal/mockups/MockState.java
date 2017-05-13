/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.mockups;

import java.lang.reflect.*;
import javax.annotation.*;

import mockit.internal.mockups.MockMethods.*;
import mockit.internal.reflection.*;

final class MockState
{
   @Nonnull final MockMethod mockMethod;
   @Nullable private Method actualMockMethod;
   @Nullable private Member realMethodOrConstructor;
   @Nullable private Object realClass;

   // Current mock invocation state:
   private int invocationCount;
   @Nullable private ThreadLocal<MockInvocation> proceedingInvocation;

   // Helper field just for synchronization:
   @Nonnull private final Object invocationCountLock;

   MockState(@Nonnull MockMethod mockMethod)
   {
      this.mockMethod = mockMethod;
      invocationCountLock = new Object();

      if (mockMethod.canBeReentered()) {
         makeReentrant();
      }
   }

   MockState(@Nonnull MockState mockState)
   {
      mockMethod = mockState.mockMethod;
      actualMockMethod = mockState.actualMockMethod;
      realMethodOrConstructor = mockState.realMethodOrConstructor;
      invocationCountLock = new Object();

      if (mockState.proceedingInvocation != null) {
         makeReentrant();
      }
   }

   @Nonnull Class<?> getRealClass() { return mockMethod.getRealClass(); }

   private void makeReentrant() { proceedingInvocation = new ThreadLocal<MockInvocation>(); }

   boolean update()
   {
      if (proceedingInvocation != null) {
         MockInvocation invocation = proceedingInvocation.get();

         if (invocation != null && invocation.proceeding) {
            invocation.proceeding = false;
            return false;
         }
      }

      synchronized (invocationCountLock) {
         invocationCount++;
      }

      return true;
   }

   int getTimesInvoked()
   {
      synchronized (invocationCountLock) {
         return invocationCount;
      }
   }

   @Nonnull
   Member getRealMethodOrConstructor(
      @Nonnull String mockedClassDesc, @Nonnull String mockedMethodName, @Nonnull String mockedMethodDesc)
   {
      if (realMethodOrConstructor == null || !mockedClassDesc.equals(realClass)) {
         String memberName = "$init".equals(mockedMethodName) ? "<init>" : mockedMethodName;

         RealMethodOrConstructor realMember;
         try { realMember = new RealMethodOrConstructor(mockedClassDesc, memberName, mockedMethodDesc); }
         catch (NoSuchMethodException e) { throw new RuntimeException(e); }

         Member member = realMember.getMember();

         if (mockMethod.isAdvice) {
            return member;
         }

         realMethodOrConstructor = member;
         realClass = mockedClassDesc;
      }

      return realMethodOrConstructor;
   }

   @Nonnull
   Member getRealMethodOrConstructor(
      @Nonnull Class<?> mockedClass, @Nonnull String mockedMethodName, @Nonnull String mockedMethodDesc)
   {
      if (realMethodOrConstructor == null || !mockedClass.equals(realClass)) {
         String memberName = "$init".equals(mockedMethodName) ? "<init>" : mockedMethodName;

         RealMethodOrConstructor realMember;
         try { realMember = new RealMethodOrConstructor(mockedClass, memberName, mockedMethodDesc); }
         catch (NoSuchMethodException e) { throw new RuntimeException(e); }

         Member member = realMember.getMember();

         if (mockMethod.isAdvice) {
            return member;
         }

         realMethodOrConstructor = member;
         realClass = mockedClass;
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

   @Override @SuppressWarnings("EqualsWhichDoesntCheckParameterClass")
   public boolean equals(@Nonnull Object other) { return mockMethod.equals(((MockState) other).mockMethod); }

   @Override
   public int hashCode() { return mockMethod.hashCode(); }
}
