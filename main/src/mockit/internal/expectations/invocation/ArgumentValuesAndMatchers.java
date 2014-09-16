/*
 * Copyright (c) 2006-2014 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.expectations.invocation;

import java.util.*;

import org.jetbrains.annotations.*;

import mockit.internal.expectations.argumentMatching.*;
import mockit.internal.util.*;

abstract class ArgumentValuesAndMatchers
{
   @NotNull final InvocationArguments signature;
   @NotNull Object[] values;
   @Nullable List<ArgumentMatcher<?>> matchers;

   ArgumentValuesAndMatchers(@NotNull InvocationArguments signature, @NotNull Object[] values)
   {
      this.signature = signature;
      this.values = values;
   }

   final void setValuesWithNoMatchers(@NotNull Object[] argsToVerify)
   {
      values = argsToVerify;
      matchers = null;
   }

   @NotNull
   final Object[] prepareForVerification(
      @NotNull Object[] argsToVerify, @Nullable List<ArgumentMatcher<?>> matchersToUse)
   {
      Object[] replayArgs = values;
      values = argsToVerify;
      matchers = matchersToUse;
      return replayArgs;
   }

   @Nullable final ArgumentMatcher<?> getArgumentMatcher(int parameterIndex)
   {
      if (matchers == null) {
         return null;
      }

      ArgumentMatcher<?> matcher = parameterIndex < matchers.size() ? matchers.get(parameterIndex) : null;

      if (matcher == null && parameterIndex < values.length && values[parameterIndex] == null) {
         matcher = AlwaysTrueMatcher.INSTANCE;
      }

      return matcher;
   }

   abstract boolean isMatch(@NotNull Object[] replayArgs, @NotNull Map<Object, Object> instanceMap);

   static boolean areEqual(
      @NotNull Object[] expectedValues, @NotNull Object[] actualValues, int count,
      @NotNull Map<Object, Object> instanceMap)
   {
      for (int i = 0; i < count; i++) {
         if (isNotEqual(expectedValues[i], actualValues[i], instanceMap)) {
            return false;
         }
      }

      return true;
   }

   private static boolean isNotEqual(
      @Nullable Object expected, @Nullable Object actual, @NotNull Map<Object, Object> instanceMap)
   {
      return
         actual == null && expected != null ||
         actual != null && expected == null ||
         actual != null && actual != expected && expected != instanceMap.get(actual) &&
         !EqualityMatcher.areEqualWhenNonNull(actual, expected);
   }

   @Nullable
   abstract Error assertMatch(
      @NotNull Object[] replayArgs, @NotNull Map<Object, Object> instanceMap,
      @Nullable CharSequence errorMessagePrefix);

   @Nullable
   final Error assertEquals(
      @NotNull Object[] expectedValues, @NotNull Object[] actualValues, int count,
      @NotNull Map<Object, Object> instanceMap, @Nullable CharSequence errorMessagePrefix)
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

   abstract boolean hasEquivalentMatchers(@NotNull ArgumentValuesAndMatchers other);

   static boolean equivalentMatches(
      @NotNull ArgumentMatcher<?> matcher1, @Nullable Object arg1,
      @NotNull ArgumentMatcher<?> matcher2, @Nullable Object arg2)
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
      @NotNull ArgumentValuesAndMatchers other)
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

   @NotNull final String toString(@NotNull MethodFormatter methodFormatter)
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
