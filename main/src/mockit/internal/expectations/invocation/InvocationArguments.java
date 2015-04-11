/*
 * Copyright (c) 2006-2015 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.expectations.invocation;

import java.lang.reflect.*;
import java.util.*;

import org.jetbrains.annotations.*;

import mockit.external.asm.*;
import mockit.internal.*;
import mockit.internal.expectations.argumentMatching.*;
import mockit.internal.state.*;
import mockit.internal.util.*;

public final class InvocationArguments
{
   @NotNull private static final String EOL = System.getProperty("line.separator");

   @NotNull final String classDesc;
   @NotNull final String methodNameAndDesc;
   @Nullable final String genericSignature;
   @NotNull private final ArgumentValuesAndMatchers valuesAndMatchers;
   @Nullable private Member realMethodOrConstructor;

   InvocationArguments(
      int access, @NotNull String classDesc, @NotNull String methodNameAndDesc, @Nullable String genericSignature,
      @NotNull Object[] args)
   {
      this.classDesc = classDesc;
      this.methodNameAndDesc = methodNameAndDesc;
      this.genericSignature = genericSignature;
      valuesAndMatchers =
         (access & Opcodes.ACC_VARARGS) == 0 ?
            new ArgumentValuesAndMatchersWithoutVarargs(this, args) :
            new ArgumentValuesAndMatchersWithVarargs(this, args);
   }

   @NotNull String getClassName() { return classDesc.replace('/', '.'); }

   boolean isForConstructor() { return methodNameAndDesc.charAt(0) == '<'; }

   @NotNull public Object[] getValues() { return valuesAndMatchers.values; }
   void setValues(@NotNull Object[] values) { valuesAndMatchers.values = values; }

   public void setValuesWithNoMatchers(@NotNull Object[] argsToVerify)
   {
      valuesAndMatchers.setValuesWithNoMatchers(argsToVerify);
   }

   @Nullable public List<ArgumentMatcher<?>> getMatchers() { return valuesAndMatchers.matchers; }
   public void setMatchers(@Nullable List<ArgumentMatcher<?>> matchers) { valuesAndMatchers.matchers = matchers; }

   @NotNull
   public Object[] prepareForVerification(@NotNull Object[] argsToVerify, @Nullable List<ArgumentMatcher<?>> matchers)
   {
      return valuesAndMatchers.prepareForVerification(argsToVerify, matchers);
   }

   public boolean isMatch(@NotNull Object[] replayArgs, @NotNull Map<Object, Object> instanceMap)
   {
      TestRun.enterNoMockingZone();
      ExecutingTest executingTest = TestRun.getExecutingTest();
      boolean previousFlag = executingTest.setShouldIgnoreMockingCallbacks(true);

      try {
         return valuesAndMatchers.isMatch(replayArgs, instanceMap);
      }
      finally {
         executingTest.setShouldIgnoreMockingCallbacks(previousFlag);
         TestRun.exitNoMockingZone();
      }
   }

   @Nullable
   public Error assertMatch(
      @NotNull Object[] replayArgs, @NotNull Map<Object, Object> instanceMap, @Nullable CharSequence errorMessagePrefix)
   {
      return valuesAndMatchers.assertMatch(replayArgs, instanceMap, errorMessagePrefix);
   }

   @NotNull
   Error argumentMismatchMessage(
      int paramIndex, @Nullable Object expected, @Nullable Object actual, @Nullable CharSequence errorMessagePrefix)
   {
      ArgumentMismatch message = new ArgumentMismatch();

      if (errorMessagePrefix != null) {
         message.append(errorMessagePrefix);
         message.append('\n');
      }

      message.append("Parameter ");

      String parameterName = ParameterNames.getName(classDesc, methodNameAndDesc, paramIndex);

      if (parameterName == null) {
         message.append(paramIndex);
      }
      else {
         message.appendFormatted(parameterName);
      }

      message.append(" of ").append(new MethodFormatter(classDesc, methodNameAndDesc).toString());
      message.append(" expected ").appendFormatted(expected);

      if (!message.isFinished()) {
         message.append(", got ").appendFormatted(actual);

         if (actual != null) {
            appendWarningMessageAboutLackOfEqualsMethod(message, actual);
         }
      }

      return new UnexpectedInvocation(message.toString());
   }

   private static void appendWarningMessageAboutLackOfEqualsMethod(
      @NotNull ArgumentMismatch message, @NotNull Object value)
   {
      Class<?> argClass = value.getClass();

      if (
         argClass == String.class || argClass == Boolean.class || argClass == Character.class ||
         Number.class.isAssignableFrom(argClass)
      ) {
         return;
      }

      Method equalsMethod;
      try { equalsMethod = argClass.getMethod("equals", Object.class); }
      catch (NoSuchMethodException e) { throw new RuntimeException(e); }

      if (equalsMethod.getDeclaringClass() == Object.class) {
         message.append(EOL);
         message.append("   Warning: argument class ").append(argClass.getName()).append(" has no \"equals\" method");
      }
   }

   @Override
   public String toString()
   {
      MethodFormatter methodFormatter = new MethodFormatter(classDesc, methodNameAndDesc);
      return valuesAndMatchers.toString(methodFormatter);
   }

   public boolean hasEquivalentMatchers(@NotNull InvocationArguments other)
   {
      return valuesAndMatchers.hasEquivalentMatchers(other.valuesAndMatchers);
   }

   @NotNull
   Member getRealMethodOrConstructor()
   {
      if (realMethodOrConstructor == null) {
         try { realMethodOrConstructor = new RealMethodOrConstructor(getClassName(), methodNameAndDesc).getMember(); }
         catch (NoSuchMethodException e) { throw new RuntimeException(e); }
      }

      return realMethodOrConstructor;
   }
}
