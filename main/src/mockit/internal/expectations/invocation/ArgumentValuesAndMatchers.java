/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.expectations.invocation;

import java.util.*;
import javax.annotation.*;

import mockit.internal.expectations.argumentMatching.*;

abstract class ArgumentValuesAndMatchers
{
   @Nonnull final InvocationArguments signature;
   @Nonnull Object[] values;
   @Nullable List<ArgumentMatcher<?>> matchers;

   ArgumentValuesAndMatchers(@Nonnull InvocationArguments signature, @Nonnull Object[] values)
   {
      this.signature = signature;
      this.values = values;
   }

   final void setValuesWithNoMatchers(@Nonnull Object[] argsToVerify) { setValuesAndMatchers(argsToVerify, null); }

   @Nonnull
   final Object[] prepareForVerification(
      @Nonnull Object[] argsToVerify, @Nullable List<ArgumentMatcher<?>> matchersToUse)
   {
      Object[] replayArgs = values;
      setValuesAndMatchers(argsToVerify, matchersToUse);
      return replayArgs;
   }

   final void setValuesAndMatchers(@Nonnull Object[] argsToVerify, @Nullable List<ArgumentMatcher<?>> matchersToUse)
   {
      values = argsToVerify;
      matchers = matchersToUse;
   }

   @Nullable
   final ArgumentMatcher<?> getArgumentMatcher(@Nonnegative int parameterIndex)
   {
      if (matchers == null) {
         return null;
      }

      ArgumentMatcher<?> matcher = parameterIndex < matchers.size() ? matchers.get(parameterIndex) : null;

      if (matcher == null && parameterIndex < values.length && values[parameterIndex] == null) {
         matcher = AlwaysTrueMatcher.ANY_VALUE;
      }

      return matcher;
   }

   abstract boolean isMatch(@Nonnull Object[] replayArgs, @Nonnull Map<Object, Object> instanceMap);

   static boolean areEqual(
      @Nonnull Object[] expectedValues, @Nonnull Object[] actualValues, @Nonnegative int count,
      @Nonnull Map<Object, Object> instanceMap)
   {
      for (int i = 0; i < count; i++) {
         if (isNotEqual(expectedValues[i], actualValues[i], instanceMap)) {
            return false;
         }
      }

      return true;
   }

   private static boolean isNotEqual(
      @Nullable Object expected, @Nullable Object actual, @Nonnull Map<Object, Object> instanceMap)
   {
      return
         actual == null && expected != null ||
         actual != null && expected == null ||
         actual != null && actual != expected && expected != instanceMap.get(actual) &&
         !EqualityMatcher.areEqualWhenNonNull(actual, expected);
   }

   @Nullable
   abstract Error assertMatch(@Nonnull Object[] replayArgs, @Nonnull Map<Object, Object> instanceMap);

   @Nullable
   final Error assertEquals(
      @Nonnull Object[] expectedValues, @Nonnull Object[] actualValues, @Nonnegative int count,
      @Nonnull Map<Object, Object> instanceMap)
   {
      for (int i = 0; i < count; i++) {
         Object expected = expectedValues[i];
         Object actual = actualValues[i];

         if (isNotEqual(expected, actual, instanceMap)) {
            return signature.argumentMismatchMessage(i, expected, actual);
         }
      }

      return null;
   }

   abstract boolean hasEquivalentMatchers(@Nonnull ArgumentValuesAndMatchers other);

   static boolean equivalentMatches(
      @Nonnull ArgumentMatcher<?> matcher1, @Nullable Object arg1,
      @Nonnull ArgumentMatcher<?> matcher2, @Nullable Object arg2)
   {
      boolean matcher1MatchesArg2 = matcher1.matches(arg2);
      boolean matcher2MatchesArg1 = matcher2.matches(arg1);

      if (arg1 != null && arg2 != null && matcher1MatchesArg2 && matcher2MatchesArg1) {
         return true;
      }

      if (arg1 == arg2 && matcher1MatchesArg2 == matcher2MatchesArg1) { // both matchers fail
         ArgumentMismatch desc1 = new ArgumentMismatch();
         matcher1.writeMismatchPhrase(desc1);
         ArgumentMismatch desc2 = new ArgumentMismatch();
         matcher2.writeMismatchPhrase(desc2);
         return desc1.toString().equals(desc2.toString());
      }

      return false;
   }

   @SuppressWarnings("unchecked")
   final <M1 extends ArgumentMatcher<M1>, M2 extends ArgumentMatcher<M2>> int indexOfFirstValueAfterEquivalentMatchers(
      @Nonnull ArgumentValuesAndMatchers other)
   {
      List<ArgumentMatcher<?>> otherMatchers = other.matchers;

      if (hasDifferentAmountOfMatchers(otherMatchers)) {
         return -1;
      }

      //noinspection ConstantConditions
      int m = matchers.size();
      int i;

      for (i = 0; i < m; i++) {
         M1 matcher1 = (M1) matchers.get(i);
         M2 matcher2 = (M2) otherMatchers.get(i);

         if (matcher1 == null || matcher2 == null) {
            if (!EqualityMatcher.areEqual(values[i], other.values[i])) {
               return -1;
            }
         }
         else if (matcher1 != matcher2) {
            if (matcher1.getClass() != matcher2.getClass()) {
               return -1;
            }

            if (!matcher1.same((M1) matcher2) && areNonEquivalentMatches(other, matcher1, matcher2, i)) {
               return -1;
            }
         }
      }

      return i;
   }

   private boolean hasDifferentAmountOfMatchers(@Nullable List<ArgumentMatcher<?>> otherMatchers)
   {
      return otherMatchers == null || matchers == null || otherMatchers.size() != matchers.size();
   }

   private boolean areNonEquivalentMatches(
      @Nonnull ArgumentValuesAndMatchers other, @Nonnull ArgumentMatcher matcher1, @Nonnull ArgumentMatcher matcher2,
      @Nonnegative int matcherIndex)
   {
      Class<?> matcherClass = matcher1.getClass();
      return
         matcherClass == ReflectiveMatcher.class || matcherClass == HamcrestAdapter.class ||
         !equivalentMatches(matcher1, values[matcherIndex], matcher2, other.values[matcherIndex]);
   }

   @Nonnull
   final String toString(@Nonnull List<String> parameterTypes)
   {
      ArgumentMismatch desc = new ArgumentMismatch();
      int parameterCount = values.length;

      if (parameterCount > 0) {
         if (matchers == null) {
            desc.appendFormatted(values);
         }
         else {
            String sep = "";

            for (int i = 0; i < parameterCount; i++) {
               ArgumentMatcher<?> matcher = getArgumentMatcher(i);
               String parameterType = parameterTypes.get(i);
               desc.append(sep).appendFormatted(parameterType, values[i], matcher);
               sep = ", ";
            }
         }

         desc.append(')');
      }

      return desc.toString();
   }
}
