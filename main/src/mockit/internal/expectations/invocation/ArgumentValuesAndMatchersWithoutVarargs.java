/*
 * Copyright (c) 2006-2014 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.expectations.invocation;

import java.util.*;

import org.jetbrains.annotations.*;

import mockit.internal.expectations.argumentMatching.*;

final class ArgumentValuesAndMatchersWithoutVarargs extends ArgumentValuesAndMatchers
{
   ArgumentValuesAndMatchersWithoutVarargs(@NotNull InvocationArguments signature, @NotNull Object[] values)
   {
      super(signature, values);
   }

   @Override
   boolean isMatch(@NotNull Object[] replayArgs, @NotNull Map<Object, Object> instanceMap)
   {
      if (matchers == null) {
         return areEqual(values, replayArgs, replayArgs.length, instanceMap);
      }

      for (int i = 0; i < replayArgs.length; i++) {
         Object actual = replayArgs[i];
         ArgumentMatcher<?> expected = getArgumentMatcher(i);

         if (expected == null) {
            Object arg = values[i];
            if (arg == null) continue;
            expected = new LenientEqualityMatcher(arg, instanceMap);
         }

         if (!expected.matches(actual)) {
            return false;
         }
      }

      return true;
   }

   @Override
   @Nullable
   Error assertMatch(
      @NotNull Object[] replayArgs, @NotNull Map<Object, Object> instanceMap, @Nullable CharSequence errorMessagePrefix)
   {
      if (matchers == null) {
         return assertEquals(values, replayArgs, replayArgs.length, instanceMap, errorMessagePrefix);
      }

      for (int i = 0; i < replayArgs.length; i++) {
         Object actual = replayArgs[i];
         ArgumentMatcher<?> expected = getArgumentMatcher(i);

         if (expected == null) {
            Object arg = values[i];
            if (arg == null) continue;
            expected = new LenientEqualityMatcher(arg, instanceMap);
         }

         if (!expected.matches(actual)) {
            return signature.argumentMismatchMessage(i, expected, actual, errorMessagePrefix);
         }
      }

      return null;
   }

   @Override
   boolean hasEquivalentMatchers(@NotNull ArgumentValuesAndMatchers other)
   {
      int i = indexOfFirstValueAfterEquivalentMatchers(other);

      if (i < 0) {
         return false;
      }

      while (i < values.length) {
         if (!EqualityMatcher.areEqual(values[i], other.values[i])) {
            return false;
         }

         i++;
      }

      return true;
   }
}
