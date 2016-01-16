/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.expectations.argumentMatching;

import javax.annotation.*;

public final class NullityMatcher implements ArgumentMatcher<NullityMatcher>
{
   public static final ArgumentMatcher<?> INSTANCE = new NullityMatcher();

   private NullityMatcher() {}

   @Override
   public boolean same(@Nonnull NullityMatcher other) { return true; }

   @Override
   public boolean matches(@Nullable Object argValue) { return argValue == null; }

   @Override
   public void writeMismatchPhrase(@Nonnull ArgumentMismatch argumentMismatch) { argumentMismatch.append("null"); }
}

