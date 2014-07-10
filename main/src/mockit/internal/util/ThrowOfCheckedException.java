/*
 * Copyright (c) 2006-2013 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.util;

import org.jetbrains.annotations.*;

public final class ThrowOfCheckedException
{
   private static Exception exceptionToThrow;

   ThrowOfCheckedException() throws Exception { throw exceptionToThrow; }

   public static synchronized void doThrow(@NotNull Exception checkedException)
   {
      exceptionToThrow = checkedException;
      ConstructorReflection.newInstanceUsingDefaultConstructor(ThrowOfCheckedException.class);
   }
}
