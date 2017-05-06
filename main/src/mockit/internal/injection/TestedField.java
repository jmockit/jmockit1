/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.injection;

import java.lang.reflect.*;
import java.util.*;
import javax.annotation.*;
import static java.lang.reflect.Modifier.*;

import mockit.*;
import mockit.internal.util.*;
import static mockit.internal.util.FieldReflection.*;

final class TestedField
{
   @Nonnull final InjectionState injectionState;
   @Nonnull private final Field testedField;
   @Nonnull private final Tested metadata;
   @Nullable private final FullInjection fullInjection;
   @Nonnull private final TestedClass testedClass;
   @Nullable private final TestedObjectCreation testedObjectCreation;
   @Nullable private List<Field> targetFields;
   private boolean createAutomatically;
   boolean requireDIAnnotation;

   TestedField(@Nonnull InjectionState injectionState, @Nonnull Field field, @Nonnull Tested metadata)
   {
      this.injectionState = injectionState;
      testedField = field;
      this.metadata = metadata;
      fullInjection = metadata.fullyInitialized() ? new FullInjection(injectionState, field) : null;

      Class<?> fieldType = field.getType();

      if (fieldType.isInterface() || fieldType.isEnum() || fieldType.isPrimitive() || fieldType.isArray()) {
         testedObjectCreation = null;
         testedClass = new TestedClass(field.getGenericType(), field.getType());
      }
      else {
         testedObjectCreation = new TestedObjectCreation(injectionState, fullInjection, field);
         testedClass = testedObjectCreation.testedClass;
         injectionState.lifecycleMethods.findLifecycleMethods(fieldType);
      }
   }

   boolean isAvailableDuringSetup() { return metadata.availableDuringSetup(); }

   void instantiateWithInjectableValues(@Nonnull Object testClassInstance)
   {
      if (isAvailableDuringSetup() && getFieldValue(testedField, testClassInstance) != null) {
         return;
      }

      Object testedObject = getTestedObjectFromFieldInTestClassIfApplicable(testClassInstance);
      Class<?> testedObjectClass = testedField.getType();

      if (isNonInstantiableType(testedObjectClass, testedObject)) {
         reusePreviouslyCreatedInstance(testClassInstance);
         return;
      }

      injectionState.setTestedTypeReflection(testedClass.reflection);

      if (testedObject == null && createAutomatically) {
         if (reusePreviouslyCreatedInstance(testClassInstance)) {
            return;
         }

         testedObject = createAndRegisterNewObject(testClassInstance);
      }
      else if (testedObject != null) {
         registerTestedObject(testedObject);
         testedObjectClass = testedObject.getClass();
      }

      if (testedObject != null && testedObjectClass.getClassLoader() != null) {
         performFieldInjection(testedObjectClass, testedObject);
         executeInitializationMethodsIfAny(testedObjectClass, testedObject);
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

   private static boolean isNonInstantiableType(@Nonnull Class<?> targetClass, @Nullable Object currentValue)
   {
      return
         targetClass.isPrimitive() && DefaultValues.defaultValueForPrimitiveType(targetClass).equals(currentValue) ||
         currentValue == null && (
            targetClass.isArray() || targetClass.isEnum() || targetClass.isAnnotation() ||
            AutoBoxing.isWrapperOfPrimitiveType(targetClass)
         );
   }

   private boolean reusePreviouslyCreatedInstance(@Nonnull Object testClassInstance)
   {
      Type testedType = testedField.getGenericType();
      String nameOfInjectionPoint = testedField.getName();
      Object previousInstance = injectionState.getTestedInstance(testedType, nameOfInjectionPoint);

      if (previousInstance != null) {
         setFieldValue(testedField, testClassInstance, previousInstance);
         return true;
      }

      return false;
   }

   @Nullable
   Object createAndRegisterNewObject(@Nonnull Object testClassInstance)
   {
      Object testedObject = null;

      if (testedObjectCreation != null) {
         testedObject = testedObjectCreation.create();
         setFieldValue(testedField, testClassInstance, testedObject);
         registerTestedObject(testedObject);
      }

      return testedObject;
   }

   private void registerTestedObject(@Nonnull Object testedObject)
   {
      InjectionPoint injectionPoint = new InjectionPoint(testedField.getGenericType(), testedField.getName());
      injectionState.saveTestedObject(injectionPoint, testedObject);
   }

   private void performFieldInjection(@Nonnull Class<?> targetClass, @Nonnull Object testedObject)
   {
      Type testedType = testedField.getGenericType();
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
