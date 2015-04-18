/*
 * Copyright (c) 2006-2015 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.util;

import javax.annotation.*;

@SuppressWarnings("UtilityClassWithoutPrivateConstructor")
public final class ThrowOfCheckedException
{
   private static Exception exceptionToThrow;

   ThrowOfCheckedException() throws Exception { throw exceptionToThrow; }

   public static synchronized void doThrow(@Nonnull Exception checkedException)
   {
      exceptionToThrow = checkedException;
      ConstructorReflection.newInstanceUsingDefaultConstructor(ThrowOfCheckedException.class);
   }
}
