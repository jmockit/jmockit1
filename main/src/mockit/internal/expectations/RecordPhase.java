/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.expectations;

import javax.annotation.*;

import mockit.*;
import mockit.internal.expectations.invocation.*;
import mockit.internal.state.*;
import mockit.internal.util.*;
import static mockit.internal.state.ExecutingTest.*;
import static mockit.internal.util.ClassLoad.*;

public final class RecordPhase extends TestOnlyPhase
{
   final boolean strict;

   RecordPhase(@Nonnull RecordAndReplayExecution recordAndReplay, boolean strict)
   {
      super(recordAndReplay);
      this.strict = strict;
   }

   public void addResult(@Nullable Object result)
   {
      getCurrentExpectation().addResult(result);
   }

   public void addSequenceOfReturnValues(@Nonnull Object[] values)
   {
      getCurrentExpectation().addSequenceOfReturnValues(values);
   }

   public void addSequenceOfReturnValues(@Nullable Object firstValue, @Nullable Object[] remainingValues)
   {
      getCurrentExpectation().addSequenceOfReturnValues(firstValue, remainingValues);
   }

   @Nullable @Override
   Object handleInvocation(
      @Nullable Object mock, int mockAccess, @Nonnull String mockClassDesc, @Nonnull String mockNameAndDesc,
      @Nullable String genericSignature, boolean withRealImpl, @Nonnull Object[] args)
      throws Throwable
   {
      //noinspection AssignmentToMethodParameter
      mock = configureMatchingOnMockInstanceIfSpecified(mock);

      ExpectedInvocation invocation = new ExpectedInvocation(
         mock, mockAccess, mockClassDesc, mockNameAndDesc, matchInstance, genericSignature, args);
      Class<?> callerClass = loadClass(invocation.getCallerClassName());

      if (Expectations.class.isAssignableFrom(callerClass)) {
         boolean nonStrictInvocation = false;

         if (strict) {
            TestRun.getExecutingTest().addStrictMock(mock, matchInstance ? null : mockClassDesc);
         }
         else if (!matchInstance && invocation.isConstructor()) {
            invocation.replacementInstance = mock;
            getReplacementMap().put(mock, mock);
         }
         else {
            nonStrictInvocation = isInstanceMethodWithStandardBehavior(mock, mockNameAndDesc);
         }

         currentExpectation = new Expectation(this, invocation, strict, nonStrictInvocation);

         if (argMatchers != null) {
            invocation.arguments.setMatchers(argMatchers);
            argMatchers = null;
         }

         recordAndReplay.executionState.addExpectation(currentExpectation, strict);
      }

      return invocation.getDefaultValueForReturnType();
   }

   @Nullable
   private Object configureMatchingOnMockInstanceIfSpecified(@Nullable Object mock)
   {
      matchInstance = false;

      if (mock == null) {
         return null;
      }

      if (nextInstanceToMatch == null) {
         Object replacementInstance = getReplacementMap().get(mock);
         matchInstance = mock == replacementInstance || isEnumElement(mock);
         return mock;
      }

      Object specified = nextInstanceToMatch;

      if (mock != specified) {
         Class<?> mockedClass = GeneratedClasses.getMockedClass(mock);

         if (!mockedClass.isInstance(specified)) {
            return mock;
         }
      }

      nextInstanceToMatch = null;
      matchInstance = true;
      return specified;
   }

   @Override
   public void handleInvocationCountConstraint(int minInvocations, int maxInvocations)
   {
      int lowerLimit = minInvocations;
      int upperLimit = maxInvocations;

      if (numberOfIterations > 1 && !strict) {
         lowerLimit *= numberOfIterations;
         upperLimit *= numberOfIterations;
      }

      getCurrentExpectation().constraints.setLimits(lowerLimit, upperLimit);
   }
}
