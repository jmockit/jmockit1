/*
 * Copyright (c) 2006-2015 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.expectations.injection;

import java.lang.reflect.*;
import java.util.*;
import javax.annotation.*;
import static java.lang.reflect.Modifier.*;

import mockit.*;
import static mockit.internal.util.FieldReflection.*;

final class TestedField
{
   @Nonnull final InjectionState injectionState;
   @Nonnull private final Field testedField;
   @Nonnull private final Tested metadata;
   @Nonnull private final TestedObjectCreation testedObjectCreation;
   @Nullable private List<Field> targetFields;
   private boolean createAutomatically;
   boolean requireDIAnnotation;

   TestedField(@Nonnull InjectionState injectionState, @Nonnull Field field, @Nonnull Tested metadata)
   {
      this.injectionState = injectionState;
      testedField = field;
      this.metadata = metadata;
      testedObjectCreation = new TestedObjectCreation(injectionState, field);
   }

   boolean isAvailableDuringSetup() { return metadata.availableDuringSetup(); }

   boolean isAtSameLevelInTestClassHierarchy(@Nonnull TestedField another)
   {
      return getDeclaringTestClass() == another.getDeclaringTestClass();
   }

   @Nonnull Class<?> getDeclaringTestClass() { return testedField.getDeclaringClass(); }

   void instantiateWithInjectableValues(@Nonnull Object testClassInstance)
   {
      if (isAvailableDuringSetup() && getFieldValue(testedField, testClassInstance) != null) {
         return;
      }

      Object testedObject = null;

      if (!createAutomatically) {
         testedObject = getFieldValue(testedField, testClassInstance);
         createAutomatically = testedObject == null && !isFinal(testedField.getModifiers());
      }

      injectionState.setTestedField(testedField);

      Class<?> testedClass;

      if (createAutomatically) {
         testedClass = testedField.getType();
         testedObject = testedObjectCreation.create();
         setFieldValue(testedField, testClassInstance, testedObject);
      }
      else {
         testedClass = testedObject == null ? null : testedObject.getClass();
      }

      if (testedObject != null) {
         FieldInjection fieldInjection = new FieldInjection(this, testedClass, metadata.fullyInitialized());

         if (targetFields == null) {
            targetFields = fieldInjection.findAllTargetInstanceFieldsInTestedClassHierarchy(testedClass);
         }

         fieldInjection.injectIntoEligibleFields(targetFields, testedObject);

         if (createAutomatically) {
            injectionState.lifecycleMethods.executePostConstructMethodIfAny(testedClass, testedObject);
         }
      }
   }

   void clearIfAutomaticCreation()
   {
      if (createAutomatically) {
         injectionState.clearInstantiatedDependencies();

         Object testClassInstance = injectionState.getCurrentTestClassInstance();
         setFieldValue(testedField, testClassInstance, null);
      }
   }
}
