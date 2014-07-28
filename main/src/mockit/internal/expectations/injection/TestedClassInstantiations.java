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
      Field[] fieldsInTestClass = testClass.getDeclaredFields();

      for (Field field : fieldsInTestClass) {
         Tested testedMetadata = field.getAnnotation(Tested.class);

         if (testedMetadata != null) {
            TestedField testedField = new TestedField(injectionState, field, testedMetadata);
            testedFields.add(testedField);
         }
         else {
            MockedType mockedType = new MockedType(field, true);

            if (mockedType.injectable) {
               injectableFields.add(mockedType);
            }
         }
      }

      boolean foundTestedFields = !testedFields.isEmpty();

      if (foundTestedFields) {
         new ParameterNameExtractor(true).extractNames(testClass);
      }

      return foundTestedFields;
   }

   public void assignNewInstancesToTestedFields(@NotNull Object testClassInstance)
   {
      injectionState.buildListsOfInjectables(testClassInstance, injectableFields);

      for (TestedField testedField : testedFields) {
         testedField.instantiateWithInjectableValues(testClassInstance);
         injectionState.resetConsumedInjectables();
      }
   }

   public void clearTestedFields()
   {
      for (TestedField testedField : testedFields) {
         testedField.clearIfAutomaticCreation();
      }
   }
}
