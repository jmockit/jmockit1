/*
 * Copyright (c) 2006 JMockit developers
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.reflection;

import java.lang.reflect.*;
import javax.annotation.*;

import static mockit.internal.util.Utilities.ensureThatMemberIsAccessible;

public final class FieldReflection
{
   private FieldReflection() {}

   @Nullable
   public static <T> T getFieldValue(@Nonnull Field field, @Nullable Object targetObject) {
      ensureThatMemberIsAccessible(field);

      try {
         //noinspection unchecked
         return (T) field.get(targetObject);
      }
      catch (IllegalAccessException e) { throw new RuntimeException(e); }
   }

   public static void setFieldValue(@Nonnull Field field, @Nullable Object targetObject, @Nullable Object value) {
      ensureThatMemberIsAccessible(field);

      try {
         field.set(targetObject, value);
      }
      catch (IllegalAccessException e) { throw new RuntimeException(e); }
   }
}
