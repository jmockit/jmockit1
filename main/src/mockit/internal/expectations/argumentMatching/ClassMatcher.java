/*
 * Copyright (c) 2006-2014 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.expectations.argumentMatching;

import org.jetbrains.annotations.*;

import mockit.internal.util.*;

public final class ClassMatcher implements ArgumentMatcher
{
   @NotNull private final String nameOfExpectedClass;
   @NotNull private final Class<?> matchableClass;

   public ClassMatcher(@NotNull Class<?> expectedClass)
   {
      nameOfExpectedClass = expectedClass.getName();

      Class<?> wrapperTypeIfPrimitive = AutoBoxing.getWrapperType(expectedClass);
      matchableClass = wrapperTypeIfPrimitive == null ? expectedClass : wrapperTypeIfPrimitive;
   }

   @Override
   public boolean matches(@Nullable Object argValue)
   {
      return matchableClass.isInstance(argValue);
   }

   @Override
   public void writeMismatchPhrase(@NotNull ArgumentMismatch argumentMismatch)
   {
      argumentMismatch.append("an instance of ").append(nameOfExpectedClass);
   }
}
