/*
 * Copyright (c) 2006-2015 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.expectations.injection;

import java.lang.reflect.*;
import java.security.*;
import java.util.*;
import static java.lang.reflect.Modifier.*;

import mockit.internal.expectations.mocking.*;
import mockit.internal.util.*;
import static mockit.internal.expectations.injection.InjectionPoint.*;

import org.jetbrains.annotations.*;

final class FieldInjection
{
   @NotNull private final TestedField testedField;
   @Nullable private final ProtectionDomain protectionDomainOfTestedClass;
   @NotNull private final String nameOfTestedClass;
   @Nullable final FullInjection fullInjection;

   FieldInjection(@NotNull TestedField testedField, @NotNull Class<?> testedClass, boolean fullInjection)
   {
      this.testedField = testedField;
      protectionDomainOfTestedClass = testedClass.getProtectionDomain();
      nameOfTestedClass = testedClass.getName();
      this.fullInjection = fullInjection ? new FullInjection(testedField.injectionState) : null;
   }

   @NotNull
   List<Field> findAllTargetInstanceFieldsInTestedClassHierarchy(@NotNull Class<?> testedClass)
   {
      testedField.requireDIAnnotation = false;

      List<Field> targetFields = new ArrayList<Field>();
      Class<?> classWithFields = testedClass;

      do {
         Field[] fields = classWithFields.getDeclaredFields();

         for (Field field : fields) {
            if (isEligibleForInjection(field)) {
               targetFields.add(field);
            }
         }

         classWithFields = classWithFields.getSuperclass();
      }
      while (isClassFromSameModuleOrSystemAsTestedClass(classWithFields) || isServlet(classWithFields));

      return targetFields;
   }

   private boolean isEligibleForInjection(@NotNull Field field)
   {
      int modifiers = field.getModifiers();

      if (isFinal(modifiers)) {
         return false;
      }

      boolean annotated = isAnnotated(field) != KindOfInjectionPoint.NotAnnotated;

      if (annotated) {
         testedField.requireDIAnnotation = true;
         return true;
      }

      return !isStatic(modifiers);
   }

   boolean isClassFromSameModuleOrSystemAsTestedClass(@NotNull Class<?> anotherClass)
   {
      if (anotherClass.getClassLoader() == null) {
         return false;
      }

      if (anotherClass.getProtectionDomain() == protectionDomainOfTestedClass) {
         return true;
      }

      String nameOfAnotherClass = anotherClass.getName();
      int p1 = nameOfAnotherClass.indexOf('.');
      int p2 = nameOfTestedClass.indexOf('.');

      if (p1 != p2 || p1 == -1) {
         return false;
      }

      p1 = nameOfAnotherClass.indexOf('.', p1 + 1);
      p2 = nameOfTestedClass.indexOf('.', p2 + 1);

      return p1 == p2 && p1 > 0 && nameOfAnotherClass.substring(0, p1).equals(nameOfTestedClass.substring(0, p2));
   }

   void injectIntoEligibleFields(@NotNull List<Field> targetFields, @NotNull Object testedObject)
   {
      for (Field field : targetFields) {
         if (notAssignedByConstructor(field, testedObject)) {
            Object injectableValue = getValueForFieldIfAvailable(targetFields, field);

            if (injectableValue != null) {
               injectableValue = wrapInProviderIfNeeded(field.getGenericType(), injectableValue);
               FieldReflection.setFieldValue(field, testedObject, injectableValue);
            }
         }
      }
   }

   private static boolean notAssignedByConstructor(@NotNull Field field, @NotNull Object testedObject)
   {
      if (isAnnotated(field) != KindOfInjectionPoint.NotAnnotated) {
         return true;
      }

      Object fieldValue = FieldReflection.getFieldValue(field, testedObject);

      if (fieldValue == null) {
         return true;
      }

      Class<?> fieldType = field.getType();

      if (!fieldType.isPrimitive()) {
         return false;
      }

      Object defaultValue = DefaultValues.defaultValueForPrimitiveType(fieldType);

      return fieldValue.equals(defaultValue);
   }

   @Nullable
   private Object getValueForFieldIfAvailable(@NotNull List<Field> targetFields, @NotNull Field fieldToBeInjected)
   {
      InjectionState injectionState = testedField.injectionState;
      injectionState.setTypeOfInjectionPoint(fieldToBeInjected.getGenericType());

      String targetFieldName = fieldToBeInjected.getName();
      MockedType mockedType;

      if (withMultipleTargetFieldsOfSameType(targetFields, fieldToBeInjected)) {
         mockedType = injectionState.findInjectableByTypeAndName(targetFieldName);
      }
      else {
         mockedType = injectionState.findInjectableByTypeAndOptionallyName(targetFieldName);
      }

      if (mockedType != null) {
         return injectionState.getValueToInject(mockedType);
      }

      KindOfInjectionPoint kindOfInjectionPoint = isAnnotated(fieldToBeInjected);

      if (fullInjection != null) {
         if (testedField.requireDIAnnotation && kindOfInjectionPoint == KindOfInjectionPoint.NotAnnotated) {
            return null;
         }

         return fullInjection.newInstanceCreatedWithNoArgsConstructorIfAvailable(this, fieldToBeInjected);
      }

      if (kindOfInjectionPoint == KindOfInjectionPoint.WithValue) {
         return getValueFromAnnotation(fieldToBeInjected);
      }

      if (kindOfInjectionPoint == KindOfInjectionPoint.Required) {
         String fieldType = fieldToBeInjected.getGenericType().toString();
         throw new IllegalStateException(
            "Missing @Injectable for field " +
            fieldToBeInjected.getDeclaringClass().getSimpleName() + '#' + fieldToBeInjected.getName() + ", of type " +
            fieldType.replace("class ", "").replace("interface ", "").replace("java.lang.", ""));
      }

      return null;
   }

   private boolean withMultipleTargetFieldsOfSameType(
      @NotNull List<Field> targetFields, @NotNull Field fieldToBeInjected)
   {
      InjectionState injectionState = testedField.injectionState;

      for (Field targetField : targetFields) {
         if (
            targetField != fieldToBeInjected &&
            injectionState.isSameTypeAsInjectionPoint(targetField.getGenericType())
         ) {
            return true;
         }
      }

      return false;
   }

   void fillOutDependenciesRecursively(@NotNull Object dependency)
   {
      Class<?> dependencyClass = dependency.getClass();
      List<Field> targetFields = findAllTargetInstanceFieldsInTestedClassHierarchy(dependencyClass);

      if (!targetFields.isEmpty()) {
         InjectionState injectionState = testedField.injectionState;
         List<MockedType> currentlyConsumedInjectables = injectionState.saveConsumedInjectables();
         injectIntoEligibleFields(targetFields, dependency);
         injectionState.restoreConsumedInjectables(currentlyConsumedInjectables);
      }
   }
}
