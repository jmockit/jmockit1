/*
 * Copyright (c) 2006-2015 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.expectations.injection;

import java.io.*;
import java.lang.reflect.*;
import java.security.*;
import java.util.*;
import java.util.regex.*;
import javax.annotation.*;
import static java.lang.reflect.Modifier.*;
import static java.util.regex.Pattern.compile;

import mockit.internal.expectations.mocking.*;
import mockit.internal.util.*;
import static mockit.internal.expectations.injection.InjectionPoint.*;

final class FieldInjection
{
   private static final Pattern TYPE_NAME = compile("class |interface |java\\.lang\\.");

   @Nonnull private final InjectionState injectionState;
   boolean requireDIAnnotation;
   @Nonnull private final ProtectionDomain protectionDomainOfTestedClass;
   @Nullable private final String codeLocationParentPath;
   @Nonnull final String nameOfTestedClass;
   @Nullable private final FullInjection fullInjection;
   private Field targetField;

   FieldInjection(@Nonnull TestedField testedField, @Nonnull Class<?> testedClass, boolean fullInjection)
   {
      injectionState = testedField.injectionState;
      requireDIAnnotation = testedField.requireDIAnnotation;
      protectionDomainOfTestedClass = testedClass.getProtectionDomain();
      CodeSource codeSource = protectionDomainOfTestedClass.getCodeSource();
      codeLocationParentPath = codeSource == null ? null : new File(codeSource.getLocation().getPath()).getParent();
      nameOfTestedClass = testedClass.getName();
      this.fullInjection = fullInjection ? new FullInjection(injectionState) : null;
   }

   @Nonnull
   List<Field> findAllTargetInstanceFieldsInTestedClassHierarchy(@Nonnull Class<?> testedClass)
   {
      requireDIAnnotation = false;

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

   private boolean isEligibleForInjection(@Nonnull Field field)
   {
      int modifiers = field.getModifiers();

      if (isFinal(modifiers)) {
         return false;
      }

      boolean annotated = isAnnotated(field) != KindOfInjectionPoint.NotAnnotated;

      if (annotated) {
         requireDIAnnotation = true;
         return true;
      }

      return !isStatic(modifiers);
   }

   boolean isClassFromSameModuleOrSystemAsTestedClass(@Nonnull Class<?> anotherClass)
   {
      if (anotherClass.getClassLoader() == null) {
         return false;
      }

      ProtectionDomain anotherProtectionDomain = anotherClass.getProtectionDomain();

      if (anotherProtectionDomain == null) {
         return false;
      }

      if (anotherProtectionDomain == protectionDomainOfTestedClass) {
         return true;
      }

      CodeSource anotherCodeSource = anotherProtectionDomain.getCodeSource();

      if (anotherCodeSource == null || anotherCodeSource.getLocation() == null) {
         return false;
      }

      if (codeLocationParentPath != null) {
         String anotherClassPath = anotherCodeSource.getLocation().getPath();
         String anotherClassParentPath = new File(anotherClassPath).getParent();

         if (anotherClassParentPath.equals(codeLocationParentPath)) {
            return true;
         }
      }

      return isInSameSubpackageAsTestedClass(anotherClass);
   }

   private boolean isInSameSubpackageAsTestedClass(@Nonnull Class<?> anotherClass)
   {
      String nameOfAnotherClass = anotherClass.getName();
      int p1 = nameOfAnotherClass.indexOf('.');
      int p2 = nameOfTestedClass.indexOf('.');
      boolean differentPackages = p1 != p2 || p1 == -1;

      if (differentPackages) {
         return false;
      }

      p1 = nameOfAnotherClass.indexOf('.', p1 + 1);
      p2 = nameOfTestedClass.indexOf('.', p2 + 1);
      boolean eitherClassDirectlyInFirstPackageLevel = p1 == -1 || p2 == -1;

      if (eitherClassDirectlyInFirstPackageLevel) {
         return true;
      }

      boolean differentSubpackages = p1 != p2;

      if (differentSubpackages) {
         return false;
      }

      return nameOfAnotherClass.substring(0, p1).equals(nameOfTestedClass.substring(0, p2));
   }

   void injectIntoEligibleFields(@Nonnull List<Field> targetFields, @Nonnull Object testedObject)
   {
      for (Field field : targetFields) {
         targetField = field;

         if (targetFieldWasNotAssignedByConstructor(testedObject)) {
            Object injectableValue = getValueForFieldIfAvailable(targetFields);

            if (injectableValue != null) {
               injectableValue = wrapInProviderIfNeeded(field.getGenericType(), injectableValue);
               FieldReflection.setFieldValue(field, testedObject, injectableValue);
            }
         }
      }
   }

   private boolean targetFieldWasNotAssignedByConstructor(@Nonnull Object testedObject)
   {
      if (isAnnotated(targetField) != KindOfInjectionPoint.NotAnnotated) {
         return true;
      }

      Object fieldValue = FieldReflection.getFieldValue(targetField, testedObject);

      if (fieldValue == null) {
         return true;
      }

      Class<?> fieldType = targetField.getType();

      if (!fieldType.isPrimitive()) {
         return false;
      }

      Object defaultValue = DefaultValues.defaultValueForPrimitiveType(fieldType);

      return fieldValue.equals(defaultValue);
   }

   @Nullable
   private Object getValueForFieldIfAvailable(@Nonnull List<Field> targetFields)
   {
      injectionState.setTypeOfInjectionPoint(targetField.getGenericType());

      String targetFieldName = targetField.getName();
      MockedType mockedType;

      if (withMultipleTargetFieldsOfSameType(targetFields)) {
         mockedType = injectionState.findInjectableByTypeAndName(targetFieldName);
      }
      else {
         mockedType = injectionState.findInjectableByTypeAndOptionallyName(targetFieldName);
      }

      if (mockedType != null) {
         return injectionState.getValueToInject(mockedType);
      }

      KindOfInjectionPoint kindOfInjectionPoint = isAnnotated(targetField);

      if (fullInjection != null) {
         if (requireDIAnnotation && kindOfInjectionPoint == KindOfInjectionPoint.NotAnnotated) {
            return null;
         }

         Object newInstance = fullInjection.newInstance(this, targetField);

         if (newInstance != null) {
            return newInstance;
         }
      }

      if (kindOfInjectionPoint == KindOfInjectionPoint.WithValue) {
         return getValueFromAnnotation(targetField);
      }

      throwExceptionIfUnableToInjectRequiredTargetField(kindOfInjectionPoint);
      return null;
   }

   private boolean withMultipleTargetFieldsOfSameType(@Nonnull List<Field> targetFields)
   {
      for (Field anotherTargetField : targetFields) {
         if (
            anotherTargetField != targetField &&
            injectionState.isSameTypeAsInjectionPoint(anotherTargetField.getGenericType())
         ) {
            return true;
         }
      }

      return false;
   }

   private void throwExceptionIfUnableToInjectRequiredTargetField(@Nonnull KindOfInjectionPoint kindOfInjectionPoint)
   {
      if (kindOfInjectionPoint == KindOfInjectionPoint.Required) {
         String fieldType = targetField.getGenericType().toString();
         fieldType = TYPE_NAME.matcher(fieldType).replaceAll("");
         String kindOfInjectable = fullInjection == null ? "@Injectable" : "instantiable class";

         throw new IllegalStateException(
            "Missing " + kindOfInjectable + " for field " + targetField.getDeclaringClass().getSimpleName() +
            '#' + targetField.getName() + ", of type " + fieldType);
      }
   }

   void fillOutDependenciesRecursively(@Nonnull Object dependency)
   {
      Class<?> dependencyClass = dependency.getClass();
      boolean previousRequireDIAnnotation = requireDIAnnotation;
      List<Field> targetFields = findAllTargetInstanceFieldsInTestedClassHierarchy(dependencyClass);

      if (!targetFields.isEmpty()) {
         List<MockedType> currentlyConsumedInjectables = injectionState.saveConsumedInjectables();

         injectIntoEligibleFields(targetFields, dependency);

         injectionState.restoreConsumedInjectables(currentlyConsumedInjectables);
      }

      requireDIAnnotation = previousRequireDIAnnotation;
   }
}
