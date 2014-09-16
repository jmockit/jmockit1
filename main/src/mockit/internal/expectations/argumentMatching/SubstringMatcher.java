/*
 * Copyright (c) 2006-2014 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.expectations.argumentMatching;

import org.jetbrains.annotations.*;

abstract class SubstringMatcher implements ArgumentMatcher<SubstringMatcher>
{
   @NotNull final String substring;

   SubstringMatcher(@NotNull CharSequence substring) { this.substring = substring.toString(); }

   @Override
   public boolean same(@NotNull SubstringMatcher other) { return substring.equals(other.substring); }
}