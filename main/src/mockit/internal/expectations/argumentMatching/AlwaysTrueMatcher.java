/*
 * Copyright (c) 2006-2014 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.expectations.argumentMatching;

import org.jetbrains.annotations.*;

public final class AlwaysTrueMatcher implements ArgumentMatcher<AlwaysTrueMatcher>
{
   public static final ArgumentMatcher<?> INSTANCE = new AlwaysTrueMatcher();

   private AlwaysTrueMatcher() {}

   @Override
   public boolean same(@NotNull AlwaysTrueMatcher other) { return true; }

   @Override
   public boolean matches(@Nullable Object argValue) { return true; }

   @Override
   public void writeMismatchPhrase(@NotNull ArgumentMismatch argumentMismatch)
   {
      argumentMismatch.append("any ").append(argumentMismatch.getParameterType());
   }
}
