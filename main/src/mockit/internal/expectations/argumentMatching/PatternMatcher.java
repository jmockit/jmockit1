/*
 * Copyright (c) 2006-2014 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.expectations.argumentMatching;

import java.util.regex.*;

import org.jetbrains.annotations.*;

public final class PatternMatcher implements ArgumentMatcher
{
   @NotNull private final Pattern pattern;

   public PatternMatcher(@NotNull String regex) { pattern = Pattern.compile(regex); }

   @Override
   public boolean matches(@Nullable Object argValue)
   {
      return pattern.matcher((CharSequence) argValue).matches();
   }

   @Override
   public void writeMismatchPhrase(@NotNull ArgumentMismatch argumentMismatch)
   {
      argumentMismatch.append("a string matching \"").append(pattern.toString()).append('"');
   }
}
