/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.expectations.invocation;

import java.util.*;
import javax.annotation.*;

import mockit.external.asm.*;
import mockit.internal.*;
import mockit.internal.expectations.*;
import mockit.internal.expectations.argumentMatching.*;
import mockit.internal.state.*;
import mockit.internal.util.*;
import mockit.internal.util.GenericTypeReflection.*;

public final class ExpectedInvocation
{
   @Nonnull private static final Object UNDEFINED_DEFAULT_RETURN = new Object();

   @Nullable public final Object instance;
   @Nullable public Object replacementInstance;
   public boolean matchInstance;
   @Nonnull public final InvocationArguments arguments;
   @Nullable private final ExpectationError invocationCause;
   @Nullable Object defaultReturnValue;

   public ExpectedInvocation(
      @Nullable Object mock, @Nonnull String mockedClassDesc, @Nonnull String mockNameAndDesc,
      @Nullable String genericSignature, @Nonnull Object[] args)
   {
      instance = mock;
      arguments = new InvocationArguments(0, mockedClassDesc, mockNameAndDesc, genericSignature, args);
      invocationCause = null;
      defaultReturnValue = determineDefaultReturnValueFromMethodSignature();
   }

   public ExpectedInvocation(
      @Nullable Object mock, int access, @Nonnull String mockedClassDesc, @Nonnull String mockNameAndDesc,
      boolean matchInstance, @Nullable String genericSignature, @Nonnull Object[] args)
   {
      instance = mock;
      this.matchInstance = matchInstance;
      arguments = new InvocationArguments(access, mockedClassDesc, mockNameAndDesc, genericSignature, args);
      invocationCause = new ExpectationError();
      defaultReturnValue = determineDefaultReturnValueFromMethodSignature();
   }

   @Nullable
   public AssertionError getInvocationCause() { return invocationCause; }

   @Nonnull
   private Object determineDefaultReturnValueFromMethodSignature()
   {
      if (instance != null) {
         Object rv = ObjectMethods.evaluateOverride(instance, getMethodNameAndDescription(), getArgumentValues());

         if (rv != null) {
            return rv;
         }
      }

      return UNDEFINED_DEFAULT_RETURN;
   }

   @Nonnull
   public String getCallerClassName()
   {
      //noinspection ConstantConditions
      StackTrace st = new StackTrace(invocationCause);

      int steIndex = 3;
      StackTraceElement ste = st.getElement(steIndex);

      if (ste.getFileName() != null && ste.getLineNumber() == -1 && ste.getMethodName().charAt(0) != '<') {
         StackTraceElement steNext = st.getElement(steIndex + 1);

         if (steNext.getMethodName().equals(ste.getMethodName())) { // bridge method
            ste = steNext;
            steIndex++;
         }
      }

      String firstCaller = ste.getClassName();

      steIndex += "mockit.internal.expectations.mocking.MockedBridge".equals(firstCaller) ? 2 : 1;
      String secondCaller = st.getElement(steIndex).getClassName();

      if (secondCaller.contains(".reflect.")) { // called through Reflection
         return getNextCallerAfterReflectionCalls(st, steIndex);
      }

      if (!secondCaller.equals(firstCaller)) {
         return secondCaller;
      }

      String thirdCaller = st.getElement(steIndex + 1).getClassName();
      return thirdCaller;
   }

   @Nonnull
   private static String getNextCallerAfterReflectionCalls(@Nonnull StackTrace st, int steIndex)
   {
      steIndex += 3;

      while (true) {
         String nextCaller = st.getElement(steIndex).getClassName();
         steIndex++;

         if ("mockit.Deencapsulation".equals(nextCaller)) {
            continue;
         }

         if (!nextCaller.contains(".reflect.") && !nextCaller.startsWith("mockit.internal.")) {
            return nextCaller;
         }
      }
   }

   // Simple getters //////////////////////////////////////////////////////////////////////////////////////////////////

   @Nonnull public String getClassDesc() { return arguments.classDesc; }
   @Nonnull public String getClassName() { return arguments.getClassName(); }
   @Nonnull public String getMethodNameAndDescription() { return arguments.methodNameAndDesc; }
   @Nonnull public Object[] getArgumentValues() { return arguments.getValues(); }
   public boolean isConstructor() { return arguments.isForConstructor(); }

   @Nullable
   public Object getRecordedInstance()
   {
      return replacementInstance != null ? replacementInstance : instance;
   }

   @Nonnull
   public String getSignatureWithResolvedReturnType()
   {
      String signature = arguments.genericSignature;

      if (signature != null) {
         char firstTypeChar = signature.charAt(signature.indexOf(')') + 1);

         if (firstTypeChar != 'T' && firstTypeChar != '[') {
            return signature;
         }
      }

      return arguments.methodNameAndDesc;
   }

   // Matching based on instance or mocked type ///////////////////////////////////////////////////////////////////////

   public boolean isMatch(@Nullable Object mock, @Nonnull String invokedClassDesc, @Nonnull String invokedMethod)
   {
      return invokedClassDesc.equals(getClassDesc()) && isMatchingMethod(mock, invokedMethod);
   }

   private boolean isMatchingMethod(@Nullable Object mock, @Nonnull String invokedMethod)
   {
      if (mock != null && instance != null) {
         String genericSignature = arguments.genericSignature;

         if (genericSignature != null) {
            Class<?> mockedClass = mock.getClass();

            if (mockedClass != instance.getClass()) {
               GenericTypeReflection typeReflection = new GenericTypeReflection(mockedClass, null);
               GenericSignature parsedSignature = typeReflection.parseSignature(genericSignature);
               return parsedSignature.satisfiesSignature(invokedMethod);
            }
         }
      }

      String nameAndDesc = getMethodNameAndDescription();
      int i = 0;

      // Will return false if the method names or parameters are different:
      while (true) {
         char c = nameAndDesc.charAt(i);

         if (c != invokedMethod.charAt(i)) {
            return false;
         }

         i++;

         if (c == ')') {
            break;
         }
      }

      int n = invokedMethod.length();

      if (n == nameAndDesc.length()) {
         int j = i;

         // Given return types of same length, will return true if they are identical:
         while (true) {
            char c = nameAndDesc.charAt(j);

            if (c != invokedMethod.charAt(j)) {
               break;
            }

            j++;

            if (j == n) {
               return true;
            }
         }
      }

      // At this point the methods are known to differ only in return type, so check if the return
      // type of the recorded one is assignable to the return type of the one invoked:
      Type rt1 = Type.getType(nameAndDesc.substring(i));
      Type rt2 = Type.getType(invokedMethod.substring(i));

      return TypeDescriptor.getClassForType(rt2).isAssignableFrom(TypeDescriptor.getClassForType(rt1));
   }

   public boolean isMatch(
      @Nullable Object replayInstance, @Nonnull String invokedClassDesc, @Nonnull String invokedMethod,
      @Nullable Map<Object, Object> replacementMap)
   {
      return
         isMatch(replayInstance, invokedClassDesc, invokedMethod) &&
         (arguments.isForConstructor() || !matchInstance || isEquivalentInstance(replayInstance, replacementMap));
   }

   private boolean isEquivalentInstance(@Nullable Object mockedInstance, @Nullable Map<Object, Object> replacementMap)
   {
      return
         mockedInstance == instance ||
         mockedInstance != null && instance != null && (
            replacementMap != null && replacementMap.get(mockedInstance) == instance ||
            TestRun.getExecutingTest().isInvokedInstanceEquivalentToCapturedInstance(instance, mockedInstance)
         );
   }

   // Creation of Error instances for invocation mismatch reporting ///////////////////////////////////////////////////

   public ExpectedInvocation(
      @Nullable Object mockedInstance, @Nonnull String classDesc, @Nonnull String methodNameAndDesc,
      @Nonnull Object[] args)
   {
      instance = mockedInstance;
      matchInstance = false;
      arguments = new InvocationArguments(0, classDesc, methodNameAndDesc, null, args);
      invocationCause = null;
   }

   @Nonnull
   public UnexpectedInvocation errorForUnexpectedInvocation()
   {
      return newUnexpectedInvocationWithCause("Unexpected invocation", "Unexpected invocation of" + this);
   }

   @Nonnull
   private UnexpectedInvocation newUnexpectedInvocationWithCause(
      @Nonnull String titleForCause, @Nonnull String initialMessage)
   {
      UnexpectedInvocation error = new UnexpectedInvocation(initialMessage);
      setErrorAsInvocationCause(titleForCause, error);
      return error;
   }

   private void setErrorAsInvocationCause(@Nonnull String titleForCause, @Nonnull Throwable error)
   {
      if (invocationCause != null) {
         invocationCause.defineCause(titleForCause, error);
      }
   }

   @Nonnull
   private MissingInvocation newMissingInvocationWithCause(
      @Nonnull String titleForCause, @Nonnull String initialMessage)
   {
      MissingInvocation error = new MissingInvocation(initialMessage);
      setErrorAsInvocationCause(titleForCause, error);
      return error;
   }

   @Nonnull
   public MissingInvocation errorForMissingInvocation()
   {
      return newMissingInvocationWithCause("Missing invocation", "Missing invocation of" + this);
   }

   @Nonnull
   public MissingInvocation errorForMissingInvocations(int missingInvocations)
   {
      String message = "Missing " + missingInvocations + invocationsTo(missingInvocations) + this;
      return newMissingInvocationWithCause("Missing invocations", message);
   }

   @Nonnull
   private static String invocationsTo(int invocations)
   {
      return invocations == 1 ? " invocation to" : " invocations to";
   }

   @Nonnull
   public UnexpectedInvocation errorForUnexpectedInvocation(
      @Nullable Object mock, @Nonnull String invokedClassDesc, @Nonnull String invokedMethod,
      @Nonnull Object[] replayArgs)
   {
      StringBuilder message = new StringBuilder(200);
      message.append("Unexpected invocation of:\n");
      message.append(new MethodFormatter(invokedClassDesc, invokedMethod));

      if (replayArgs.length > 0) {
         ArgumentMismatch argumentMismatch = new ArgumentMismatch();
         argumentMismatch.appendFormatted(replayArgs);
         message.append("\n   with arguments: ").append(argumentMismatch);
      }

      if (mock != null) {
         message.append("\n   on instance: ").append(ObjectMethods.objectIdentity(mock));
      }

      message.append("\nwhen was expecting an invocation of").append(this);

      return newUnexpectedInvocationWithCause("Unexpected invocation", message.toString());
   }

   @Nonnull
   public UnexpectedInvocation errorForUnexpectedInvocation(@Nonnull Object[] replayArgs)
   {
      String message = "unexpected invocation to" + toString(replayArgs);
      return newUnexpectedInvocationWithCause("Unexpected invocation", message);
   }

   @Nonnull
   public UnexpectedInvocation errorForUnexpectedInvocations(@Nonnull Object[] replayArgs, int numUnexpected)
   {
      String message = numUnexpected + " unexpected" + invocationsTo(numUnexpected) + toString(replayArgs);
      String titleForCause = numUnexpected == 1 ? "Unexpected invocation" : "Unexpected invocations";
      return newUnexpectedInvocationWithCause(titleForCause, message);
   }

   @Nonnull
   public UnexpectedInvocation errorForUnexpectedInvocationBeforeAnother(@Nonnull ExpectedInvocation another)
   {
      return newUnexpectedInvocationWithCause("Unexpected invocation" + this, "Unexpected invocation before" + another);
   }

   @Nonnull
   public UnexpectedInvocation errorForUnexpectedInvocationFoundBeforeAnother()
   {
      String initialMessage = "Invocation occurred unexpectedly before another" + this;
      return newUnexpectedInvocationWithCause("Unexpected invocation", initialMessage);
   }

   @Nonnull
   public UnexpectedInvocation errorForUnexpectedInvocationFoundBeforeAnother(@Nonnull ExpectedInvocation another)
   {
      String initialMessage = "Another invocation unexpectedly occurred before" + another;
      return newUnexpectedInvocationWithCause("Unexpected invocation" + this, initialMessage);
   }

   @Nonnull
   public UnexpectedInvocation errorForUnexpectedInvocationAfterAnother(@Nonnull ExpectedInvocation another)
   {
      return newUnexpectedInvocationWithCause("Unexpected invocation" + this, "Unexpected invocation after" + another);
   }

   public IllegalStateException exceptionForRedundantExpectation()
   {
      IllegalStateException exception = new IllegalStateException(
         "Identical expectation already recorded; please remove this verification or adjust the recording");
      setErrorAsInvocationCause("Redundant expectation", exception);
      return exception;
   }

   @Nonnull @Override
   public String toString()
   {
      String desc = arguments.toString();

      if (instance != null) {
         desc += "\n   on mock instance: " + ObjectMethods.objectIdentity(instance);
      }

      return desc;
   }

   @Nonnull
   String toString(@Nonnull Object[] actualInvocationArguments)
   {
      Object[] invocationArgs = arguments.getValues();
      List<ArgumentMatcher<?>> matchers = arguments.getMatchers();
      arguments.setValues(actualInvocationArguments);
      arguments.setMatchers(null);
      String description = toString();
      arguments.setMatchers(matchers);
      arguments.setValues(invocationArgs);
      return description;
   }

   @Nullable
   public Error assertThatArgumentsMatch(@Nonnull Object[] replayArgs, @Nonnull Map<Object, Object> instanceMap)
   {
      return arguments.assertMatch(replayArgs, instanceMap);
   }

   // Default result //////////////////////////////////////////////////////////////////////////////////////////////////

   @Nullable
   public Object getDefaultValueForReturnType(@Nullable TestOnlyPhase phase)
   {
      if (defaultReturnValue == UNDEFINED_DEFAULT_RETURN) {
         Class<?> resolvedReturnType = getReturnTypeAsResolvedFromClassArgument();

         if (resolvedReturnType != null) {
            defaultReturnValue = DefaultValues.computeForType(resolvedReturnType);

            if (defaultReturnValue == null) {
               String returnTypeDesc = 'L' + resolvedReturnType.getName().replace('.', '/') + ';';
               String mockedTypeDesc = getClassDesc();
               Object cascadedMock = MockedTypeCascade.getMock(
                  mockedTypeDesc, arguments.methodNameAndDesc, instance, returnTypeDesc, resolvedReturnType);
               useCascadedMock(phase, cascadedMock);
            }

            return defaultReturnValue;
         }

         String returnTypeDesc = DefaultValues.getReturnTypeDesc(arguments.methodNameAndDesc);
         defaultReturnValue = DefaultValues.computeForType(returnTypeDesc);

         if (defaultReturnValue == null) {
            produceCascadedInstanceIfApplicable(phase, returnTypeDesc, arguments.genericSignature);
         }
      }

      return defaultReturnValue;
   }

   @Nullable
   private Class<?> getReturnTypeAsResolvedFromClassArgument()
   {
      String genericSignature = arguments.genericSignature;

      if (genericSignature != null) {
         int returnTypePos = genericSignature.lastIndexOf(')') + 1;
         char c = genericSignature.charAt(returnTypePos);

         if (c == 'T') {
            for (Object arg : arguments.getValues()) {
               if (arg instanceof Class<?>) {
                  return (Class<?>) arg;
               }
            }
         }
      }

      return null;
   }

   private void produceCascadedInstanceIfApplicable(
      @Nullable TestOnlyPhase phase, @Nonnull String returnTypeDesc, @Nullable String genericSignature)
   {
      String mockedTypeDesc = getClassDesc();
      Object cascadedMock = MockedTypeCascade.getMock(
         mockedTypeDesc, arguments.methodNameAndDesc, instance, returnTypeDesc, genericSignature);

      useCascadedMock(phase, cascadedMock);
   }

   private void useCascadedMock(@Nullable TestOnlyPhase phase, @Nullable Object cascadedMock)
   {
      if (cascadedMock != null) {
         if (phase != null && !TestRun.getExecutingTest().isRegularMockedInstance(cascadedMock)) {
            phase.setNextInstanceToMatch(cascadedMock);
         }

         defaultReturnValue = cascadedMock;
      }
   }

   public void copyDefaultReturnValue(@Nonnull ExpectedInvocation other)
   {
      defaultReturnValue = other.defaultReturnValue;
   }

   public boolean isRedundant(@Nonnull ExpectedInvocation other)
   {
      if (matchInstance != other.matchInstance) {
         return false;
      }

      List<ArgumentMatcher<?>> thisMatchers = arguments.getMatchers();
      List<ArgumentMatcher<?>> otherMatchers = other.arguments.getMatchers();

      if (thisMatchers == otherMatchers) {
         return true;
      }

      if (thisMatchers == null || otherMatchers == null) {
         return false;
      }

      int n = thisMatchers.size();

      if (otherMatchers.size() != n) {
         return false;
      }

      for (int i = 0; i < n; i++) {
         ArgumentMatcher<?> thisMatcher = thisMatchers.get(i);
         ArgumentMatcher<?> otherMatcher = otherMatchers.get(i);

         if (thisMatcher != otherMatcher && !thisMatcher.equals(otherMatcher)) {
            return false;
         }
      }

      return true;
   }
}
