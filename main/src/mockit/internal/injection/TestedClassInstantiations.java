/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.injection;

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
      findAllTestedAndInjectableFieldsInTestClassHierarchy(testClass);
      return !testedFields.isEmpty();
   }

   private void findAllTestedAndInjectableFieldsInTestClassHierarchy(@Nonnull Class<?> testClass)
   {
      Class<?> superclass = testClass.getSuperclass();

      if (superclass.getClassLoader() != null) {
         findAllTestedAndInjectableFieldsInTestClassHierarchy(superclass);
      }

      for (Field field : testClass.getDeclaredFields()) {
         addAsTestedOrInjectableFieldIfApplicable(field);
      }
   }

   private void addAsTestedOrInjectableFieldIfApplicable(@Nonnull Field fieldFromTestClass)
   {
      for (Annotation fieldAnnotation : fieldFromTestClass.getDeclaredAnnotations()) {
         Tested testedMetadata = getTestedAnnotationIfPresent(fieldAnnotation);

         if (testedMetadata != null) {
            TestedField testedField = new TestedField(injectionState, fieldFromTestClass, testedMetadata);
            testedFields.add(testedField);
            break;
         }
         else if (fieldAnnotation instanceof Injectable) {
            MockedType mockedType = new MockedType(fieldFromTestClass);
            injectableFields.add(mockedType);
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

      for (TestedField testedField : testedFields) {
         if (!beforeSetup || testedField.isAvailableDuringSetup()) {
            try {
               testedField.instantiateWithInjectableValues(testClassInstance);
            }
            finally {
               injectionState.resetConsumedInjectables();
            }
         }
      }
   }

   public void clearTestedFields()
   {
      injectionState.lifecycleMethods.executeTerminationMethodsIfAny();

      for (TestedField testedField : testedFields) {
         testedField.clearIfAutomaticCreation();
      }
   }

   @Nonnull
   public BeanExporter getBeanExporter() { return injectionState; }
}
