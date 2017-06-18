/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.expectations.argumentMatching;

import java.lang.reflect.Array;
import javax.annotation.*;

public class EqualityMatcher implements ArgumentMatcher<EqualityMatcher>
{
   @Nullable final Object object;

   EqualityMatcher(@Nullable Object equalArg) { object = equalArg; }

   @Override
   public final boolean same(@Nonnull EqualityMatcher other) { return object == other.object; }

   @Override
   public boolean matches(@Nullable Object argValue) { return areEqual(argValue, object); }

   @Override
   public void writeMismatchPhrase(@Nonnull ArgumentMismatch argumentMismatch)
   {
      argumentMismatch.appendFormatted(object);
   }

   public static boolean areEqual(@Nullable Object o1, @Nullable Object o2)
   {
      if (o1 == null) {
         return o2 == null;
      }

      return o2 != null && (o1 == o2 || areEqualWhenNonNull(o1, o2));
   }

   public static boolean areEqualWhenNonNull(@Nonnull Object o1, @Nonnull Object o2)
   {
      if (isArray(o1)) {
         return isArray(o2) && areArraysEqual(o1, o2);
      }

      return o1.equals(o2);
   }

   private static boolean isArray(@Nonnull Object o) { return o.getClass().isArray(); }

   private static boolean areArraysEqual(@Nonnull Object array1, @Nonnull Object array2)
   {
      int length1 = Array.getLength(array1);

      if (length1 != Array.getLength(array2)) {
         return false;
      }

      for (int i = 0; i < length1; i++) {
         Object value1 = Array.get(array1, i);
         Object value2 = Array.get(array2, i);

         if (!areEqual(value1, value2)) {
            return false;
         }
      }

      return true;
   }
}
