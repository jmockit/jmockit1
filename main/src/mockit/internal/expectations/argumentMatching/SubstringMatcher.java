/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.expectations.argumentMatching;

import javax.annotation.*;

abstract class SubstringMatcher implements ArgumentMatcher<SubstringMatcher>
{
   @Nonnull final String substring;

   SubstringMatcher(@Nonnull CharSequence substring) { this.substring = substring.toString(); }

   @Override
   public final boolean same(@Nonnull SubstringMatcher other) { return substring.equals(other.substring); }
}