/*
 * Copyright (c) 2006 JMockit developers
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.expectations.invocation;

import java.util.*;
import javax.annotation.*;

import mockit.internal.expectations.argumentMatching.*;

final class ArgumentValuesAndMatchersWithoutVarargs extends ArgumentValuesAndMatchers
{
   ArgumentValuesAndMatchersWithoutVarargs(@Nonnull InvocationArguments signature, @Nonnull Object[] values) {
      super(signature, values);
   }

   @Override
   boolean isMatch(@Nonnull Object[] replayArgs, @Nonnull Map<Object, Object> instanceMap) {
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
   boolean hasEquivalentMatchers(@Nonnull ArgumentValuesAndMatchers other) {
      @SuppressWarnings("unchecked") int i = indexOfFirstValueAfterEquivalentMatchers(other);

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