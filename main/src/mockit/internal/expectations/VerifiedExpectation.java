/*
 * Copyright (c) 2006 JMockit developers
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.expectations;

import java.util.*;
import javax.annotation.*;

import mockit.internal.expectations.argumentMatching.*;

final class VerifiedExpectation
{
   @Nonnull final Expectation expectation;
   @Nonnull final Object[] arguments;
   @Nullable final List<ArgumentMatcher<?>> argMatchers;
   private final int replayIndex;

   VerifiedExpectation(
      @Nonnull Expectation expectation, @Nonnull Object[] arguments, @Nullable List<ArgumentMatcher<?>> argMatchers, int replayIndex
   ) {
      this.expectation = expectation;
      this.arguments = arguments;
      this.argMatchers = argMatchers;
      this.replayIndex = replayIndex;
   }

   boolean matchesReplayIndex(@Nonnegative int expectationIndex) {
      return replayIndex < 0 || replayIndex == expectationIndex;
   }

   @Nullable Object captureNewInstance() { return expectation.invocation.instance; }
}
