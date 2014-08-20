/*
 * Copyright (c) 2006-2014 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.expectations.injection;

import java.lang.reflect.*;
import java.util.*;

import mockit.*;
import mockit.internal.expectations.mocking.*;

import org.jetbrains.annotations.*;

public final class TestedClassInstantiations
{
   @NotNull private final List<TestedField> testedFields;
   @NotNull private final List<MockedType> injectableFields;
   @NotNull private final InjectionState injectionState;

   public TestedClassInstantiations()
   {
      testedFields = new LinkedList<TestedField>();
      injectableFields = new ArrayList<MockedType>();
      injectionState = new InjectionState();
   }

   public boolean findTestedAndInjectableFields(@NotNull Class<?> testClass)
   {
      boolean foundTestedFields = findAllTestedAndInjectableFieldsInTestClassHierarchy(testClass);

      if (foundTestedFields) {
         new ParameterNameExtractor(true).extractNames(testClass);
      }

      return foundTestedFields;
   }

   private boolean findAllTestedAndInjectableFieldsInTestClassHierarchy(@NotNull Class<?> testClass)
   {
      Class<?> classWithFields = testClass;

      do {
         Field[] fields = classWithFields.getDeclaredFields();

         for (Field field : fields) {
            if (field.isAnnotationPresent(Injectable.class)) {
               MockedType mockedType = new MockedType(field, true);
               injectableFields.add(mockedType);
            }
            else {
               Tested testedMetadata = field.getAnnotation(Tested.class);

               if (testedMetadata != null) {
                  TestedField testedField = new TestedField(injectionState, field, testedMetadata);
                  testedFields.add(testedField);
               }
            }
         }

         classWithFields = classWithFields.getSuperclass();
      }
      while (classWithFields.getClassLoader() != null);

      return !testedFields.isEmpty();
   }

   public void assignNewInstancesToTestedFields(@NotNull Object testClassInstance)
   {
      injectionState.buildListsOfInjectables(testClassInstance, injectableFields);

      TestedField previousField = null;

      for (TestedField testedField : testedFields) {
         if (previousField != null && !testedField.isAtSameLevelInTestClassHierarchy(previousField)) {
            injectionState.discardInjectablesFromLowerTestClassHierarchyLevels(testedField.getDeclaringTestClass());
         }

         testedField.instantiateWithInjectableValues(testClassInstance);
         injectionState.resetConsumedInjectables();
         previousField = testedField;
      }
   }

   public void clearTestedFields()
   {
      for (TestedField testedField : testedFields) {
         testedField.clearIfAutomaticCreation();
      }
   }
}
