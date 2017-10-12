/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.expectations.argumentMatching;

import javax.annotation.*;

public final class AlwaysTrueMatcher implements ArgumentMatcher<AlwaysTrueMatcher>
{
   public static final ArgumentMatcher<?> ANY_STRING  = new AlwaysTrueMatcher(String.class, "String");
   public static final ArgumentMatcher<?> ANY_BOOLEAN = new AlwaysTrueMatcher(Boolean.class, "boolean");
   public static final ArgumentMatcher<?> ANY_CHAR    = new AlwaysTrueMatcher(Character.class, "char");
   public static final ArgumentMatcher<?> ANY_BYTE    = new AlwaysTrueMatcher(Byte.class, "byte");
   public static final ArgumentMatcher<?> ANY_SHORT   = new AlwaysTrueMatcher(Short.class, "short");
   public static final ArgumentMatcher<?> ANY_INT     = new AlwaysTrueMatcher(Integer.class, "int");
   public static final ArgumentMatcher<?> ANY_FLOAT   = new AlwaysTrueMatcher(Float.class, "float");
   public static final ArgumentMatcher<?> ANY_LONG    = new AlwaysTrueMatcher(Long.class, "long");
   public static final ArgumentMatcher<?> ANY_DOUBLE  = new AlwaysTrueMatcher(Double.class, "double");
   public static final ArgumentMatcher<?> ANY_VALUE   = new AlwaysTrueMatcher(Object.class, null);

   @Nonnull private final Class<?> expectedType;
   @Nullable private final String typeName;

   private AlwaysTrueMatcher(@Nonnull Class<?> expectedType, @Nullable String typeName)
   {
      this.expectedType = expectedType;
      this.typeName = typeName;
   }

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
      String parameterTypeName = typeName != null ? typeName : argumentMismatch.getParameterType();
      argumentMismatch.append("any ").append(parameterTypeName);
   }
}
