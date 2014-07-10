/*
 * Copyright (c) 2006-2014 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.expectations.argumentMatching;

import org.jetbrains.annotations.*;

import mockit.internal.util.*;

/**
 * Adapts the {@code org.hamcrest.Matcher} interface to {@link ArgumentMatcher}.
 */
@SuppressWarnings("UnnecessaryFullyQualifiedName")
public final class HamcrestAdapter implements ArgumentMatcher
{
   @NotNull private final org.hamcrest.Matcher<?> hamcrestMatcher;

   @NotNull public static ArgumentMatcher create(@NotNull Object matcher)
   {
      if (matcher instanceof org.hamcrest.Matcher<?>) {
         return new HamcrestAdapter((org.hamcrest.Matcher<?>) matcher);
      }

      return new ReflectiveMatcher(matcher);
   }

   public HamcrestAdapter(@NotNull org.hamcrest.Matcher<?> matcher) { hamcrestMatcher = matcher; }

   @Override
   public boolean matches(@Nullable Object argValue)
   {
      return hamcrestMatcher.matches(argValue);
   }

   @Override
   public void writeMismatchPhrase(@NotNull ArgumentMismatch argumentMismatch)
   {
      org.hamcrest.Description strDescription = new org.hamcrest.StringDescription();
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
      org.hamcrest.Matcher<?> innerMatcher = hamcrestMatcher;

      while (innerMatcher instanceof org.hamcrest.core.Is || innerMatcher instanceof org.hamcrest.core.IsNot) {
         innerMatcher = FieldReflection.getField(innerMatcher.getClass(), org.hamcrest.Matcher.class, innerMatcher);
      }

      assert innerMatcher != null;
      return innerMatcher;
   }

   @Nullable private Object getArgumentValueFromMatcherIfAvailable(@NotNull Object argMatcher)
   {
      if (
         argMatcher instanceof org.hamcrest.core.IsEqual || argMatcher instanceof org.hamcrest.core.IsSame ||
         "org.hamcrest.number.OrderingComparison".equals(argMatcher.getClass().getName())
      ) {
         return FieldReflection.getField(argMatcher.getClass(), Object.class, argMatcher);
      }

      return null;
   }
}
