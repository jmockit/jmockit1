/*
 * Copyright (c) 2006-2014 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.expectations;

import mockit.*;
import mockit.internal.expectations.invocation.*;
import mockit.internal.expectations.mocking.*;
import mockit.internal.state.*;
import mockit.internal.util.*;
import static mockit.internal.util.Utilities.*;

import org.jetbrains.annotations.*;
import sun.reflect.*;

public final class RecordPhase extends TestOnlyPhase
{
   private final boolean nonStrict;

   RecordPhase(@NotNull RecordAndReplayExecution recordAndReplay, boolean nonStrict)
   {
      super(recordAndReplay);
      this.nonStrict = nonStrict;
   }

   public void addResult(@Nullable Object result)
   {
      getCurrentExpectation().addResult(result);
   }

   public void addSequenceOfReturnValues(@Nullable Object firstValue, @Nullable Object[] remainingValues)
   {
      getCurrentExpectation().addSequenceOfReturnValues(firstValue, remainingValues);
   }

   @Nullable @Override
   Object handleInvocation(
      @Nullable Object mock, int mockAccess, @NotNull String mockClassDesc, @NotNull String mockNameAndDesc,
      @Nullable String genericSignature, boolean withRealImpl, @NotNull Object[] args)
      throws Throwable
   {
      //noinspection AssignmentToMethodParameter
      mock = configureMatchingOnMockInstanceIfSpecified(mock);

      ExpectedInvocation invocation = new ExpectedInvocation(
         mock, mockAccess, mockClassDesc, mockNameAndDesc, matchInstance, genericSignature, args);
      Class<?> callerClass = getCallerClass();

      if (!Expectations.class.isAssignableFrom(callerClass)) {
         String kind = invocation.isConstructor() ? "constructor" : "method";
         throw new IllegalStateException(
            "Attempted to record invocation to mocked " + kind + " from outside expectation block" + invocation);
      }

      ExecutingTest executingTest = TestRun.getExecutingTest();
      boolean nonStrictInvocation =
         nonStrict || executingTest.isNonStrictInvocation(mock, mockClassDesc, mockNameAndDesc);

      if (!nonStrictInvocation) {
         executingTest.addStrictMock(mock, matchInstance ? null : mockClassDesc);
      }
      else if (!matchInstance && invocation.isConstructor()) {
         invocation.replacementInstance = mock;
         getReplacementMap().put(mock, mock);
      }

      currentExpectation = new Expectation(this, invocation, nonStrictInvocation);

      if (argMatchers != null) {
         invocation.arguments.setMatchers(argMatchers);
         argMatchers = null;
      }

      recordAndReplay.executionState.addExpectation(currentExpectation, nonStrictInvocation);

      return invocation.getDefaultValueForReturnType(this);
   }

   @Nullable
   private Object configureMatchingOnMockInstanceIfSpecified(@Nullable Object mock)
   {
      matchInstance = false;

      if (mock == null) {
         return null;
      }

      if (nextInstanceToMatch == null) {
         if (mock == getReplacementMap().get(mock)) {
            matchInstance = true;
         }

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

   private static final int CALLER_CLASS_INDEX = HOTSPOT_VM ? 4 : 5;

   @NotNull @SuppressWarnings("deprecation")
   private static Class<?> getCallerClass()
   {
      Class<?> firstCaller = Reflection.getCallerClass(CALLER_CLASS_INDEX);
      int steIndex = CALLER_CLASS_INDEX + (firstCaller == MockedBridge.class ? 2 : 1);
      Class<?> secondCaller = Reflection.getCallerClass(steIndex);

      if (secondCaller == MethodReflection.class) { // called through Reflection
         steIndex += 3;

         while (true) {
            Class<?> nextCaller = Reflection.getCallerClass(steIndex);
            steIndex++;

            if (nextCaller == Deencapsulation.class) {
               continue;
            }

            String className = nextCaller.getName();

            if (!className.contains(".reflect.") && !className.startsWith("mockit.internal.")) {
               return nextCaller;
            }
         }
      }

      if (secondCaller != firstCaller) {
         return secondCaller;
      }

      Class<?> thirdCaller = Reflection.getCallerClass(steIndex + 1);
      return thirdCaller;
   }

   @Override
   public void handleInvocationCountConstraint(int minInvocations, int maxInvocations)
   {
      int lowerLimit = minInvocations;
      int upperLimit = maxInvocations;

      if (numberOfIterations > 1 && nonStrict) {
         lowerLimit *= numberOfIterations;
         upperLimit *= numberOfIterations;
      }

      getCurrentExpectation().constraints.setLimits(lowerLimit, upperLimit);
   }

   @Override
   public void setCustomErrorMessage(@Nullable CharSequence customMessage)
   {
      getCurrentExpectation().setCustomErrorMessage(customMessage);
   }
}
