/*
 * Copyright (c) 2006-2014 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.expectations.invocation;

import java.util.*;

import org.jetbrains.annotations.*;

import mockit.internal.*;
import mockit.external.asm.Type;

import mockit.internal.expectations.*;
import mockit.internal.expectations.argumentMatching.*;
import mockit.internal.state.*;
import mockit.internal.util.*;

public final class ExpectedInvocation
{
   private static final Object UNDEFINED_DEFAULT_RETURN = new Object();

   @Nullable public final Object instance;
   @Nullable public Object replacementInstance;
   public boolean matchInstance;
   @NotNull public final InvocationArguments arguments;
   @Nullable public CharSequence customErrorMessage;
   @Nullable private final ExpectationError invocationCause;
   @Nullable private Object defaultReturnValue;
   @Nullable private Object cascadedMock;

   public ExpectedInvocation(
      @Nullable Object mock, int access, @NotNull String mockedClassDesc, @NotNull String mockNameAndDesc,
      boolean matchInstance, @Nullable String genericSignature, @NotNull Object[] args)
   {
      instance = mock;
      this.matchInstance = matchInstance;
      arguments = new InvocationArguments(access, mockedClassDesc, mockNameAndDesc, genericSignature, args);
      invocationCause = new ExpectationError();
      determineDefaultReturnValueFromMethodSignature();
   }

   private void determineDefaultReturnValueFromMethodSignature()
   {
      Object rv = ObjectMethods.evaluateOverride(instance, getMethodNameAndDescription(), getArgumentValues());
      defaultReturnValue = rv == null ? UNDEFINED_DEFAULT_RETURN : rv;
   }

   // Simple getters //////////////////////////////////////////////////////////////////////////////////////////////////

   @NotNull public String getClassDesc() { return arguments.classDesc; }
   @NotNull public String getClassName() { return arguments.getClassName(); }
   @NotNull public String getMethodNameAndDescription() { return arguments.methodNameAndDesc; }
   @NotNull public Object[] getArgumentValues() { return arguments.getValues(); }
   public boolean isConstructor() { return arguments.isForConstructor(); }

   @Nullable public Object getRecordedInstance()
   {
      return replacementInstance != null ? replacementInstance : instance;
   }

   @NotNull public String getSignatureWithResolvedReturnType()
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

   public boolean isMatch(@NotNull String invokedClassDesc, @NotNull String invokedMethod)
   {
      return invokedClassDesc.equals(getClassDesc()) && isMatchingMethod(invokedMethod);
   }

   private boolean isMatchingMethod(@NotNull String invokedMethod)
   {
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
      @Nullable Object replayInstance, @NotNull String invokedClassDesc, @NotNull String invokedMethod,
      @Nullable Map<Object, Object> replacementMap)
   {
      return
         isMatch(invokedClassDesc, invokedMethod) &&
         (arguments.isForConstructor() || !matchInstance || isEquivalentInstance(replayInstance, replacementMap));
   }

   private boolean isEquivalentInstance(@Nullable Object mockedInstance, @Nullable Map<Object, Object> replacementMap)
   {
      return
         mockedInstance == instance ||
         replacementMap != null && replacementMap.get(mockedInstance) == instance ||
         TestRun.getExecutingTest().isInvokedInstanceEquivalentToCapturedInstance(instance, mockedInstance);
   }

   // Creation of Error instances for invocation mismatch reporting ///////////////////////////////////////////////////

   public ExpectedInvocation(
      @Nullable Object mockedInstance, @NotNull String classDesc, @NotNull String methodNameAndDesc,
      @NotNull Object[] args)
   {
      instance = mockedInstance;
      matchInstance = false;
      arguments = new InvocationArguments(0, classDesc, methodNameAndDesc, null, args);
      invocationCause = null;
   }

   @NotNull public UnexpectedInvocation errorForUnexpectedInvocation()
   {
      return newUnexpectedInvocationWithCause("Unexpected invocation", "Unexpected invocation of" + this);
   }

   @NotNull
   private UnexpectedInvocation newUnexpectedInvocationWithCause(
      @NotNull String titleForCause, @NotNull String initialMessage)
   {
      String errorMessage = getErrorMessage(initialMessage);
      UnexpectedInvocation error = new UnexpectedInvocation(errorMessage);
      setErrorAsInvocationCause(titleForCause, error);
      return error;
   }

   @NotNull private String getErrorMessage(@NotNull String initialMessage)
   {
      return customErrorMessage == null ? initialMessage : customErrorMessage + "\n" + initialMessage;
   }

   private void setErrorAsInvocationCause(@NotNull String titleForCause, @NotNull Error error)
   {
      if (invocationCause != null) {
         invocationCause.defineCause(titleForCause, error);
      }
   }

   @NotNull
   private MissingInvocation newMissingInvocationWithCause(
      @NotNull String titleForCause, @NotNull String initialMessage)
   {
      String errorMessage = getErrorMessage(initialMessage);
      MissingInvocation error = new MissingInvocation(errorMessage);
      setErrorAsInvocationCause(titleForCause, error);
      return error;
   }

   @NotNull public MissingInvocation errorForMissingInvocation()
   {
      return newMissingInvocationWithCause("Missing invocation", "Missing invocation of" + this);
   }

   @NotNull public MissingInvocation errorForMissingInvocations(int missingInvocations)
   {
      String message = "Missing " + missingInvocations + invocationsTo(missingInvocations) + this;
      return newMissingInvocationWithCause("Missing invocations", message);
   }

   @NotNull private static String invocationsTo(int invocations)
   {
      return invocations == 1 ? " invocation to" : " invocations to";
   }

   @NotNull
   public UnexpectedInvocation errorForUnexpectedInvocation(
      @Nullable Object mock, @NotNull String invokedClassDesc, @NotNull String invokedMethod,
      @NotNull Object[] replayArgs)
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

   @NotNull public UnexpectedInvocation errorForUnexpectedInvocation(@NotNull Object[] replayArgs)
   {
      String message = "unexpected invocation to" + toString(replayArgs);
      return newUnexpectedInvocationWithCause("Unexpected invocation", message);
   }

   @NotNull public UnexpectedInvocation errorForUnexpectedInvocations(@NotNull Object[] replayArgs, int numUnexpected)
   {
      String message = numUnexpected + " unexpected" + invocationsTo(numUnexpected) + toString(replayArgs);
      String titleForCause = numUnexpected == 1 ? "Unexpected invocation" : "Unexpected invocations";
      return newUnexpectedInvocationWithCause(titleForCause, message);
   }

   @NotNull public UnexpectedInvocation errorForUnexpectedInvocationBeforeAnother(@NotNull ExpectedInvocation another)
   {
      return newUnexpectedInvocationWithCause("Unexpected invocation" + this, "Unexpected invocation before" + another);
   }

   @NotNull public UnexpectedInvocation errorForUnexpectedInvocationFoundBeforeAnother()
   {
      String initialMessage = "Invocation occurred unexpectedly before another" + this;
      return newUnexpectedInvocationWithCause("Unexpected invocation", initialMessage);
   }

   @NotNull
   public UnexpectedInvocation errorForUnexpectedInvocationFoundBeforeAnother(@NotNull ExpectedInvocation another)
   {
      String initialMessage = "Another invocation unexpectedly occurred before" + another;
      return newUnexpectedInvocationWithCause("Unexpected invocation" + this, initialMessage);
   }

   @NotNull public UnexpectedInvocation errorForUnexpectedInvocationAfterAnother(@NotNull ExpectedInvocation another)
   {
      return newUnexpectedInvocationWithCause("Unexpected invocation" + this, "Unexpected invocation after" + another);
   }

   @Override
   public String toString()
   {
      String desc = arguments.toString();

      if (instance != null) {
         desc += "\n   on mock instance: " + ObjectMethods.objectIdentity(instance);
      }

      return desc;
   }

   @NotNull String toString(@NotNull Object[] actualInvocationArguments)
   {
      Object[] invocationArgs = arguments.getValues();
      List<ArgumentMatcher> matchers = arguments.getMatchers();
      arguments.setValues(actualInvocationArguments);
      arguments.setMatchers(null);
      String description = toString();
      arguments.setMatchers(matchers);
      arguments.setValues(invocationArgs);
      return description;
   }

   @Nullable
   public Error assertThatArgumentsMatch(@NotNull Object[] replayArgs, @NotNull Map<Object, Object> instanceMap)
   {
      return arguments.assertMatch(replayArgs, instanceMap, customErrorMessage);
   }

   // Default result //////////////////////////////////////////////////////////////////////////////////////////////////

   @Nullable
   public Object getDefaultValueForReturnType(@Nullable TestOnlyPhase phase)
   {
      if (defaultReturnValue == UNDEFINED_DEFAULT_RETURN) {
         String returnTypeDesc = DefaultValues.getReturnTypeDesc(arguments.methodNameAndDesc);
         defaultReturnValue = DefaultValues.computeForType(returnTypeDesc);

         if (defaultReturnValue == null) {
            String genericSignature = arguments.genericSignature;
            String genericReturnTypeDesc = null;

            if (genericSignature != null) {
               String resolvedSignature = new GenericTypeReflection().resolveReturnType(genericSignature);
               genericReturnTypeDesc = DefaultValues.getReturnTypeDesc(resolvedSignature);
            }

            produceCascadedInstanceIfApplicable(phase, returnTypeDesc, genericReturnTypeDesc);
         }
      }

      return defaultReturnValue;
   }

   private void produceCascadedInstanceIfApplicable(
      @Nullable TestOnlyPhase phase, @NotNull String returnTypeDesc, @Nullable String genericReturnTypeDesc)
   {
      String mockedTypeDesc = getClassDesc();
      cascadedMock =
         MockedTypeCascade.getMock(
            mockedTypeDesc, arguments.methodNameAndDesc, instance, returnTypeDesc, genericReturnTypeDesc);

      if (cascadedMock != null) {
         if (phase != null) {
            phase.setNextInstanceToMatch(cascadedMock);
         }

         defaultReturnValue = cascadedMock;
      }
   }

   @Nullable public Object getCascadedMock() { return cascadedMock; }

   public void copyDefaultReturnValue(@NotNull ExpectedInvocation other)
   {
      defaultReturnValue = other.defaultReturnValue;
   }
}
