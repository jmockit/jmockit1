/*
 * Copyright (c) 2006-2015 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.mockups;

import java.lang.reflect.*;

import mockit.internal.*;
import mockit.internal.state.*;
import static mockit.internal.util.Utilities.*;

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
      @Nullable Object invokedInstance, @Nullable Object[] invokedArguments,
      @NotNull String mockClassDesc, int mockStateIndex,
      @NotNull String mockedClassDesc, @NotNull String mockedMethodName, @NotNull String mockedMethodDesc)
   {
      Object mockUp = TestRun.getMock(mockClassDesc, invokedInstance);
      assert mockUp != null;
      MockState mockState = TestRun.getMockStates().getMockState(mockUp, mockStateIndex);
      Object[] args = invokedArguments == null ? NO_ARGS : invokedArguments;
      return new MockInvocation(invokedInstance, args, mockState, mockedClassDesc, mockedMethodName, mockedMethodDesc);
   }

   MockInvocation(
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
   public void prepareToProceed()
   {
      mockState.prepareToProceed(this);
      proceeding = true;
   }

   public void prepareToProceedFromNonRecursiveMock()
   {
      mockState.prepareToProceedFromNonRecursiveMock(this);
      proceeding = true;
   }

   @Override
   public void cleanUpAfterProceed()
   {
      mockState.clearProceedIndicator();
   }
}
