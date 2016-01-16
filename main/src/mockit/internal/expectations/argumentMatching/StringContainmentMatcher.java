/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.expectations.argumentMatching;

import javax.annotation.*;

public final class StringContainmentMatcher extends SubstringMatcher
{
   public StringContainmentMatcher(@Nonnull CharSequence substring) { super(substring); }

   @Override
   public boolean matches(@Nullable Object argValue)
   {
      return argValue instanceof CharSequence && argValue.toString().contains(substring);
   }

   @Override
   public void writeMismatchPhrase(@Nonnull ArgumentMismatch argumentMismatch)
   {
      argumentMismatch.append("a string containing ").appendFormatted(substring);
   }
}