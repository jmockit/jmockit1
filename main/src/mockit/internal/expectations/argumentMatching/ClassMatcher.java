/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.expectations.argumentMatching;

import java.util.*;
import javax.annotation.*;

public final class ClassMatcher implements ArgumentMatcher<ClassMatcher>
{
   private static final Map<Class<?>, ClassMatcher> PRIMITIVE_MATCHERS;
   static {
      PRIMITIVE_MATCHERS = new IdentityHashMap<Class<?>, ClassMatcher>(16);

      PRIMITIVE_MATCHERS.put(Boolean.class, new ClassMatcher(Boolean.class));
      PRIMITIVE_MATCHERS.put(Character.class, new ClassMatcher(Character.class));
      PRIMITIVE_MATCHERS.put(Byte.class, new ClassMatcher(Byte.class));
      PRIMITIVE_MATCHERS.put(Short.class, new ClassMatcher(Short.class));
      PRIMITIVE_MATCHERS.put(Integer.class, new ClassMatcher(Integer.class));
      PRIMITIVE_MATCHERS.put(Float.class, new ClassMatcher(Float.class));
      PRIMITIVE_MATCHERS.put(Long.class, new ClassMatcher(Long.class));
      PRIMITIVE_MATCHERS.put(Double.class, new ClassMatcher(Double.class));

      PRIMITIVE_MATCHERS.put(boolean.class, new ClassMatcher(boolean.class, Boolean.class));
      PRIMITIVE_MATCHERS.put(char.class, new ClassMatcher(char.class, Character.class));
      PRIMITIVE_MATCHERS.put(byte.class, new ClassMatcher(byte.class, Byte.class));
      PRIMITIVE_MATCHERS.put(short.class, new ClassMatcher(short.class, Short.class));
      PRIMITIVE_MATCHERS.put(int.class, new ClassMatcher(int.class, Integer.class));
      PRIMITIVE_MATCHERS.put(float.class, new ClassMatcher(float.class, Float.class));
      PRIMITIVE_MATCHERS.put(long.class, new ClassMatcher(long.class, Long.class));
      PRIMITIVE_MATCHERS.put(double.class, new ClassMatcher(double.class, Double.class));
   }

   @Nonnull
   public static ClassMatcher create(@Nonnull Class<?> expectedClass)
   {
      ClassMatcher matcher = PRIMITIVE_MATCHERS.get(expectedClass);

      if (matcher == null) {
         matcher = new ClassMatcher(expectedClass);
      }

      return matcher;
   }

   @Nonnull private final String nameOfExpectedClass;
   @Nonnull private final Class<?> matchableClass;

   private ClassMatcher(@Nonnull Class<?> expectedClass)
   {
      nameOfExpectedClass = expectedClass.getName();
      matchableClass = expectedClass;
   }

   private ClassMatcher(@Nonnull Class<?> primitiveClass, @Nonnull Class<?> primitiveWrapperClass)
   {
      nameOfExpectedClass = primitiveClass.getName();
      matchableClass = primitiveWrapperClass;
   }

   @Override
   public boolean same(@Nonnull ClassMatcher other) { return matchableClass == other.matchableClass; }

   @Override
   public boolean matches(@Nullable Object argValue) { return matchableClass.isInstance(argValue); }

   @Override
   public void writeMismatchPhrase(@Nonnull ArgumentMismatch argumentMismatch)
   {
      argumentMismatch.append("an instance of ").append(nameOfExpectedClass);
   }
}
