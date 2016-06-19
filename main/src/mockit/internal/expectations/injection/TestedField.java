/*
 * Copyright (c) 2006 Rogério Liesenfeld
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
   @Nullable private final FullInjection fullInjection;
   @Nullable private final TestedObjectCreation testedObjectCreation;
   @Nullable private List<Field> targetFields;
   private boolean createAutomatically;
   boolean requireDIAnnotation;

   TestedField(@Nonnull InjectionState injectionState, @Nonnull Field field, @Nonnull Tested metadata)
   {
      this.injectionState = injectionState;
      testedField = field;
      this.metadata = metadata;
      fullInjection = metadata.fullyInitialized() ? new FullInjection(injectionState) : null;

      Class<?> fieldType = field.getType();

      if (fieldType.isInterface()) {
         testedObjectCreation = null;
      }
      else {
         testedObjectCreation = new TestedObjectCreation(injectionState, fullInjection, field);
         injectionState.lifecycleMethods.findLifecycleMethods(fieldType);
      }
   }

   boolean isAvailableDuringSetup() { return metadata.availableDuringSetup(); }

   void instantiateWithInjectableValues(@Nonnull Object testClassInstance)
   {
      if (isAvailableDuringSetup() && getFieldValue(testedField, testClassInstance) != null) {
         return;
      }

      injectionState.setTestedField(testedField);

      Object testedObject = getTestedObjectFromFieldInTestClassIfApplicable(testClassInstance);
      Type testedType = testedField.getGenericType();
      Class<?> testedClass = testedField.getType();

      if (testedObject == null && createAutomatically) {
         if (reusePreviouslyCreatedInstance(testClassInstance, testedType)) {
            return;
         }

         if (testedObjectCreation != null) {
            testedObject = testedObjectCreation.create();
            setFieldValue(testedField, testClassInstance, testedObject);
            registerTestedObject(testedObject);
         }
      }
      else if (testedObject != null) {
         registerTestedObject(testedObject);
         testedClass = testedObject.getClass();
      }

      if (testedObject != null && testedClass.getClassLoader() != null) {
         performFieldInjection(testedType, testedClass, testedObject);
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

   private boolean reusePreviouslyCreatedInstance(@Nonnull Object testClassInstance, @Nonnull Type testedClass)
   {
      Object previousInstance = injectionState.getTestedInstance(testedClass);

      if (previousInstance != null) {
         setFieldValue(testedField, testClassInstance, previousInstance);
         return true;
      }

      return false;
   }

   private void registerTestedObject(@Nonnull Object testedObject)
   {
      InjectionPoint injectionPoint = new InjectionPoint(testedField.getGenericType(), testedField.getName());
      injectionState.saveTestedObject(injectionPoint, testedObject);
   }

   private void performFieldInjection(
      @Nonnull Type testedType, @Nonnull Class<?> targetClass, @Nonnull Object testedObject)
   {
      TestedClass testedClass = testedObjectCreation == null ? null : testedObjectCreation.testedClass;

      if (testedClass == null || targetClass != testedClass.targetClass) {
         testedClass = new TestedClass(testedType, targetClass);
      }

      FieldInjection fieldInjection = new FieldInjection(this, testedClass, fullInjection);

      if (targetFields == null) {
         targetFields = fieldInjection.findAllTargetInstanceFieldsInTestedClassHierarchy(targetClass);
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
