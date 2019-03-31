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

      if (targetObject != null && !field.getDeclaringClass().isInstance(targetObject)) {
         Field outerInstanceField = getDeclaredField(targetObject.getClass(), "this$0", true);
         targetObject = getFieldValue(outerInstanceField, targetObject);
      }

      try {
         //noinspection unchecked
         return (T) field.get(targetObject);
      }
      catch (IllegalAccessException e) { throw new RuntimeException(e); }
   }

   @Nonnull
   private static Field getDeclaredField(@Nonnull Class<?> theClass, @Nonnull String fieldName, boolean instanceField) {
      try {
         return theClass.getDeclaredField(fieldName);
      }
      catch (NoSuchFieldException ignore) {
         Class<?> superClass = theClass.getSuperclass();

         if (superClass != null && superClass != Object.class) {
            //noinspection TailRecursion
            return getDeclaredField(superClass, fieldName, instanceField);
         }

         String kind = instanceField ? "instance" : "static";
         throw new IllegalArgumentException("No " + kind + " field of name \"" + fieldName + "\" found in " + theClass);
      }
   }

   public static void setFieldValue(@Nonnull Field field, @Nullable Object targetObject, @Nullable Object value) {
      ensureThatMemberIsAccessible(field);

      if (targetObject != null && !field.getDeclaringClass().isInstance(targetObject)) {
         Field outerInstanceField = getDeclaredField(targetObject.getClass(), "this$0", true);
         targetObject = getFieldValue(outerInstanceField, targetObject);
      }

      try {
         field.set(targetObject, value);
      }
      catch (IllegalAccessException e) { throw new RuntimeException(e); }
   }
}
