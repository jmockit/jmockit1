/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.reflection;

import java.lang.reflect.*;
import javax.annotation.*;
import static java.lang.reflect.Modifier.*;

import mockit.internal.util.*;
import static mockit.internal.util.Utilities.ensureThatMemberIsAccessible;

public final class FieldReflection
{
   private FieldReflection() {}

   @Nullable
   public static <T> T getField(@Nonnull Class<?> theClass, @Nonnull String fieldName, @Nullable Object targetObject)
   {
      Field field = getDeclaredField(theClass, fieldName, targetObject != null);
      return getFieldValue(field, targetObject);
   }

   @Nonnull
   private static Field getDeclaredField(@Nonnull Class<?> theClass, @Nonnull String fieldName, boolean instanceField)
   {
      try {
         return theClass.getDeclaredField(fieldName);
      }
      catch (NoSuchFieldException ignore) {
         Class<?> superClass = theClass.getSuperclass();

         if (superClass != null && superClass != Object.class) {
            return getDeclaredField(superClass, fieldName, instanceField);
         }

         String kind = instanceField ? "instance" : "static";
         throw new IllegalArgumentException("No " + kind + " field of name \"" + fieldName + "\" found in " + theClass);
      }
   }

   @Nullable
   public static <T> T getFieldValue(@Nonnull Field field, @Nullable Object targetObject)
   {
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

   @Nullable
   public static <T> T getField(@Nonnull Class<?> theClass, @Nonnull Class<T> fieldType, @Nullable Object targetObject)
   {
      Field field = getDeclaredField(theClass, fieldType, targetObject != null, false);
      return getFieldValue(field, targetObject);
   }

   public static void setField(
      @Nonnull Class<?> theClass, @Nullable Object targetObject, @Nullable String fieldName,
      @Nullable Object fieldValue)
   {
      boolean instanceField = targetObject != null;
      Field field;

      if (fieldName != null) {
         field = getDeclaredField(theClass, fieldName, instanceField);
      }
      else if (fieldValue != null) {
         field = getDeclaredField(theClass, fieldValue.getClass(), instanceField, true);
      }
      else {
         throw new IllegalArgumentException("Missing field value when setting field by type");
      }

      setFieldValue(field, targetObject, fieldValue);
   }

   @Nonnull
   private static Field getDeclaredField(
      @Nonnull Class<?> theClass, @Nonnull Type desiredType, boolean instanceField, boolean forAssignment)
   {
      Field found = getDeclaredFieldInSingleClass(theClass, desiredType, instanceField, forAssignment);

      if (found == null) {
         Class<?> superClass = theClass.getSuperclass();

         if (superClass != null && superClass != Object.class) {
            return getDeclaredField(superClass, desiredType, instanceField, forAssignment);
         }

         StringBuilder errorMsg = new StringBuilder(instanceField ? "Instance" : "Static");
         String typeName = getTypeName(desiredType);
         errorMsg.append(" field of type ").append(typeName).append(" not found in ").append(theClass);
         throw new IllegalArgumentException(errorMsg.toString());
      }

      return found;
   }

   @Nullable
   private static Field getDeclaredFieldInSingleClass(
      @Nonnull Class<?> theClass, @Nonnull Type desiredType, boolean instanceField, boolean forAssignment)
   {
      Field found = null;

      for (Field field : theClass.getDeclaredFields()) {
         if (!field.isSynthetic()) {
            Type fieldType = field.getGenericType();

            if (
               instanceField != isStatic(field.getModifiers()) &&
               isCompatibleFieldType(fieldType, desiredType, forAssignment)
            ) {
               if (found != null) {
                  String message =
                     errorMessageForMoreThanOneFieldFound(desiredType, instanceField, forAssignment, found, field);
                  throw new IllegalArgumentException(message);
               }

               found = field;
            }
         }
      }

      return found;
   }

   private static boolean isCompatibleFieldType(
      @Nonnull Type fieldType, @Nonnull Type desiredType, boolean forAssignment)
   {
      Class<?> fieldClass = Utilities.getClassType(fieldType);
      Class<?> desiredClass = Utilities.getClassType(desiredType);

      if (ParameterReflection.isSameTypeIgnoringAutoBoxing(desiredClass, fieldClass)) {
         return true;
      }

      if (forAssignment) {
         return fieldClass.isAssignableFrom(desiredClass);
      }

      return desiredClass.isAssignableFrom(fieldClass) || fieldClass.isAssignableFrom(desiredClass);
   }

   private static String errorMessageForMoreThanOneFieldFound(
      @Nonnull Type desiredFieldType, boolean instanceField, boolean forAssignment,
      @Nonnull Field firstField, @Nonnull Field secondField)
   {
      return
         "More than one " + (instanceField ? "instance" : "static") + " field " +
         (forAssignment ? "to" : "from") + " which a value of type " + getTypeName(desiredFieldType) +
         (forAssignment ? " can be assigned" : " can be read") +
         " exists in " + secondField.getDeclaringClass() + ": " + firstField.getName() + ", " + secondField.getName();
   }

   @Nonnull
   private static String getTypeName(@Nonnull Type type)
   {
      Class<?> classType = Utilities.getClassType(type);
      Class<?> primitiveType = AutoBoxing.getPrimitiveType(classType);

      if (primitiveType != null) {
         return primitiveType + " or " + classType.getSimpleName();
      }

      String name = classType.getName();
      return name.startsWith("java.lang.") ? name.substring(10) : name;
   }

   public static void setFieldValue(@Nonnull Field field, @Nullable Object targetObject, @Nullable Object value)
   {
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
