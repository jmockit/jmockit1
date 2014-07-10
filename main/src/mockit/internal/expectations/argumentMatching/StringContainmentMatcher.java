/*
 * Copyright (c) 2006-2014 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.expectations.argumentMatching;

import org.jetbrains.annotations.*;

public final class StringContainmentMatcher extends SubstringMatcher
{
   public StringContainmentMatcher(@NotNull CharSequence substring) { super(substring); }

   @Override
   public boolean matches(@Nullable Object string)
   {
      return string instanceof CharSequence && string.toString().contains(substring);
   }

   @Override
   public void writeMismatchPhrase(@NotNull ArgumentMismatch argumentMismatch)
   {
      argumentMismatch.append("a string containing ").appendFormatted(substring);
   }
}