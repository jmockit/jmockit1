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
         if (targetObject != null && !field.getDeclaringClass().isInstance(targetObject)) {
            Field outerInstanceField = targetObject.getClass().getDeclaredField("this$0");
            targetObject = getFieldValue(outerInstanceField, targetObject);
         }

         //noinspection unchecked
         return (T) field.get(targetObject);
      }
      catch (IllegalAccessException | NoSuchFieldException e) { throw new RuntimeException(e); }
   }

   public static void setFieldValue(@Nonnull Field field, @Nullable Object targetObject, @Nullable Object value) {
      ensureThatMemberIsAccessible(field);

      try {
         if (targetObject != null && !field.getDeclaringClass().isInstance(targetObject)) {
            Field outerInstanceField = targetObject.getClass().getDeclaredField("this$0");
            targetObject = getFieldValue(outerInstanceField, targetObject);
         }

         field.set(targetObject, value);
      }
      catch (IllegalAccessException | NoSuchFieldException e) { throw new RuntimeException(e); }
   }
}
