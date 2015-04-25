/*
 * Copyright (c) 2006-2015 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.expectations.invocation;

import java.util.*;
import javax.annotation.*;

import mockit.internal.expectations.argumentMatching.*;
import mockit.internal.util.*;

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

   final void setValuesWithNoMatchers(@Nonnull Object[] argsToVerify)
   {
      values = argsToVerify;
      matchers = null;
   }

   @Nonnull
   final Object[] prepareForVerification(
      @Nonnull Object[] argsToVerify, @Nullable List<ArgumentMatcher<?>> matchersToUse)
   {
      Object[] replayArgs = values;
      values = argsToVerify;
      matchers = matchersToUse;
      return replayArgs;
   }

   @Nullable
   final ArgumentMatcher<?> getArgumentMatcher(int parameterIndex)
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
      @Nonnull Object[] expectedValues, @Nonnull Object[] actualValues, int count,
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
   abstract Error assertMatch(
      @Nonnull Object[] replayArgs, @Nonnull Map<Object, Object> instanceMap,
      @Nullable CharSequence errorMessagePrefix);

   @Nullable
   final Error assertEquals(
      @Nonnull Object[] expectedValues, @Nonnull Object[] actualValues, int count,
      @Nonnull Map<Object, Object> instanceMap, @Nullable CharSequence errorMessagePrefix)
   {
      for (int i = 0; i < count; i++) {
         Object expected = expectedValues[i];
         Object actual = actualValues[i];

         if (isNotEqual(expected, actual, instanceMap)) {
            return signature.argumentMismatchMessage(i, expected, actual, errorMessagePrefix);
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

      if (otherMatchers == null || matchers == null || otherMatchers.size() != matchers.size()) {
         return -1;
      }

      int i = 0;
      int m = matchers.size();

      while (i < m) {
         M1 matcher1 = (M1) matchers.get(i);
         M2 matcher2 = (M2) otherMatchers.get(i);

         if (matcher1 == null || matcher2 == null) {
            if (!EqualityMatcher.areEqual(values[i], other.values[i])) {
               return -1;
            }
         }
         else if (matcher1 != matcher2) {
            Class<?> matcherClass = matcher1.getClass();

            if (matcherClass != matcher2.getClass()) {
               return -1;
            }

            if (!matcher1.same((M1) matcher2)) {
               if (
                  matcherClass == ReflectiveMatcher.class || matcherClass == HamcrestAdapter.class ||
                  !equivalentMatches(matcher1, values[i], matcher2, other.values[i])
               ){
                  return -1;
               }
            }
         }

         i++;
      }

      return i;
   }

   @Nonnull
   final String toString(@Nonnull MethodFormatter methodFormatter)
   {
      ArgumentMismatch desc = new ArgumentMismatch();
      desc.append(":\n").append(methodFormatter.toString());

      int parameterCount = values.length;

      if (parameterCount > 0) {
         desc.append("\n   with arguments: ");

         if (matchers == null) {
            desc.appendFormatted(values);
         }
         else {
            List<String> parameterTypes = methodFormatter.getParameterTypes();
            String sep = "";

            for (int i = 0; i < parameterCount; i++) {
               ArgumentMatcher<?> matcher = getArgumentMatcher(i);
               String parameterType = parameterTypes.get(i);
               desc.append(sep).appendFormatted(parameterType, values[i], matcher);
               sep = ", ";
            }
         }
      }

      return desc.toString();
   }
}
