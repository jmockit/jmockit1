/*
 * Copyright (c) 2006-2013 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.expectations.argumentMatching;

import org.jetbrains.annotations.*;

/**
 * An argument matcher for the recording/verification of expectations.
 */
public interface ArgumentMatcher
{
   /**
    * Evaluates the matcher for the given argument.
    */
   boolean matches(@Nullable Object argValue);

   /**
    * Writes a phrase to be part of an error message describing an argument mismatch.
    */
   void writeMismatchPhrase(@NotNull ArgumentMismatch argumentMismatch);
}
