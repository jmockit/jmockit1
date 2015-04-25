/*
 * Copyright (c) 2006-2015 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.expectations.invocation;

import javax.annotation.*;

import mockit.internal.util.*;

final class ExpectationError extends AssertionError
{
   private String message;

   @Override
   @Nonnull public String toString() { return message; }

   void prepareForDisplay(@Nonnull String title)
   {
      message = title;
      StackTrace.filterStackTrace(this);
   }

   void defineCause(@Nonnull String title, @Nonnull Error error)
   {
      prepareForDisplay(title);
      error.initCause(this);
   }
}
