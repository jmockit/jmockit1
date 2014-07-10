/*
 * Copyright (c) 2006-2013 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.expectations.invocation;

import org.jetbrains.annotations.*;

import mockit.internal.util.*;

final class ExpectationError extends AssertionError
{
   @NotNull private String message;

   @Override
   @NotNull public String toString() { return message; }

   void prepareForDisplay(@NotNull String title)
   {
      message = title;
      StackTrace.filterStackTrace(this);
   }

   void defineCause(@NotNull String title, @NotNull Error error)
   {
      prepareForDisplay(title);
      error.initCause(this);
   }
}
