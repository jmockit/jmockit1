/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.injection;

import java.lang.reflect.*;
import javax.annotation.*;

import static java.lang.reflect.Modifier.isFinal;

import mockit.*;
import mockit.internal.util.*;
import static mockit.internal.reflection.FieldReflection.*;

final class TestedField extends TestedObject
{
   @Nonnull private final Field testedField;

   TestedField(@Nonnull InjectionState injectionState, @Nonnull Field field, @Nonnull Tested metadata)
   {
      super(injectionState, metadata, field.getName(), field.getGenericType(), field.getType());
      testedField = field;
   }

   boolean isFromBaseClass(@Nonnull Class<?> testClass) { return testedField.getDeclaringClass() != testClass; }

   @Override
   boolean alreadyInstantiated(@Nonnull Object testClassInstance)
   {
      return isAvailableDuringSetup() && getFieldValue(testedField, testClassInstance) != null;
   }

   @Nullable @Override
   Object getExistingTestedInstanceIfApplicable(@Nonnull Object testClassInstance)
   {
      Object testedObject = null;

      if (!createAutomatically) {
         Class<?> targetClass = testedField.getType();
         testedObject = getFieldValue(testedField, testClassInstance);

         if (testedObject == null || isNonInstantiableType(targetClass, testedObject)) {
            String providedValue = metadata.value();

            if (!providedValue.isEmpty()) {
               testedObject = Utilities.convertFromString(targetClass, providedValue);
            }

            createAutomatically = testedObject == null && !isFinal(testedField.getModifiers());
         }
      }

      return testedObject;
   }

   @Override
   void setInstance(@Nonnull Object testClassInstance, @Nullable Object testedInstance)
   {
      setFieldValue(testedField, testClassInstance, testedInstance);
   }
}
