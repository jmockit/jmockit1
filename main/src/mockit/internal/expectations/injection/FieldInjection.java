/*
 * Copyright (c) 2006-2014 Rogério Liesenfeld
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
   @NotNull private final InjectionState injectionState;
   @Nullable private final ProtectionDomain protectionDomainOfTestedClass;
   @NotNull private final String nameOfTestedClass;
   private final boolean requiresAnnotation;
   @Nullable final FullInjection fullInjection;
   private boolean foundAnnotations;

   FieldInjection(
      @NotNull InjectionState injectionState, @NotNull Class<?> testedClass,
      boolean requiresAnnotation, boolean fullInjection)
   {
      this.injectionState = injectionState;
      protectionDomainOfTestedClass = testedClass.getProtectionDomain();
      nameOfTestedClass = testedClass.getName();
      this.requiresAnnotation = requiresAnnotation;
      this.fullInjection = fullInjection ? new FullInjection(injectionState) : null;
   }

   @NotNull
   List<Field> findAllTargetInstanceFieldsInTestedClassHierarchy(@NotNull Class<?> testedClass)
   {
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
      while (
         isClassFromSameModuleOrSystemAsTestedClass(classWithFields) ||
         isExternalBaseClassSupportingInjection(classWithFields)
      );

      discardFieldsNotAnnotatedIfAtLeastOneIsAnnotated(targetFields);

      return targetFields;
   }

   private boolean isEligibleForInjection(@NotNull Field field)
   {
      if (isFinal(field.getModifiers())) {
         return false;
      }

      if (requiresAnnotation || foundAnnotations) {
         return isAnnotated(field);
      }

      if (WITH_INJECTION_API_IN_CLASSPATH) {
         foundAnnotations = isAnnotated(field);
      }

      return foundAnnotations || !isStatic(field.getModifiers());
   }

   private void discardFieldsNotAnnotatedIfAtLeastOneIsAnnotated(@NotNull List<Field> targetFields)
   {
      if (!requiresAnnotation && foundAnnotations) {
         ListIterator<Field> itr = targetFields.listIterator();

         while (itr.hasNext()) {
            Field targetField = itr.next();

            if (!isAnnotated(targetField)) {
               itr.remove();
            }
         }
      }
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

   private static boolean isExternalBaseClassSupportingInjection(@NotNull Class<?> anotherClass)
   {
      return SERVLET_CLASS != null && SERVLET_CLASS.isAssignableFrom(anotherClass);
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
      if (WITH_INJECTION_API_IN_CLASSPATH && isAnnotated(field)) {
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

      if (fullInjection != null) {
         return fullInjection.newInstanceCreatedWithNoArgsConstructorIfAvailable(this, fieldToBeInjected);
      }

      return null;
   }

   private boolean withMultipleTargetFieldsOfSameType(
      @NotNull List<Field> targetFields, @NotNull Field fieldToBeInjected)
   {
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
         List<MockedType> currentlyConsumedInjectables = injectionState.saveConsumedInjectables();
         injectIntoEligibleFields(targetFields, dependency);
         injectionState.restoreConsumedInjectables(currentlyConsumedInjectables);
      }
   }
}
