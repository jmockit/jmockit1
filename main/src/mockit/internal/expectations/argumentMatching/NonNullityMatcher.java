/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.expectations.argumentMatching;

import javax.annotation.*;

public final class NonNullityMatcher implements ArgumentMatcher<NonNullityMatcher>
{
   public static final ArgumentMatcher<?> INSTANCE = new NonNullityMatcher();

   private NonNullityMatcher() {}

   @Override
   public boolean same(@Nonnull NonNullityMatcher other) { return true; }

   @Override
   public boolean matches(@Nullable Object argValue) { return argValue != null; }

   @Override
   public void writeMismatchPhrase(@Nonnull ArgumentMismatch argumentMismatch) { argumentMismatch.append("not null"); }
}

