/*
 * Copyright (c) 2006-2015 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.expectations.injection;

import java.lang.annotation.*;
import java.lang.reflect.*;
import java.util.*;
import javax.annotation.*;

import mockit.*;
import mockit.internal.expectations.mocking.*;

public final class TestedClassInstantiations
{
   @Nonnull private final List<TestedField> testedFields;
   @Nonnull private final List<MockedType> injectableFields;
   @Nonnull private final InjectionState injectionState;

   public TestedClassInstantiations()
   {
      testedFields = new LinkedList<TestedField>();
      injectableFields = new ArrayList<MockedType>();
      injectionState = new InjectionState();
   }

   public boolean findTestedAndInjectableFields(@Nonnull Class<?> testClass)
   {
      boolean foundTestedFields = findAllTestedAndInjectableFieldsInTestClassHierarchy(testClass);

      if (foundTestedFields) {
         new ParameterNameExtractor(true).extractNames(testClass);
      }

      return foundTestedFields;
   }

   private boolean findAllTestedAndInjectableFieldsInTestClassHierarchy(@Nonnull Class<?> testClass)
   {
      Class<?> classWithFields = testClass;

      do {
         Field[] fields = classWithFields.getDeclaredFields();
         findTestedAndInjectableFields(fields);
         classWithFields = classWithFields.getSuperclass();
      }
      while (classWithFields.getClassLoader() != null);

      return !testedFields.isEmpty();
   }

   private void findTestedAndInjectableFields(@Nonnull Field[] fieldsDeclaredInTestClass)
   {
      for (Field field : fieldsDeclaredInTestClass) {
         addAsTestedOrInjectableFieldIfApplicable(field);
      }
   }

   private void addAsTestedOrInjectableFieldIfApplicable(@Nonnull Field fieldFromTestClass)
   {
      boolean testedAnnotationFound = false;
      boolean injectableAnnotationFound = false;

      for (Annotation fieldAnnotation : fieldFromTestClass.getDeclaredAnnotations()) {
         Tested testedMetadata = testedAnnotationFound ? null : getTestedAnnotationIfPresent(fieldAnnotation);

         if (testedMetadata != null) {
            TestedField testedField = new TestedField(injectionState, fieldFromTestClass, testedMetadata);
            testedFields.add(testedField);
            testedAnnotationFound = true;
         }
         else if (fieldAnnotation instanceof Injectable) {
            MockedType mockedType = new MockedType(fieldFromTestClass);
            injectableFields.add(mockedType);
            injectableAnnotationFound = true;
         }

         if (testedAnnotationFound && injectableAnnotationFound) {
            Collections.swap(testedFields, 0, testedFields.size() - 1);
            break;
         }
      }
   }

   @Nullable
   private static Tested getTestedAnnotationIfPresent(@Nonnull Annotation fieldAnnotation)
   {
      if (fieldAnnotation instanceof Tested) {
         return (Tested) fieldAnnotation;
      }

      return fieldAnnotation.annotationType().getAnnotation(Tested.class);
   }

   public void assignNewInstancesToTestedFields(@Nonnull Object testClassInstance, boolean beforeSetup)
   {
      injectionState.buildListsOfInjectables(testClassInstance, injectableFields);

      TestedField previousField = null;

      for (TestedField testedField : testedFields) {
         if (!beforeSetup || testedField.isAvailableDuringSetup()) {
            if (previousField != null && !testedField.isAtSameLevelInTestClassHierarchy(previousField)) {
               injectionState.discardInjectablesFromLowerTestClassHierarchyLevels(testedField.getDeclaringTestClass());
            }

            try {
               testedField.instantiateWithInjectableValues(testClassInstance);
            }
            finally {
               injectionState.resetConsumedInjectables();
            }

            previousField = testedField;
         }
      }
   }

   public void clearTestedFields()
   {
      injectionState.lifecycleMethods.executePreDestroyMethodsIfAny();

      for (TestedField testedField : testedFields) {
         testedField.clearIfAutomaticCreation();
      }
   }
}
