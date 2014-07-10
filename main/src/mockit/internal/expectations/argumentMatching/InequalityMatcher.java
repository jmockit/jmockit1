/*
 * Copyright (c) 2006-2013 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.expectations.argumentMatching;

import org.jetbrains.annotations.*;

public final class InequalityMatcher extends EqualityMatcher
{
   public InequalityMatcher(@Nullable Object notEqualArg) { super(notEqualArg); }

   @Override
   public boolean matches(@Nullable Object argValue) { return !super.matches(argValue); }

   @Override
   public void writeMismatchPhrase(@NotNull ArgumentMismatch argumentMismatch)
   {
      argumentMismatch.append("not ").appendFormatted(object);
   }
}
