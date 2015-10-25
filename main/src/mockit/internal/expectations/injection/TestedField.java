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
   @Nullable private final TestedObjectCreation testedObjectCreation;
   @Nullable private List<Field> targetFields;
   private boolean createAutomatically;
   boolean requireDIAnnotation;

   TestedField(@Nonnull InjectionState injectionState, @Nonnull Field field, @Nonnull Tested metadata)
   {
      this.injectionState = injectionState;
      testedField = field;
      this.metadata = metadata;

      Class<?> fieldType = field.getType();

      if (fieldType.isInterface()) {
         testedObjectCreation = null;
      }
      else {
         testedObjectCreation = new TestedObjectCreation(injectionState, field);
         injectionState.lifecycleMethods.findLifecycleMethods(fieldType);
      }
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

      injectionState.setTestedField(testedField);

      Object testedObject = getTestedObjectFromFieldInTestClassIfApplicable(testClassInstance);
      Class<?> testedClass = testedField.getType();

      if (testedObject == null && createAutomatically) {
         if (reusePreviouslyCreatedInstance(testClassInstance, testedClass)) {
            return;
         }

         if (testedObjectCreation != null) {
            testedObject = testedObjectCreation.create();
            setFieldValue(testedField, testClassInstance, testedObject);
            injectionState.saveTestedObject(testedClass, testedObject);
         }
      }
      else if (testedObject != null) {
         String dependencyKey = InjectionPoint.dependencyKey(testedClass, testedField.getName());
         injectionState.saveTestedObject(dependencyKey, testedObject);
         testedClass = testedObject.getClass();
      }

      if (testedObject != null) {
         performFieldInjection(testedClass, testedObject);
         executeInitializationMethodsIfAny(testedClass, testedObject);
      }
   }

   @Nullable
   private Object getTestedObjectFromFieldInTestClassIfApplicable(@Nonnull Object testClassInstance)
   {
      Object testedObject = null;

      if (!createAutomatically) {
         testedObject = getFieldValue(testedField, testClassInstance);
         createAutomatically = testedObject == null && !isFinal(testedField.getModifiers());
      }

      return testedObject;
   }

   private boolean reusePreviouslyCreatedInstance(@Nonnull Object testClassInstance, @Nonnull Class<?> testedClass)
   {
      Object previousInstance = injectionState.getTestedInstance(testedClass);

      if (previousInstance != null) {
         setFieldValue(testedField, testClassInstance, previousInstance);
         return true;
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

   private void executeInitializationMethodsIfAny(@Nonnull Class<?> testedClass, @Nonnull Object testedObject)
   {
      if (createAutomatically) {
         injectionState.lifecycleMethods.executeInitializationMethodsIfAny(testedClass, testedObject);
      }
   }

   void clearIfAutomaticCreation()
   {
      if (createAutomatically) {
         injectionState.clearTestedObjectsAndInstantiatedDependencies();

         if (!isAvailableDuringSetup()) {
            Object testClassInstance = injectionState.getCurrentTestClassInstance();
            setFieldValue(testedField, testClassInstance, null);
         }
      }
   }
}
