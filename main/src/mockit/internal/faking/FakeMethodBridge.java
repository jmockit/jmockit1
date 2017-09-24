/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.faking;

import java.lang.reflect.*;
import javax.annotation.*;

import mockit.*;
import mockit.internal.*;
import mockit.internal.reflection.*;
import mockit.internal.state.*;
import mockit.internal.util.*;

public final class FakeMethodBridge extends MockingBridge
{
   @Nonnull public static final MockingBridge MB = new FakeMethodBridge();

   private FakeMethodBridge() { super("$FMB"); }

   @Nullable @Override
   public Object invoke(@Nullable Object mockedInstance, Method method, @Nonnull Object[] args) throws Throwable
   {
      String fakeClassDesc = (String) args[0];
      String fakedClassDesc = (String) args[1];
      String fakeDesc = (String) args[4];

      Object fake = TestRun.getFake(fakeClassDesc, mockedInstance);

      if (notToBeMocked(mockedInstance, fakedClassDesc)) {
         return DefaultValues.computeForReturnType(fakeDesc);
      }

      String fakeName = (String) args[3];
      int mockStateIndex = (Integer) args[5];
      Object[] fakeArgs = extractMockArguments(6, args);

      return callFake(mockedInstance, fake, fakedClassDesc, fakeName, fakeDesc, mockStateIndex, fakeArgs);
   }

   @Nullable
   private static Object callFake(
      @Nullable Object fakedInstance, @Nonnull Object fake, @Nonnull String fakedClassDesc,
      @Nonnull String fakeOrFakedName, @Nonnull String fakeOrFakedDesc, int mockStateIndex, @Nonnull Object[] fakeArgs)
      throws Throwable
   {
      Class<?> fakeClass = fake.getClass();

      if (mockStateIndex < 0) {
         return executeSimpleFakeMethod(fakeClass, fake, fakeOrFakedName, fakeOrFakedDesc, fakeArgs);
      }

      FakeState fakeState = TestRun.getFakeStates().getFakeState(fake, mockStateIndex);

      if (!fakeState.fakeMethod.hasInvocationParameter) {
         return executeFakeMethodWithoutInvocationArgument(fakeState, fakeClass, fake, fakeOrFakedDesc, fakeArgs);
      }

      if (fakeState.shouldProceedIntoRealImplementation(fakedInstance, fakedClassDesc)) {
         return Void.class;
      }

      return executeFakeMethodWithInvocationArgument(
         fakeState, fakeClass, fake, fakedInstance, fakedClassDesc, fakeOrFakedName, fakeOrFakedDesc, fakeArgs);
   }

   @Nullable
   private static Object executeSimpleFakeMethod(
      @Nonnull Class<?> fakeClass, @Nullable Object fake,
      @Nonnull String fakeOrFakedName, @Nonnull String fakeOrFakedDesc, @Nonnull Object[] fakeArgs)
      throws Throwable
   {
      Class<?>[] paramClasses = TypeDescriptor.getParameterTypes(fakeOrFakedDesc);
      Object result =
         MethodReflection.invokeWithCheckedThrows(fakeClass, fake, fakeOrFakedName, paramClasses, fakeArgs);
      return result;
   }

   @Nullable
   private static Object executeFakeMethodWithoutInvocationArgument(
      @Nonnull FakeState fakeState, @Nonnull Class<?> fakeClass, @Nullable Object fake,
      @Nonnull String fakeOrFakedDesc, @Nonnull Object[] fakeArgs)
      throws Throwable
   {
      Class<?>[] paramClasses = TypeDescriptor.getParameterTypes(fakeOrFakedDesc);
      Method fakeMethod = fakeState.getFakeMethod(fakeClass, paramClasses);
      Object result = MethodReflection.invokeWithCheckedThrows(fake, fakeMethod, fakeArgs);
      return result;
   }

   @Nullable
   private static Object executeFakeMethodWithInvocationArgument(
      @Nonnull FakeState fakeState, @Nonnull Class<?> fakeClass, @Nullable Object fake,
      @Nullable Object fakedInstance, @Nonnull String fakedClassDesc, @Nonnull String fakedName,
      @Nonnull String fakedDesc, @Nonnull Object[] fakeArgs)
      throws Throwable
   {
      Class<?>[] paramClasses;
      Method fakeMethod;
      MockInvocation invocation;
      Object[] executionArgs;

      if (fakeState.fakeMethod.isAdvice) {
         paramClasses = new Class[] {Invocation.class};
         fakeMethod = fakeState.getFakeMethod(fakeClass, paramClasses);
         invocation = new MockInvocation(fakedInstance, fakeArgs, fakeState, fakedClassDesc, fakedName, fakedDesc);
         executionArgs = new Object[] {invocation};
      }
      else {
         paramClasses = TypeDescriptor.getParameterTypes(fakedDesc);
         fakeMethod = fakeState.getFakeMethod(fakeClass, paramClasses);

         //noinspection AssignmentToMethodParameter
         fakedDesc = fakeState.fakeMethod.fakeDescWithoutInvocationParameter;
         invocation = new MockInvocation(fakedInstance, fakeArgs, fakeState, fakedClassDesc, fakedName, fakedDesc);
         executionArgs = ParameterReflection.argumentsWithExtraFirstValue(fakeArgs, invocation);
      }

      Object result = MethodReflection.invokeWithCheckedThrows(fake, fakeMethod, executionArgs);
      return invocation.shouldProceedIntoConstructor() ? Void.class : result;
   }
}
