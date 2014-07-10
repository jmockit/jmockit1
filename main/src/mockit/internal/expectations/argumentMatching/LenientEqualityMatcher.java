/*
 * Copyright (c) 2006-2014 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.expectations.argumentMatching;

import java.util.*;

import org.jetbrains.annotations.*;

public final class LenientEqualityMatcher extends EqualityMatcher
{
   @NotNull private final Map<Object, Object> instanceMap;

   public LenientEqualityMatcher(@Nullable Object equalArg, @NotNull Map<Object, Object> instanceMap)
   {
      super(equalArg);
      this.instanceMap = instanceMap;
   }

   @Override
   public boolean matches(@Nullable Object argValue)
   {
      if (argValue == null) {
         return object == null;
      }
      else if (object == null) {
         return false;
      }
      else if (argValue == object || instanceMap.get(argValue) == object) {
         return true;
      }

      return areEqualWhenNonNull(argValue, object);
   }
}
