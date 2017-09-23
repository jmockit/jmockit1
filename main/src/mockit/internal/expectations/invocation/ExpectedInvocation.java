/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.expectations.invocation;

import java.io.*;
import java.util.*;
import javax.annotation.*;

import mockit.external.asm.*;
import mockit.internal.*;
import mockit.internal.expectations.argumentMatching.*;
import mockit.internal.expectations.state.*;
import mockit.internal.reflection.*;
import mockit.internal.state.*;
import mockit.internal.util.*;
import mockit.internal.reflection.GenericTypeReflection.*;
import static mockit.external.asm.Type.getType;
import static mockit.internal.util.TypeDescriptor.getClassForType;

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

   // Simple getters //////////////////////////////////////////////////////////////////////////////////////////////////

   @Nonnull public String getClassDesc() { return arguments.classDesc; }
   @Nonnull public String getClassName() { return arguments.getClassName(); }
   @Nonnull public String getMethodNameAndDescription() { return arguments.methodNameAndDesc; }
   @Nonnull public Object[] getArgumentValues() { return arguments.getValues(); }
   public boolean isConstructor() { return arguments.isForConstructor(); }

   @Nullable
   public Object getRecordedInstance() { return replacementInstance != null ? replacementInstance : instance; }

   @Nonnull
   public String getSignatureWithResolvedReturnType()
   {
      String signature = arguments.genericSignature;

      if (signature != null) {
         // TODO: cache it for use in return type conversion, cascading, etc.
         String classDesc = getClassDesc();
         Class<?> mockedClass = instance != null ? instance.getClass() : ClassLoad.loadByInternalName(classDesc);
         GenericTypeReflection reflection = new GenericTypeReflection(mockedClass, null);
         signature = reflection.resolveSignature(classDesc, signature);

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
      return
         (invokedClassDesc.equals(getClassDesc()) || mock != null && TestRun.mockFixture().isCaptured(mock)) &&
         (isMatchingGenericMethod(mock, invokedMethod) || isMatchingMethod(invokedMethod));
   }

   private boolean isMatchingGenericMethod(@Nullable Object mock, @Nonnull String invokedMethod)
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

      return false;
   }

   private boolean isMatchingMethod(@Nonnull String invokedMethod)
   {
      int returnTypeStartPos = getReturnTypePosition(invokedMethod);

      if (returnTypeStartPos < 0) {
         return false;
      }

      if (haveSameReturnTypes(invokedMethod, returnTypeStartPos)) {
         return true;
      }

      // At this point the methods are known to differ only in return type, so check if the return type of
      // the recorded one is assignable to the return type of the one invoked:
      return isReturnTypeOfRecordedMethodAssignableToReturnTypeOfInvokedMethod(invokedMethod, returnTypeStartPos);
   }

   // Returns -1 if the method names or parameters are different.
   private int getReturnTypePosition(@Nonnull String invokedMethod)
   {
      String recordedMethod = getMethodNameAndDescription();
      int i = 0;

      while (true) {
         char c = recordedMethod.charAt(i);

         if (c != invokedMethod.charAt(i)) {
            return -1;
         }

         i++;

         if (c == ')') {
            return i;
         }
      }
   }

   private boolean haveSameReturnTypes(@Nonnull String invokedMethod, @Nonnegative int returnTypeStartPos)
   {
      String recordedMethod = getMethodNameAndDescription();
      int n = invokedMethod.length();

      if (n != recordedMethod.length()) {
         return false;
      }

      int j = returnTypeStartPos;

      while (true) {
         char c = recordedMethod.charAt(j);

         if (c != invokedMethod.charAt(j)) {
            return false;
         }

         j++;

         if (j == n) {
            return true;
         }
      }
   }

   private boolean isReturnTypeOfRecordedMethodAssignableToReturnTypeOfInvokedMethod(
      @Nonnull String invokedMethod, @Nonnegative int returnTypeStartPos)
   {
      String recordedMethod = getMethodNameAndDescription();
      Type recordedRT = getType(recordedMethod.substring(returnTypeStartPos));
      Type invokedRT  = getType(invokedMethod.substring(returnTypeStartPos));

      return getClassForType(invokedRT).isAssignableFrom(getClassForType(recordedRT));
   }

   public boolean isMatch(@Nonnull ExpectedInvocation other)
   {
      return isMatch(other.instance, other.getClassDesc(), other.getMethodNameAndDescription(), null);
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
      String initialMessage = "Unexpected invocation of " + this;
      return newUnexpectedInvocationWithCause("Unexpected invocation", initialMessage);
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
   public MissingInvocation errorForMissingInvocation(@Nonnull List<ExpectedInvocation> nonMatchingInvocations)
   {
      StringBuilder errorMessage = new StringBuilder(200);
      errorMessage.append("Missing invocation to:\n").append(this);
      appendNonMatchingInvocations(errorMessage, nonMatchingInvocations);

      return newMissingInvocationWithCause("Missing invocation", errorMessage.toString());
   }

   @Nonnull
   public MissingInvocation errorForMissingInvocations(
      @Nonnegative int missingInvocations, @Nonnull List<ExpectedInvocation> nonMatchingInvocations)
   {
      StringBuilder errorMessage = new StringBuilder(200);
      errorMessage.append("Missing ").append(missingInvocations).append(invocationsTo(missingInvocations)).append(this);
      appendNonMatchingInvocations(errorMessage, nonMatchingInvocations);

      return newMissingInvocationWithCause("Missing invocations", errorMessage.toString());
   }

   @Nonnull
   private static String invocationsTo(@Nonnegative int invocations)
   {
      return invocations == 1 ? " invocation to:\n" : " invocations to:\n";
   }

   private void appendNonMatchingInvocations(
      @Nonnull StringBuilder errorMessage, @Nonnull List<ExpectedInvocation> nonMatchingInvocations)
   {
      if (!nonMatchingInvocations.isEmpty()) {
         errorMessage.append("\ninstead got:\n");
         String sep = "";

         for (ExpectedInvocation nonMatchingInvocation : nonMatchingInvocations) {
            String invocationDescription = nonMatchingInvocation.toString(instance);
            errorMessage.append(sep).append(invocationDescription);
            sep = "\n";
            nonMatchingInvocation.printCause(errorMessage);
         }
      }
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

      message.append("\nwhen was expecting an invocation of:\n").append(this);

      return newUnexpectedInvocationWithCause("Unexpected invocation", message.toString());
   }

   @Nonnull
   public UnexpectedInvocation errorForUnexpectedInvocation(@Nonnull Object[] replayArgs)
   {
      String message = "Unexpected invocation to:\n" + toString(replayArgs);
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
      String titleForCause = "Unexpected invocation " + this;
      String initialMessage = "Unexpected invocation before " + another;
      return newUnexpectedInvocationWithCause(titleForCause, initialMessage);
   }

   @Nonnull
   public UnexpectedInvocation errorForUnexpectedInvocationFoundBeforeAnother()
   {
      String initialMessage = "Invocation occurred unexpectedly before another " + this;
      return newUnexpectedInvocationWithCause("Unexpected invocation", initialMessage);
   }

   @Nonnull
   public UnexpectedInvocation errorForUnexpectedInvocationFoundBeforeAnother(@Nonnull ExpectedInvocation another)
   {
      String titleForCause = "Unexpected invocation " + this;
      String initialMessage = "Another invocation unexpectedly occurred before" + another;
      return newUnexpectedInvocationWithCause(titleForCause, initialMessage);
   }

   @Nonnull
   public UnexpectedInvocation errorForUnexpectedInvocationAfterAnother(@Nonnull ExpectedInvocation another)
   {
      String titleForCause = "Unexpected invocation " + this;
      String initialMessage = "Unexpected invocation after " + another;
      return newUnexpectedInvocationWithCause(titleForCause, initialMessage);
   }

   @Nonnull @Override
   public String toString() { return toString((Object) null); }

   @Nonnull
   public String toString(@Nullable Object otherInstance)
   {
      String desc = arguments.toString();

      if (instance != otherInstance && instance != null) {
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

   public void printCause(@Nonnull Appendable errorMessage)
   {
      if (invocationCause != null) {
         try { errorMessage.append('\n'); } catch (IOException ignore) {}

         StackTrace st = new StackTrace(invocationCause);
         st.filter();
         st.print(errorMessage);
      }
   }

   @Nullable
   public Error assertThatArgumentsMatch(@Nonnull Object[] replayArgs, @Nonnull Map<Object, Object> instanceMap)
   {
      return arguments.assertMatch(replayArgs, instanceMap);
   }

   // Default result //////////////////////////////////////////////////////////////////////////////////////////////////

   @Nullable
   public Object getDefaultValueForReturnType()
   {
      if (defaultReturnValue == UNDEFINED_DEFAULT_RETURN) {
         Class<?> resolvedReturnType = getReturnTypeAsResolvedFromClassArgument();

         if (resolvedReturnType != null) {
            defaultReturnValue = DefaultValues.computeForType(resolvedReturnType);

            if (defaultReturnValue == null) {
               String returnTypeDesc = 'L' + resolvedReturnType.getName().replace('.', '/') + ';';
               String mockedTypeDesc = getClassDesc();
               defaultReturnValue = MockedTypeCascade.getMock(
                  mockedTypeDesc, arguments.methodNameAndDesc, instance, returnTypeDesc, resolvedReturnType);
            }

            return defaultReturnValue;
         }

         String returnTypeDesc = DefaultValues.getReturnTypeDesc(arguments.methodNameAndDesc);

         if ("V".equals(returnTypeDesc)) {
            return null;
         }

         defaultReturnValue = DefaultValues.computeForType(returnTypeDesc);

         if (defaultReturnValue == null) {
            String mockedTypeDesc = getClassDesc();
            defaultReturnValue = MockedTypeCascade.getMock(
               mockedTypeDesc, arguments.methodNameAndDesc, instance, returnTypeDesc, arguments.genericSignature);
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

   public void copyDefaultReturnValue(@Nonnull ExpectedInvocation other)
   {
      defaultReturnValue = other.defaultReturnValue;
   }
}
