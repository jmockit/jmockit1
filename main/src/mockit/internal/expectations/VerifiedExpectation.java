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

   VerifiedExpectation(@Nonnull Expectation expectation, @Nonnull Object[] arguments, @Nullable List<ArgumentMatcher<?>> argMatchers) {
      this.expectation = expectation;
      this.arguments = arguments;
      this.argMatchers = argMatchers;
   }

   @Nullable Object captureNewInstance() { return expectation.invocation.instance; }
}
