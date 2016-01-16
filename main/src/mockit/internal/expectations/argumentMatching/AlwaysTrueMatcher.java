/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.expectations.argumentMatching;

import javax.annotation.*;

public final class AlwaysTrueMatcher implements ArgumentMatcher<AlwaysTrueMatcher>
{
   public static final ArgumentMatcher<?> ANY_STRING  = new AlwaysTrueMatcher(String.class);
   public static final ArgumentMatcher<?> ANY_BOOLEAN = new AlwaysTrueMatcher(Boolean.class);
   public static final ArgumentMatcher<?> ANY_CHAR    = new AlwaysTrueMatcher(Character.class);
   public static final ArgumentMatcher<?> ANY_BYTE    = new AlwaysTrueMatcher(Byte.class);
   public static final ArgumentMatcher<?> ANY_SHORT   = new AlwaysTrueMatcher(Short.class);
   public static final ArgumentMatcher<?> ANY_INT     = new AlwaysTrueMatcher(Integer.class);
   public static final ArgumentMatcher<?> ANY_FLOAT   = new AlwaysTrueMatcher(Float.class);
   public static final ArgumentMatcher<?> ANY_LONG    = new AlwaysTrueMatcher(Long.class);
   public static final ArgumentMatcher<?> ANY_DOUBLE  = new AlwaysTrueMatcher(Double.class);
   public static final ArgumentMatcher<?> ANY_VALUE   = new AlwaysTrueMatcher(Object.class);

   @Nonnull private final Class<?> expectedType;
   private AlwaysTrueMatcher(@Nonnull Class<?> expectedType) { this.expectedType = expectedType; }

   @Override
   public boolean same(@Nonnull AlwaysTrueMatcher other)
   {
      return expectedType == other.expectedType;
   }

   @Override
   public boolean matches(@Nullable Object argValue)
   {
      return argValue == null || expectedType.isInstance(argValue);
   }

   @Override
   public void writeMismatchPhrase(@Nonnull ArgumentMismatch argumentMismatch)
   {
      argumentMismatch.append("any ").append(argumentMismatch.getParameterType());
   }
}
