/*
 * Copyright (c) 2006-2014 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.expectations.argumentMatching;

import mockit.internal.util.*;

import org.hamcrest.*;
import org.hamcrest.core.*;
import org.jetbrains.annotations.*;

/**
 * Adapts the {@code org.hamcrest.Matcher} interface to {@link ArgumentMatcher}.
 */
public final class HamcrestAdapter implements ArgumentMatcher<HamcrestAdapter>
{
   @NotNull private final Matcher<?> hamcrestMatcher;

   public HamcrestAdapter(@NotNull Matcher<?> matcher) { hamcrestMatcher = matcher; }

   @Override
   public boolean same(@NotNull HamcrestAdapter other) { return hamcrestMatcher == other.hamcrestMatcher; }

   @Override
   public boolean matches(@Nullable Object argValue)
   {
      return hamcrestMatcher.matches(argValue);
   }

   @Override
   public void writeMismatchPhrase(@NotNull ArgumentMismatch argumentMismatch)
   {
      Description strDescription = new StringDescription();
      hamcrestMatcher.describeTo(strDescription);
      argumentMismatch.append(strDescription.toString());
   }

   @Nullable public Object getInnerValue()
   {
      Object innermostMatcher = getInnermostMatcher();
      return getArgumentValueFromMatcherIfAvailable(innermostMatcher);
   }

   @NotNull private Object getInnermostMatcher()
   {
      Matcher<?> innerMatcher = hamcrestMatcher;

      while (innerMatcher instanceof Is || innerMatcher instanceof IsNot) {
         innerMatcher = FieldReflection.getField(innerMatcher.getClass(), Matcher.class, innerMatcher);
      }

      assert innerMatcher != null;
      return innerMatcher;
   }

   @Nullable
   private static Object getArgumentValueFromMatcherIfAvailable(@NotNull Object argMatcher)
   {
      if (
         argMatcher instanceof IsEqual || argMatcher instanceof IsSame ||
         "org.hamcrest.number.OrderingComparison".equals(argMatcher.getClass().getName())
      ) {
         return FieldReflection.getField(argMatcher.getClass(), Object.class, argMatcher);
      }

      return null;
   }
}
