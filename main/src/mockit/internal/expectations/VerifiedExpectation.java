/*
 * Copyright (c) 2006-2014 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.expectations;

import java.util.*;

import org.jetbrains.annotations.*;

import mockit.internal.expectations.argumentMatching.*;

final class VerifiedExpectation
{
   @NotNull final Expectation expectation;
   @NotNull final Object[] arguments;
   @Nullable final List<ArgumentMatcher> argMatchers;
   final int replayIndex;

   VerifiedExpectation(
      @NotNull Expectation expectation, @NotNull Object[] arguments, @Nullable List<ArgumentMatcher> argMatchers,
      int replayIndex)
   {
      this.expectation = expectation;
      this.arguments = arguments;
      this.argMatchers = argMatchers;
      this.replayIndex = replayIndex;
   }

   @Nullable Object captureNewInstance() { return expectation.invocation.instance; }
}
