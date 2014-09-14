/*
 * Copyright (c) 2006-2014 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.expectations.mocking;

import java.lang.annotation.*;
import java.lang.reflect.*;
import static java.lang.reflect.Modifier.*;

import org.jetbrains.annotations.*;

import mockit.*;
import mockit.internal.state.*;
import mockit.internal.util.*;

@SuppressWarnings({"EqualsAndHashcode", "ClassWithTooManyFields"})
public final class MockedType
{
   @SuppressWarnings("unused")
   @Mocked private static final Object DUMMY = null;
   private static final int DUMMY_HASHCODE;

   static
   {
      int h = 0;

      try {
         h = MockedType.class.getDeclaredField("DUMMY").getAnnotation(Mocked.class).hashCode();
      }
      catch (NoSuchFieldException ignore) {}

      DUMMY_HASHCODE = h;
   }

   @Nullable public final Field field;
   public final boolean fieldFromTestClass;
   private final int accessModifiers;
   @Nullable private final Mocked mocked;
   @Nullable private final Capturing capturing;
   @Nullable private final Cascading cascading;
   public final boolean injectable;
   @NotNull public final Type declaredType;
   @NotNull public final String mockId;
   @Nullable MockingConfiguration mockingCfg;
   @Nullable Object providedValue;

   public MockedType(@NotNull Field field)
   {
      this.field = field;
      fieldFromTestClass = true;
      accessModifiers = field.getModifiers();
      mocked = field.getAnnotation(Mocked.class);
      capturing = field.getAnnotation(Capturing.class);
      cascading = field.getAnnotation(Cascading.class);
      Injectable injectableAnnotation = field.getAnnotation(Injectable.class);
      injectable = injectableAnnotation != null;
      declaredType = field.getGenericType();
      mockId = field.getName();
      providedValue = getDefaultInjectableValue(injectableAnnotation);
      registerCascadingAsNeeded();
   }

   @Nullable
   private Object getDefaultInjectableValue(@Nullable Injectable annotation)
   {
      if (annotation != null) {
         String value = annotation.value();

         if (!value.isEmpty()) {
            Class<?> injectableClass = getClassType();

            if (injectableClass == TypeVariable.class) {
               // Not supported, do nothing.
            }
            else if (injectableClass == char.class) {
               return value.charAt(0);
            }
            else if (injectableClass == String.class) {
               return value;
            }
            else if (injectableClass.isPrimitive()) {
               Class<?> wrapperClass = AutoBoxing.getWrapperType(injectableClass);
               assert wrapperClass != null;
               Class<?>[] constructorParameters = {String.class};
               return ConstructorReflection.newInstance(wrapperClass, constructorParameters, value);
            }
            else if (injectableClass.isEnum()) {
               @SuppressWarnings({"rawtypes", "unchecked"})
               Class<? extends Enum> enumType = (Class<? extends Enum>) injectableClass;
               return Enum.valueOf(enumType, value);
            }
         }
      }

      return null;
   }

   private void registerCascadingAsNeeded()
   {
      boolean shouldCascade = mocked == null || mocked.cascading();

      if (shouldCascade && !(declaredType instanceof TypeVariable)) {
         TestRun.getExecutingTest().getCascadingTypes().add(fieldFromTestClass, declaredType, null);
      }
   }

   MockedType(
      @NotNull String testClassDesc, @NotNull String testMethodDesc, int paramIndex, @NotNull Type parameterType,
      @NotNull Annotation[] annotationsOnParameter)
   {
      field = null;
      fieldFromTestClass = false;
      accessModifiers = 0;
      mocked = getAnnotation(annotationsOnParameter, Mocked.class);
      capturing = getAnnotation(annotationsOnParameter, Capturing.class);
      cascading = getAnnotation(annotationsOnParameter, Cascading.class);
      Injectable injectableAnnotation = getAnnotation(annotationsOnParameter, Injectable.class);
      injectable = injectableAnnotation != null;
      declaredType = parameterType;

      String parameterName = ParameterNames.getName(testClassDesc, testMethodDesc, paramIndex);
      mockId = parameterName == null ? "param" + paramIndex : parameterName;

      providedValue = getDefaultInjectableValue(injectableAnnotation);
      registerCascadingAsNeeded();
   }

   @Nullable
   private static <A extends Annotation> A getAnnotation(
      @NotNull Annotation[] annotations, @NotNull Class<A> annotation)
   {
      for (Annotation paramAnnotation : annotations) {
         if (paramAnnotation.annotationType() == annotation) {
            //noinspection unchecked
            return (A) paramAnnotation;
         }
      }

      return null;
   }

   MockedType(@NotNull String cascadingMethodName, @NotNull Type cascadedType)
   {
      field = null;
      fieldFromTestClass = false;
      accessModifiers = 0;
      mocked = null;
      capturing = null;
      cascading = null;
      injectable = true;
      declaredType = cascadedType;
      mockId = cascadingMethodName;
   }

   /**
    * @return the class object corresponding to the type to be mocked, or {@code TypeVariable.class} in case the
    * mocked type is a type variable (which usually occurs when the mocked implements/extends multiple types)
    */
   @NotNull public Class<?> getClassType()
   {
      if (declaredType instanceof Class) {
         return (Class<?>) declaredType;
      }

      if (declaredType instanceof ParameterizedType) {
         ParameterizedType parameterizedType = (ParameterizedType) declaredType;
         return (Class<?>) parameterizedType.getRawType();
      }

      // Occurs when declared type is a TypeVariable, usually having two or more bound types.
      // In such cases, there isn't a single class type.
      return TypeVariable.class;
   }

   boolean isMockableType()
   {
      if (mocked == null && !injectable && cascading == null && capturing == null) {
         return false;
      }

      if (!(declaredType instanceof Class)) {
         return true;
      }

      Class<?> classType = (Class<?>) declaredType;

      if (classType.isPrimitive() || classType.isArray() || classType == Integer.class) {
         return false;
      }

      if (injectable && providedValue != null) {
         if (classType == String.class || classType.isEnum()) {
            return false;
         }
      }

      return true;
   }

   boolean isFinalFieldOrParameter() { return field == null || isFinal(accessModifiers); }

   void buildMockingConfiguration()
   {
      if (mocked == null) {
         return;
      }

      String[] filters = mocked.value();

      if (filters.length > 0) {
         mockingCfg = new MockingConfiguration(filters);
      }
   }

   boolean isClassInitializationToBeStubbedOut() { return mocked != null && mocked.stubOutClassInitialization(); }

   boolean withInstancesToCapture() { return getMaxInstancesToCapture() > 0; }

   int getMaxInstancesToCapture()
   {
      return capturing == null ? 0 : capturing.maxInstances();
   }

   @Nullable public Object getValueToInject(@Nullable Object objectWithFields)
   {
      if (field == null) {
         return providedValue;
      }

      Object value = FieldReflection.getFieldValue(field, objectWithFields);

      if (!injectable) {
         return value;
      }

      if (value == null) {
         return providedValue;
      }

      Class<?> fieldType = field.getType();

      if (!fieldType.isPrimitive()) {
         return value;
      }

      Object defaultValue = DefaultValues.defaultValueForPrimitiveType(fieldType);

      return value.equals(defaultValue) ? providedValue : value;
   }

   @Override
   public int hashCode()
   {
      int result = declaredType.hashCode();

      if (isFinal(accessModifiers)) {
         result *= 31;
      }

      if (injectable) {
         result *= 37;
      }

      if (mocked != null) {
         int h = mocked.hashCode();

         if (h != DUMMY_HASHCODE) {
            result = 31 * result + h;
         }
      }

      return result;
   }
}
