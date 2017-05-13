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
import mockit.internal.util.*;
import static mockit.external.asm.Opcodes.*;

public final class TestedClassInstantiations
{
   private static final int FIELD_ACCESS_MASK = ACC_SYNTHETIC + ACC_STATIC;

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

      Field[] fieldsFromTestClass = testClass.getDeclaredFields();

      for (Field candidateField : fieldsFromTestClass) {
         int fieldModifiers = candidateField.getModifiers();

         if ((fieldModifiers & FIELD_ACCESS_MASK) == 0) {
            addAsTestedOrInjectableFieldIfApplicable(candidateField);
         }
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
   private static Tested getTestedAnnotationIfPresent(@Nonnull Annotation annotation)
   {
      if (annotation instanceof Tested) {
         return (Tested) annotation;
      }

      return annotation.annotationType().getAnnotation(Tested.class);
   }

   public void createTestedParameters(@Nonnull Object testClassInstance, @Nonnull TestMethod testMethod)
   {
      int n = testMethod.getParameterCount();

      for (int i = 0; i < n; i++) {
         TestedParameter testedParameter = createTestedParameterIfApplicable(testMethod, i);

         if (testedParameter != null) {
            instantiateTestedObject(testClassInstance, testedParameter);
         }
      }
   }

   @Nullable
   private TestedParameter createTestedParameterIfApplicable(@Nonnull TestMethod testMethod, @Nonnegative int i)
   {
      Annotation[] parameterAnnotations = testMethod.getParameterAnnotations(i);

      for (Annotation parameterAnnotation : parameterAnnotations) {
         Tested testedMetadata = getTestedAnnotationIfPresent(parameterAnnotation);

         if (testedMetadata != null) {
            return new TestedParameter(injectionState, testMethod, i, testedMetadata);
         }
      }

      return null;
   }

   private void instantiateTestedObject(@Nonnull Object testClassInstance, @Nonnull TestedObject testedObject)
   {
      try {
         testedObject.instantiateWithInjectableValues(testClassInstance);
      }
      finally {
         injectionState.resetConsumedInjectables();
      }
   }

   public void assignNewInstancesToTestedFields(@Nonnull Object testClassInstance, boolean beforeSetup)
   {
      injectionState.buildListsOfInjectables(testClassInstance, injectableFields);

      for (TestedObject testedField : testedFields) {
         if (!beforeSetup || testedField.isAvailableDuringSetup()) {
            instantiateTestedObject(testClassInstance, testedField);
         }
      }
   }

   public void clearTestedObjects()
   {
      injectionState.lifecycleMethods.executeTerminationMethodsIfAny();
      injectionState.clearTestedObjectsAndInstantiatedDependencies();

      Object testClassInstance = injectionState.getCurrentTestClassInstance();

      for (TestedObject testedField : testedFields) {
         testedField.clearIfAutomaticCreation(testClassInstance);
      }
   }

   @Nonnull
   public BeanExporter getBeanExporter() { return injectionState; }
}
