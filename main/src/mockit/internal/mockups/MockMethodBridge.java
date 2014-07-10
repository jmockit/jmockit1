/*
 * Copyright (c) 2006-2014 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.mockups;

import java.lang.reflect.*;
import static java.lang.reflect.Modifier.*;

import mockit.*;
import mockit.internal.*;
import mockit.internal.state.*;
import mockit.internal.util.*;

import org.jetbrains.annotations.*;

public final class MockMethodBridge extends MockingBridge
{
   @NotNull public static final MockingBridge MB = new MockMethodBridge();

   private MockMethodBridge() { super("$MMB"); }

   @Nullable @Override
   public Object invoke(@Nullable Object mockedInstance, Method method, @NotNull Object[] args) throws Throwable
   {
      String mockClassDesc = (String) args[0];
      String mockedClassDesc = (String) args[1];
      String mockDesc = (String) args[4];

      Object mockUp = TestRun.getMock(mockClassDesc, mockedInstance);

      if (mockUp == null || notToBeMocked(mockedInstance, mockedClassDesc)) {
         Integer mockedAccess = (Integer) args[2];
         return isNative(mockedAccess) ? DefaultValues.computeForReturnType(mockDesc) : Void.class;
      }

      String mockName = (String) args[3];
      int mockStateIndex = (Integer) args[5];
      Object[] mockArgs = extractMockArguments(6, args);

      return callMock(mockedInstance, mockUp, mockedClassDesc, mockName, mockDesc, mockStateIndex, mockArgs);
   }

   @Nullable
   private static Object callMock(
      @Nullable Object mockedInstance, @NotNull Object mockUp, @NotNull String mockedClassDesc,
      @NotNull String mockOrMockedName, @NotNull String mockOrMockedDesc, int mockStateIndex,
      @NotNull Object[] mockArgs)
      throws Throwable
   {
      Class<?> mockUpClass = mockUp.getClass();

      if (mockStateIndex < 0) {
         // TODO: optimize by storing Methods in a Map with key = name+desc
         return executeSimpleMockMethod(mockUpClass, mockUp, mockOrMockedName, mockOrMockedDesc, mockArgs);
      }

      MockState mockState = TestRun.getMockStates().getMockState(mockUp, mockStateIndex);

      if (!mockState.mockMethod.hasInvocationParameter) {
         return executeMockMethodWithoutInvocationArgument(mockState, mockUpClass, mockUp, mockOrMockedDesc, mockArgs);
      }

      if (mockState.shouldProceedIntoRealImplementation(mockedInstance, mockedClassDesc)) {
         return Void.class;
      }

      return executeMockMethodWithInvocationArgument(
         mockState, mockUpClass, mockUp, mockedInstance, mockedClassDesc, mockOrMockedName, mockOrMockedDesc, mockArgs);
   }

   @Nullable
   private static Object executeSimpleMockMethod(
      @NotNull Class<?> mockUpClass, @Nullable Object mockUp,
      @NotNull String mockOrMockedName, @NotNull String mockOrMockedDesc, @NotNull Object[] mockArgs)
      throws Throwable
   {
      Class<?>[] paramClasses = TypeDescriptor.getParameterTypes(mockOrMockedDesc);
      Object result =
         MethodReflection.invokeWithCheckedThrows(mockUpClass, mockUp, mockOrMockedName, paramClasses, mockArgs);
      return result;
   }

   @Nullable
   private static Object executeMockMethodWithoutInvocationArgument(
      @NotNull MockState mockState, @NotNull Class<?> mockUpClass, @Nullable Object mockUp,
      @NotNull String mockOrMockedDesc, @NotNull Object[] mockArgs)
      throws Throwable
   {
      Class<?>[] paramClasses = TypeDescriptor.getParameterTypes(mockOrMockedDesc);
      Method mockMethod = mockState.getMockMethod(mockUpClass, paramClasses);
      Object result = MethodReflection.invokeWithCheckedThrows(mockUp, mockMethod, mockArgs);
      return result;
   }

   @Nullable
   private static Object executeMockMethodWithInvocationArgument(
      @NotNull MockState mockState, @NotNull Class<?> mockUpClass, @Nullable Object mockUp,
      @Nullable Object mockedInstance, @NotNull String mockedClassDesc, @NotNull String mockedName,
      @NotNull String mockedDesc, @NotNull Object[] mockArgs)
      throws Throwable
   {
      Class<?>[] paramClasses;
      Method mockMethod;
      MockInvocation invocation;
      Object[] executionArgs;

      if (mockState.mockMethod.isAdvice) {
         paramClasses = new Class[] {Invocation.class};
         mockMethod = mockState.getMockMethod(mockUpClass, paramClasses);
         invocation = new MockInvocation(mockedInstance, mockArgs, mockState, mockedClassDesc, mockedName, mockedDesc);
         executionArgs = new Object[] {invocation};
      }
      else {
         paramClasses = TypeDescriptor.getParameterTypes(mockedDesc);
         mockMethod = mockState.getMockMethod(mockUpClass, paramClasses);

         //noinspection AssignmentToMethodParameter
         mockedDesc = mockState.mockMethod.mockDescWithoutInvocationParameter;
         invocation = new MockInvocation(mockedInstance, mockArgs, mockState, mockedClassDesc, mockedName, mockedDesc);
         executionArgs = ParameterReflection.argumentsWithExtraFirstValue(mockArgs, invocation);
      }

      Object result = MethodReflection.invokeWithCheckedThrows(mockUp, mockMethod, executionArgs);
      return invocation.shouldProceedIntoConstructor() ? Void.class : result;
   }
}
