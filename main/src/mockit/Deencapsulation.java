/*
 * Copyright (c) 2006 JMockit developers
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit;

import javax.annotation.*;

import mockit.internal.reflection.*;

/**
 * Provides utility methods that enable access to ("de-encapsulate") otherwise non-accessible fields.
 *
 * @see #getField(Object, String)
 * @see #setField(Object, String, Object)
 */
public final class Deencapsulation
{
   private Deencapsulation() {}

   /**
    * Gets the value of a non-accessible (eg <tt>private</tt>) field from a given object.
    *
    * @param objectWithField the instance from which to get the field value
    * @param fieldName the name of the field to get
    * @param <T> interface or class type to which the returned value should be assignable
    *
    * @throws IllegalArgumentException if the desired field is not found
    *
    * @see #getField(Object, Class)
    * @see #getField(Class, String)
    * @see #setField(Object, String, Object)
    */
   @Nullable
   public static <T> T getField(@Nonnull Object objectWithField, @Nonnull String fieldName) {
      return FieldReflection.getField(objectWithField.getClass(), fieldName, objectWithField);
   }

   /**
    * Gets the value of a non-accessible (eg <tt>private</tt>) field from a given object, <em>assuming</em> there is only one field declared in the
    * class of the given object whose type can receive values of the specified field type.
    *
    * @param objectWithField the instance from which to get the field value
    * @param fieldType the declared type of the field, or a sub-type of the declared field type
    *
    * @throws IllegalArgumentException if either the desired field is not found, or more than one is
    *
    * @see #getField(Object, String)
    * @see #getField(Class, String)
    * @see #setField(Object, Object)
    */
   @Nullable
   public static <T> T getField(@Nonnull Object objectWithField, @Nonnull Class<T> fieldType) {
      return FieldReflection.getField(objectWithField.getClass(), fieldType, objectWithField);
   }

   /**
    * Gets the value of a non-accessible static field defined in a given class.
    *
    * @param classWithStaticField the class from which to get the field value
    * @param fieldName the name of the static field to get
    * @param <T> interface or class type to which the returned value should be assignable
    *
    * @throws IllegalArgumentException if the desired field is not found
    *
    * @see #getField(Class, Class)
    * @see #getField(Object, String)
    * @see #setField(Class, String, Object)
    */
   @Nullable
   public static <T> T getField(@Nonnull Class<?> classWithStaticField, @Nonnull String fieldName) {
      return FieldReflection.getField(classWithStaticField, fieldName, null);
   }

   /**
    * Gets the value of a non-accessible static field defined in a given class, <em>assuming</em> there is only one field declared in the given class
    * whose type can receive values of the specified field type.
    *
    * @param classWithStaticField the class from which to get the field value
    * @param fieldType the declared type of the field, or a sub-type of the declared field type
    * @param <T> interface or class type to which the returned value should be assignable
    *
    * @throws IllegalArgumentException if either the desired field is not found, or more than one is
    *
    * @see #getField(Class, String)
    * @see #getField(Object, Class)
    * @see #setField(Class, Object)
    */
   @Nullable
   public static <T> T getField(@Nonnull Class<?> classWithStaticField, @Nonnull Class<T> fieldType) {
      return FieldReflection.getField(classWithStaticField, fieldType, null);
   }

   /**
    * Sets the value of a non-accessible field on a given object.
    *
    * @param objectWithField the instance on which to set the field value
    * @param fieldName the name of the field to set
    * @param fieldValue the value to set the field to
    *
    * @throws IllegalArgumentException if the desired field is not found
    *
    * @see #setField(Class, String, Object)
    * @see #setField(Object, Object)
    * @see #getField(Object, String)
    */
   public static void setField(@Nonnull Object objectWithField, @Nonnull String fieldName, @Nullable Object fieldValue) {
      FieldReflection.setField(objectWithField.getClass(), objectWithField, fieldName, fieldValue);
   }

   /**
    * Sets the value of a non-accessible field on a given object.
    * The field is looked up by the type of the given field value instead of by name.
    *
    * @throws IllegalArgumentException if either the desired field is not found, or more than one is
    *
    * @see #setField(Object, String, Object)
    * @see #setField(Class, String, Object)
    * @see #getField(Object, String)
    */
   public static void setField(@Nonnull Object objectWithField, @Nonnull Object fieldValue) {
      FieldReflection.setField(objectWithField.getClass(), objectWithField, null, fieldValue);
   }

   /**
    * Sets the value of a non-accessible static field on a given class.
    *
    * @param classWithStaticField the class on which the static field is defined
    * @param fieldName the name of the field to set
    * @param fieldValue the value to set the field to
    *
    * @throws IllegalArgumentException if the desired field is not found
    *
    * @see #setField(Class, Object)
    * @see #setField(Object, String, Object)
    * @see #getField(Class, String)
    */
   public static void setField(@Nonnull Class<?> classWithStaticField, @Nonnull String fieldName, @Nullable Object fieldValue) {
      FieldReflection.setField(classWithStaticField, null, fieldName, fieldValue);
   }

   /**
    * Sets the value of a non-accessible static field on a given class.
    * The field is looked up by the type of the given field value instead of by name.
    *
    * @param classWithStaticField the class on which the static field is defined
    * @param fieldValue the value to set the field to
    *
    * @throws IllegalArgumentException if either the desired field is not found, or more than one is
    *
    * @see #setField(Class, String, Object)
    * @see #setField(Object, Object)
    * @see #getField(Class, Class)
    */
   public static void setField(@Nonnull Class<?> classWithStaticField, @Nonnull Object fieldValue) {
      FieldReflection.setField(classWithStaticField, null, null, fieldValue);
   }
}
