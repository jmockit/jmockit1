/*
 * Copyright (c) 2006-2014 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.expectations.injection;

import java.lang.reflect.*;
import java.util.*;
import static java.lang.reflect.Modifier.*;

import mockit.*;
import mockit.internal.util.*;

import org.jetbrains.annotations.*;

final class TestedField
{
   @NotNull private final InjectionState injectionState;
   @NotNull private final Field testedField;
   @NotNull private final Tested metadata;
   @NotNull private final TestedObjectCreation testedObjectCreation;
   @Nullable private List<Field> targetFields;
   private boolean createAutomatically;

   TestedField(@NotNull InjectionState injectionState, @NotNull Field field, @NotNull Tested metadata)
   {
      this.injectionState = injectionState;
      testedField = field;
      this.metadata = metadata;
      testedObjectCreation = new TestedObjectCreation(injectionState, field);
   }

   boolean isAtSameLevelInTestClassHierarchy(@NotNull TestedField another)
   {
      return getDeclaringTestClass() == another.getDeclaringTestClass();
   }

   @NotNull Class<?> getDeclaringTestClass() { return testedField.getDeclaringClass(); }

   void instantiateWithInjectableValues(@NotNull Object testClassInstance)
   {
      Object testedObject = null;

      if (!createAutomatically) {
         testedObject = FieldReflection.getFieldValue(testedField, testClassInstance);
         createAutomatically = testedObject == null && !isFinal(testedField.getModifiers());
      }

      injectionState.setTestedField(testedField);

      boolean requiresAnnotation = false;
      Class<?> testedClass;

      if (createAutomatically) {
         testedClass = testedField.getType();
         testedObject = testedObjectCreation.create();
         FieldReflection.setFieldValue(testedField, testClassInstance, testedObject);
         requiresAnnotation = testedObjectCreation.constructorIsAnnotated;
      }
      else {
         testedClass = testedObject == null ? null : testedObject.getClass();
      }

      if (testedObject != null) {
         FieldInjection fieldInjection =
            new FieldInjection(injectionState, testedClass, requiresAnnotation, metadata.fullyInitialized());

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
         Object testClassInstance = injectionState.getCurrentTestClassInstance();
         FieldReflection.setFieldValue(testedField, testClassInstance, null);
      }
   }
}
