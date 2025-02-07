/*
 * Copyright (c) 2006 JMockit developers
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.injection;

import java.lang.annotation.*;
import java.lang.reflect.*;
import java.util.*;
import javax.annotation.*;

import mockit.*;
import mockit.internal.injection.field.*;
import mockit.internal.injection.full.*;
import static mockit.internal.util.AutoBoxing.*;
import static mockit.internal.util.DefaultValues.*;

abstract class TestedObject
{
   @Nonnull private final InjectionState injectionState;
   @Nonnull final Tested metadata;
   @Nonnull private final String testedName;
   @Nullable private final FullInjection fullInjection;
   @Nonnull private final TestedClass testedClass;
   @Nullable private final TestedObjectCreation testedObjectCreation;
   @Nullable private List<Field> targetFields;
   boolean createAutomatically;

   @Nullable
   static Tested getTestedAnnotationIfPresent(@Nonnull Annotation annotation) {
      if (annotation instanceof Tested) {
         return (Tested) annotation;
      }

      return annotation.annotationType().getAnnotation(Tested.class);
   }

   TestedObject(
      @Nonnull InjectionState injectionState, @Nonnull Tested metadata, @Nonnull Class<?> testClass,
      @Nonnull String testedName, @Nonnull Type testedType, @Nonnull Class<?> testedClass
   ) {
      this.injectionState = injectionState;
      this.metadata = metadata;
      this.testedName = testedName;
      fullInjection = metadata.fullyInitialized() ? new FullInjection(injectionState, testedClass, testedName) : null;

      if (testedClass.isInterface() || testedClass.isEnum() || testedClass.isPrimitive() || testedClass.isArray()) {
         testedObjectCreation = null;
         this.testedClass = new TestedClass(testedType, testedClass);
      }
      else {
         testedObjectCreation = new TestedObjectCreation(injectionState, fullInjection, testedType, testedClass);
         this.testedClass = testedObjectCreation.testedClass;
         injectionState.lifecycleMethods.findLifecycleMethods(testedClass);
      }

      this.testedClass.testClass = testClass;
   }

   boolean isAvailableDuringSetup() { return metadata.availableDuringSetup(); }

   void instantiateWithInjectableValues(@Nonnull Object testClassInstance) {
      if (alreadyInstantiated(testClassInstance)) {
         return;
      }

      Object testedObject = getExistingTestedInstanceIfApplicable(testClassInstance);
      Class<?> testedObjectClass = testedClass.targetClass;
      InjectionPoint injectionPoint = new InjectionPoint(testedClass.declaredType, testedName);

      if (isNonInstantiableType(testedObjectClass, testedObject)) {
         reusePreviouslyCreatedInstance(testClassInstance, injectionPoint);
         return;
      }

      if (testedObject == null && createAutomatically) {
         if (reusePreviouslyCreatedInstance(testClassInstance, injectionPoint)) {
            return;
         }

         testedObject = createAndRegisterNewObject(testClassInstance, injectionPoint);
      }
      else if (testedObject != null) {
         registerTestedObject(injectionPoint, testedObject);
         testedObjectClass = testedObject.getClass();
      }

      if (testedObject != null && testedObjectClass.getClassLoader() != null) {
         performFieldInjection(testedObjectClass, testedObject);

         if (createAutomatically) {
            injectionState.lifecycleMethods.executeInitializationMethodsIfAny(testedObjectClass, testedObject);
         }
      }
   }

   boolean alreadyInstantiated(@Nonnull Object testClassInstance) { return false; }

   @Nullable
   abstract Object getExistingTestedInstanceIfApplicable(@Nonnull Object testClassInstance);

   static boolean isNonInstantiableType(@Nonnull Class<?> targetClass, @Nullable Object currentValue) {
      return
         targetClass.isPrimitive() && defaultValueForPrimitiveType(targetClass).equals(currentValue) ||
         currentValue == null && (
            targetClass.isArray() || targetClass.isEnum() || targetClass.isAnnotation() || isWrapperOfPrimitiveType(targetClass)
         );
   }

   private boolean reusePreviouslyCreatedInstance(@Nonnull Object testClassInstance, @Nonnull InjectionPoint injectionPoint) {
      Object previousInstance = injectionState.getTestedInstance(injectionPoint, metadata.global());

      if (previousInstance != null) {
         setInstance(testClassInstance, previousInstance);
         return true;
      }

      return false;
   }

   abstract void setInstance(@Nonnull Object testClassInstance, @Nullable Object testedInstance);

   @Nullable
   private Object createAndRegisterNewObject(@Nonnull Object testClassInstance, @Nonnull InjectionPoint injectionPoint) {
      Object testedInstance = null;

      if (testedObjectCreation != null) {
         testedInstance = testedObjectCreation.create(false, true);

         if (testedInstance != null) {
            setInstance(testClassInstance, testedInstance);
            registerTestedObject(injectionPoint, testedInstance);
         }
      }

      return testedInstance;
   }

   private void registerTestedObject(@Nonnull InjectionPoint injectionPoint, @Nonnull Object testedObject) {
      injectionState.saveTestedObject(injectionPoint, testedObject, metadata.global());
   }

   private void performFieldInjection(@Nonnull Class<?> targetClass, @Nonnull Object testedObject) {
      FieldInjection fieldInjection = new FieldInjection(injectionState, fullInjection);

      if (targetFields == null) {
         targetFields = Injector.findAllTargetInstanceFieldsInTestedClassHierarchy(targetClass, testedClass);
      }

      fieldInjection.injectIntoEligibleFields(targetFields, testedObject, testedClass);
   }

   void clearIfAutomaticCreation(@Nonnull Object testClassInstance, boolean duringTearDown) {
      if (createAutomatically && (duringTearDown || !isAvailableDuringSetup())) {
         setInstance(testClassInstance, null);

         if (fullInjection != null) {
            fullInjection.clear();
         }
      }
   }
}