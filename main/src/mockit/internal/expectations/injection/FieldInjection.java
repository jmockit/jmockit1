/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.expectations.injection;

import java.lang.reflect.*;
import java.util.*;
import java.util.regex.*;
import javax.annotation.*;
import static java.lang.reflect.Modifier.*;
import static java.util.regex.Pattern.*;

import mockit.internal.expectations.mocking.*;
import mockit.internal.util.*;
import static mockit.internal.expectations.injection.InjectionPoint.*;

final class FieldInjection implements Injector
{
   private static final Pattern TYPE_NAME = compile("class |interface |java\\.lang\\.");

   @Nonnull private final TestedClass testedClass;
   @Nonnull private final InjectionState injectionState;
   boolean requireDIAnnotation;
   @Nullable private final FullInjection fullInjection;
   @Nonnull Class<?> targetClass;
   Field targetField;

   FieldInjection(
      @Nonnull TestedField testedField, @Nonnull TestedClass testedClass, @Nullable FullInjection fullInjection)
   {
      injectionState = testedField.injectionState;
      requireDIAnnotation = testedField.requireDIAnnotation;
      this.testedClass = testedClass;
      this.fullInjection = fullInjection;
      targetClass = testedClass.targetClass;
   }

   @Nonnull
   List<Field> findAllTargetInstanceFieldsInTestedClassHierarchy(@Nonnull Class<?> actualTestedClass)
   {
      requireDIAnnotation = false;

      List<Field> targetFields = new ArrayList<Field>();
      Class<?> classWithFields = actualTestedClass;

      do {
         Field[] fields = classWithFields.getDeclaredFields();

         for (Field field : fields) {
            if (isEligibleForInjection(field)) {
               targetFields.add(field);
            }
         }

         classWithFields = classWithFields.getSuperclass();
      }
      while (testedClass.isClassFromSameModuleOrSystemAsTestedClass(classWithFields) || isServlet(classWithFields));

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

   void injectIntoEligibleFields(@Nonnull List<Field> targetFields, @Nonnull Object testedObject)
   {
      targetClass = testedObject.getClass();

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
      String qualifiedTargetFieldName = getQualifiedName(targetField.getDeclaredAnnotations());
      MockedType mockedType = findAvailableInjectableIfAny(targetFields, qualifiedTargetFieldName);

      if (mockedType != null) {
         return injectionState.getValueToInject(mockedType);
      }

      KindOfInjectionPoint kindOfInjectionPoint = isAnnotated(targetField);

      if (fullInjection != null) {
         if (requireDIAnnotation && kindOfInjectionPoint == KindOfInjectionPoint.NotAnnotated) {
            return null;
         }

         FieldToInject fieldToInject = new FieldToInject(targetField);
         Object newInstance = fullInjection.newInstance(testedClass, this, fieldToInject, qualifiedTargetFieldName);

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

   @Nullable
   private MockedType findAvailableInjectableIfAny(
      @Nonnull List<Field> targetFields, @Nullable String qualifiedTargetFieldName)
   {
      injectionState.setTypeOfInjectionPoint(targetField.getGenericType());

      if (qualifiedTargetFieldName != null && !qualifiedTargetFieldName.isEmpty()) {
         return injectionState.findInjectableByTypeAndName(qualifiedTargetFieldName);
      }

      String targetFieldName = targetField.getName();

      return withMultipleTargetFieldsOfSameType(targetFields) ?
         injectionState.findInjectableByTypeAndName(targetFieldName) :
         injectionState.findInjectableByTypeAndOptionallyName(targetFieldName);
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

   @Override
   public void fillOutDependenciesRecursively(@Nonnull Object dependency)
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
