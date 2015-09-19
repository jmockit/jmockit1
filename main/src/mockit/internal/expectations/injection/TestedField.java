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
         if (reusePreviouslyCreatedTestedObject(testClassInstance)) {
            return;
         }

         testedClass = testedField.getType();
         testedObject = testedObjectCreation.create();
         setFieldValue(testedField, testClassInstance, testedObject);

         if (metadata.fullyInitialized()) {
            injectionState.saveInstantiatedDependency(testedClass, testedObject, false);
         }
      }
      else {
         testedClass = testedObject == null ? null : testedObject.getClass();
      }

      if (testedObject != null) {
         performFieldInjection(testedClass, testedObject);
         executeInitializationMethodIfAny(testedClass, testedObject);
      }
   }

   private boolean reusePreviouslyCreatedTestedObject(@Nonnull Object testClassInstance)
   {
      if (metadata.fullyInitialized()) {
         Class<?> testedClass = testedField.getType();
         Object testedObject = injectionState.getInstantiatedDependency(testedClass);

         if (testedObject != null) {
            setFieldValue(testedField, testClassInstance, testedObject);
            return true;
         }
      }

      return false;
   }

   private void performFieldInjection(@Nonnull Class<?> testedClass, @Nonnull Object testedObject)
   {
      FieldInjection fieldInjection = new FieldInjection(this, testedClass, metadata.fullyInitialized());

      if (targetFields == null) {
         targetFields = fieldInjection.findAllTargetInstanceFieldsInTestedClassHierarchy(testedClass);
         requireDIAnnotation = fieldInjection.requireDIAnnotation;
      }

      fieldInjection.injectIntoEligibleFields(targetFields, testedObject);
   }

   private void executeInitializationMethodIfAny(@Nonnull Class<?> testedClass, @Nonnull Object testedObject)
   {
      if (createAutomatically) {
         injectionState.lifecycleMethods.executePostConstructMethodIfAny(testedClass, testedObject);
      }
   }

   void clearIfAutomaticCreation()
   {
      if (createAutomatically) {
         injectionState.clearInstantiatedDependencies();

         if (!isAvailableDuringSetup()) {
            Object testClassInstance = injectionState.getCurrentTestClassInstance();
            setFieldValue(testedField, testClassInstance, null);
         }
      }
   }
}
