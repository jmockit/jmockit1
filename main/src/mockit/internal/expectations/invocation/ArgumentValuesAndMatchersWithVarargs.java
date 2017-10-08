/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.expectations.invocation;

import java.lang.reflect.*;
import java.util.*;
import javax.annotation.*;

import mockit.internal.expectations.argumentMatching.*;

final class ArgumentValuesAndMatchersWithVarargs extends ArgumentValuesAndMatchers
{
   ArgumentValuesAndMatchersWithVarargs(@Nonnull InvocationArguments signature, @Nonnull Object[] values)
   {
      super(signature, values);
   }

   @Override
   boolean isMatch(@Nonnull Object[] replayArgs, @Nonnull Map<Object, Object> instanceMap)
   {
      if (matchers == null) {
         return areEqual(replayArgs, instanceMap);
      }

      VarargsComparison varargsComparison = new VarargsComparison(replayArgs);
      int totalArgCount = varargsComparison.getTotalArgumentCountWhenDifferent();
      int regularArgCount = varargsComparison.regularArgCount;

      if (totalArgCount < 0) {
         return false;
      }

      for (int i = 0; i < totalArgCount; i++) {
         Object actual = varargsComparison.getOtherArgument(i);
         ArgumentMatcher<?> expected = getArgumentMatcher(i);

         if (expected == null) {
            Object arg = varargsComparison.getThisArgument(i);
            if (arg == null) continue;
            expected = new LenientEqualityMatcher(arg, instanceMap);
         }
         else if (i == regularArgCount && expected instanceof CaptureMatcher<?>) {
            actual = varargsComparison.getOtherVarArgs();
            i = totalArgCount;
         }

         if (!expected.matches(actual)) {
            return false;
         }
      }

      return true;
   }

   private boolean areEqual(@Nonnull Object[] replayArgs, @Nonnull Map<Object, Object> instanceMap)
   {
      int argCount = replayArgs.length;

      if (!areEqual(values, replayArgs, argCount - 1, instanceMap)) {
         return false;
      }

      VarargsComparison varargsComparison = new VarargsComparison(replayArgs);
      Object[] expectedValues = varargsComparison.getThisVarArgs();
      Object[] actualValues = varargsComparison.getOtherVarArgs();

      return
         varargsComparison.sameVarargArrayLength() &&
         areEqual(expectedValues, actualValues, expectedValues.length, instanceMap);
   }

   @Nullable @Override
   Error assertMatch(
      @Nonnull Object[] replayArgs, @Nonnull Map<Object, Object> instanceMap)
   {
      if (matchers == null) {
         return assertEquality(replayArgs, instanceMap);
      }

      VarargsComparison varargsComparison = new VarargsComparison(replayArgs);
      int n = varargsComparison.getTotalArgumentCountWhenDifferent();

      if (n < 0) {
         return varargsComparison.errorForVarargsArraysOfDifferentLengths();
      }

      for (int i = 0; i < n; i++) {
         Object actual = varargsComparison.getOtherArgument(i);
         ArgumentMatcher<?> expected = getArgumentMatcher(i);

         if (expected == null) {
            Object arg = varargsComparison.getThisArgument(i);
            if (arg == null) continue;
            expected = new LenientEqualityMatcher(arg, instanceMap);
         }

         if (!expected.matches(actual)) {
            int paramIndex = i < replayArgs.length ? i : replayArgs.length - 1;
            return signature.argumentMismatchMessage(paramIndex, expected, actual);
         }
      }

      return null;
   }

   @Nullable
   private Error assertEquality(@Nonnull Object[] replayArgs, @Nonnull Map<Object, Object> instanceMap)
   {
      int argCount = replayArgs.length;
      Error nonVarargsError = assertEquals(values, replayArgs, argCount - 1, instanceMap);

      if (nonVarargsError != null) {
         return nonVarargsError;
      }

      VarargsComparison varargsComparison = new VarargsComparison(replayArgs);
      Object[] expectedValues = varargsComparison.getThisVarArgs();
      Object[] actualValues = varargsComparison.getOtherVarArgs();

      if (!varargsComparison.sameVarargArrayLength()) {
         return varargsComparison.errorForVarargsArraysOfDifferentLengths();
      }

      Error varargsError = assertEquals(expectedValues, actualValues, expectedValues.length, instanceMap);

      if (varargsError != null) {
         return new UnexpectedInvocation("Varargs " + varargsError);
      }

      return null;
   }

   @Override
   boolean hasEquivalentMatchers(@Nonnull ArgumentValuesAndMatchers other)
   {
      @SuppressWarnings("unchecked") int i = indexOfFirstValueAfterEquivalentMatchers(other);

      if (i < 0) {
         return false;
      }

      VarargsComparison varargsComparison = new VarargsComparison(other.values);
      int n = varargsComparison.getTotalArgumentCountWhenDifferent();

      if (n < 0) {
         return false;
      }

      while (i < n) {
         Object thisArg = varargsComparison.getThisArgument(i);
         Object otherArg = varargsComparison.getOtherArgument(i);

         if (!EqualityMatcher.areEqual(thisArg, otherArg)) {
            return false;
         }

         i++;
      }

      return true;
   }
   
   private static final Object[] NULL_VARARGS = new Object[0];

   private final class VarargsComparison
   {
      @Nonnull private final Object[] otherValues;
      @Nullable private final Object[] thisVarArgs;
      @Nullable private final Object[] otherVarArgs;
      final int regularArgCount;

      VarargsComparison(@Nonnull Object[] otherValues)
      {
         this.otherValues = otherValues;
         thisVarArgs = getVarArgs(values);
         otherVarArgs = getVarArgs(otherValues);
         regularArgCount = values.length - 1;
      }

      @Nonnull Object[] getThisVarArgs()  { return thisVarArgs  == null ? NULL_VARARGS : thisVarArgs; }
      @Nonnull Object[] getOtherVarArgs() { return otherVarArgs == null ? NULL_VARARGS : otherVarArgs; }

      @Nullable
      private Object[] getVarArgs(@Nonnull Object[] args)
      {
         Object lastArg = args[args.length - 1];

         if (lastArg == null) {
            return null;
         }

         if (lastArg instanceof Object[]) {
            return (Object[]) lastArg;
         }

         int varArgsLength = Array.getLength(lastArg);
         Object[] results = new Object[varArgsLength];

         for (int i = 0; i < varArgsLength; i++) {
            results[i] = Array.get(lastArg, i);
         }

         return results;
      }

      int getTotalArgumentCountWhenDifferent()
      {
         if (thisVarArgs == null) {
            return regularArgCount + 1;
         }

         if (!sameVarargArrayLength()) {
            return -1;
         }

         return regularArgCount + thisVarArgs.length;
      }

      boolean sameVarargArrayLength() { return getThisVarArgs().length == getOtherVarArgs().length; }

      @Nullable
      Object getThisArgument(int parameter)
      {
         if (parameter < regularArgCount) return values[parameter];
         int p = parameter - regularArgCount;
         if (thisVarArgs == null || p >= thisVarArgs.length) return null;
         return thisVarArgs[p];
      }

      @Nullable
      Object getOtherArgument(int parameter)
      {
         if (parameter < regularArgCount) return otherValues[parameter];
         int p = parameter - regularArgCount;
         if (otherVarArgs == null || p >= otherVarArgs.length) return null;
         return otherVarArgs[p];
      }

      @Nonnull
      Error errorForVarargsArraysOfDifferentLengths()
      {
         int n = getThisVarArgs().length;
         int m = getOtherVarArgs().length;
         return new UnexpectedInvocation("Expected " + n + " values for varargs parameter, got " + m);
      }
   }
}
