/*
 * Copyright (c) 2006 JMockit developers
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.expectations.invocation;

import java.lang.reflect.*;
import java.util.*;
import javax.annotation.*;

import mockit.asm.jvmConstants.*;
import mockit.internal.expectations.argumentMatching.*;
import mockit.internal.expectations.state.*;
import mockit.internal.reflection.*;
import mockit.internal.state.*;
import mockit.internal.util.*;

public final class InvocationArguments
{
   @Nonnull final String classDesc;
   @Nonnull final String methodNameAndDesc;
   @Nullable final String genericSignature;
   @Nonnull private final ArgumentValuesAndMatchers valuesAndMatchers;
   @Nullable private Member realMethodOrConstructor;

   InvocationArguments(
      int access, @Nonnull String classDesc, @Nonnull String methodNameAndDesc, @Nullable String genericSignature, @Nonnull Object[] args
   ) {
      this.classDesc = classDesc;
      this.methodNameAndDesc = methodNameAndDesc;
      this.genericSignature = genericSignature;
      valuesAndMatchers = (access & Access.VARARGS) == 0 ?
         new ArgumentValuesAndMatchersWithoutVarargs(this, args) : new ArgumentValuesAndMatchersWithVarargs(this, args);
   }

   @Nonnull String getClassName() { return classDesc.replace('/', '.'); }

   boolean isForConstructor() { return methodNameAndDesc.charAt(0) == '<'; }

   @Nonnull public Object[] getValues() { return valuesAndMatchers.values; }
   void setValues(@Nonnull Object[] values) { valuesAndMatchers.values = values; }

   public void setValuesWithNoMatchers(@Nonnull Object[] argsToVerify) {
      valuesAndMatchers.setValuesWithNoMatchers(argsToVerify);
   }

   public void setValuesAndMatchers(@Nonnull Object[] argsToVerify, @Nullable List<ArgumentMatcher<?>> matchers) {
      valuesAndMatchers.setValuesAndMatchers(argsToVerify, matchers);
   }

   @Nullable public List<ArgumentMatcher<?>> getMatchers() { return valuesAndMatchers.matchers; }
   public void setMatchers(@Nullable List<ArgumentMatcher<?>> matchers) { valuesAndMatchers.matchers = matchers; }

   @Nonnull
   public Object[] prepareForVerification(@Nonnull Object[] argsToVerify, @Nullable List<ArgumentMatcher<?>> matchers) {
      return valuesAndMatchers.prepareForVerification(argsToVerify, matchers);
   }

   public boolean isMatch(@Nonnull Object[] replayArgs, @Nonnull Map<Object, Object> instanceMap) {
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

   @Override
   public String toString() {
      MethodFormatter methodFormatter = new MethodFormatter(classDesc, methodNameAndDesc, false);
      List<String> parameterTypes = methodFormatter.getParameterTypes();
      String arguments = valuesAndMatchers.toString(parameterTypes);
      methodFormatter.append(arguments);
      return methodFormatter.toString();
   }

   public boolean hasEquivalentMatchers(@Nonnull InvocationArguments other) {
      return valuesAndMatchers.hasEquivalentMatchers(other.valuesAndMatchers);
   }

   @Nonnull
   Member getRealMethodOrConstructor() {
      if (realMethodOrConstructor == null) {
         try { realMethodOrConstructor = new RealMethodOrConstructor(getClassName(), methodNameAndDesc).getMember(); }
         catch (NoSuchMethodException e) { throw new RuntimeException(e); }
      }

      return realMethodOrConstructor;
   }
}