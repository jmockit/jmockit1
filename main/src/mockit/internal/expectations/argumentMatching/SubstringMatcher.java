/*
 * Copyright (c) 2006-2013 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.expectations.argumentMatching;

import org.jetbrains.annotations.*;

abstract class SubstringMatcher implements ArgumentMatcher
{
   @NotNull final String substring;

   SubstringMatcher(@NotNull CharSequence substring) { this.substring = substring.toString(); }
}