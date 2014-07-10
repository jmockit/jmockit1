/*
 * Copyright (c) 2006-2014 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.mockups;

import java.lang.reflect.*;

import mockit.internal.*;
import mockit.internal.state.*;

import org.jetbrains.annotations.*;

/**
 * An invocation to a {@code @Mock} method.
 */
public final class MockInvocation extends BaseInvocation
{
   @NotNull private final MockState mockState;
   @NotNull private final String mockedClassDesc;
   @NotNull private final String mockedMethodName;
   @NotNull private final String mockedMethodDesc;
   boolean proceeding;

   @NotNull
   public static MockInvocation create(
      @Nullable Object invokedInstance, @NotNull Object[] invokedArguments,
      @NotNull String mockClassDesc, int mockStateIndex,
      @NotNull String mockedClassDesc, @NotNull String mockedMethodName, @NotNull String mockedMethodDesc)
   {
      Object mockUp = TestRun.getMock(mockClassDesc, invokedInstance);
      MockState mockState = TestRun.getMockStates().getMockState(mockUp, mockStateIndex);
      return new MockInvocation(
         invokedInstance, invokedArguments, mockState, mockedClassDesc, mockedMethodName, mockedMethodDesc);
   }

   public MockInvocation(
      @Nullable Object invokedInstance, @NotNull Object[] invokedArguments, @NotNull MockState mockState,
      @NotNull String mockedClassDesc, @NotNull String mockedMethodName, @NotNull String mockedMethodDesc)
   {
      super(
         invokedInstance, invokedArguments,
         mockState.getTimesInvoked(), mockState.getMinInvocations(), mockState.getMaxInvocations());
      this.mockState = mockState;
      this.mockedClassDesc = mockedClassDesc;
      this.mockedMethodName = mockedMethodName;
      this.mockedMethodDesc = mockedMethodDesc;
   }

   public MockInvocation(
      @Nullable Object invokedInstance, @NotNull Object[] invokedArguments, @NotNull MockState mockState)
   {
      super(
         invokedInstance, invokedArguments,
         mockState.getTimesInvoked(), mockState.getMinInvocations(), mockState.getMaxInvocations());
      this.mockState = mockState;
      mockedClassDesc = "";
      mockedMethodName = "";
      mockedMethodDesc = "";
   }

   @NotNull @Override
   protected Member findRealMember()
   {
      return mockState.getRealMethodOrConstructor(mockedClassDesc, mockedMethodName, mockedMethodDesc);
   }

   public boolean shouldProceedIntoConstructor()
   {
      if (proceeding && getInvokedMember() instanceof Constructor) {
         mockState.clearProceedIndicator();
         return true;
      }

      return false;
   }

   @Override
   public boolean prepareToProceed()
   {
      if (mockState.prepareToProceed(this)) {
         proceeding = true;
         return true;
      }

      return false;
   }

   @Override
   public void cleanUpAfterProceed()
   {
      mockState.clearProceedIndicator();
   }
}
