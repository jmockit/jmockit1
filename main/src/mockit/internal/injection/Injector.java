/*
 * Copyright (c) 2006 JMockit developers
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.injection;

import java.lang.reflect.*;
import java.util.*;
import java.util.regex.*;
import javax.annotation.*;
import javax.persistence.*;

import static java.lang.reflect.Modifier.*;
import static java.util.regex.Pattern.compile;

import mockit.internal.injection.field.*;
import mockit.internal.injection.full.*;
import mockit.internal.reflection.*;
import mockit.internal.util.*;
import static mockit.internal.injection.InjectionPoint.*;
import static mockit.internal.injection.InjectionProvider.NULL;

public class Injector
{
   private static final Pattern TYPE_NAME = compile("class |interface |java\\.lang\\.");

   @Nonnull protected final InjectionState injectionState;
   @Nullable protected final FullInjection fullInjection;

   protected Injector(@Nonnull InjectionState state, @Nullable FullInjection fullInjection) {
      injectionState = state;
      this.fullInjection = fullInjection;
   }

   @Nonnull
   public static List<Field> findAllTargetInstanceFieldsInTestedClassHierarchy(
      @Nonnull Class<?> actualTestedClass, @Nonnull TestedClass testedClass
   ) {
      List<Field> targetFields = new ArrayList<>();
      Class<?> classWithFields = actualTestedClass;

      do {
         addEligibleFields(targetFields, classWithFields);
         classWithFields = classWithFields.getSuperclass();
      }
      while (testedClass.isClassFromSameModuleOrSystemAsTestedClass(classWithFields) || isServlet(classWithFields));

      return targetFields;
   }

   private static void addEligibleFields(@Nonnull List<Field> targetFields, @Nonnull Class<?> classWithFields) {
      Field[] fields = classWithFields.getDeclaredFields();

      for (Field field : fields) {
         if (isEligibleForInjection(field)) {
            targetFields.add(field);
         }
      }
   }

   private static boolean isEligibleForInjection(@Nonnull Field field) {
      int modifiers = field.getModifiers();

      if (isFinal(modifiers)) {
         return false;
      }

      if (kindOfInjectionPoint(field) != KindOfInjectionPoint.NotAnnotated) {
         return true;
      }

      //noinspection SimplifiableIfStatement
      if (PERSISTENCE_UNIT_CLASS != null && field.getType().isAnnotationPresent(Entity.class)) {
         return false;
      }

      return !isStatic(modifiers) && !isVolatile(modifiers);
   }

   public final void fillOutDependenciesRecursively(@Nonnull Object dependency, @Nonnull TestedClass testedClass) {
      Class<?> dependencyClass = dependency.getClass();
      List<Field> targetFields = findAllTargetInstanceFieldsInTestedClassHierarchy(dependencyClass, testedClass);

      if (!targetFields.isEmpty()) {
         List<InjectionProvider> currentlyConsumedInjectables = injectionState.injectionProviders.saveConsumedInjectionProviders();
         injectIntoEligibleFields(targetFields, dependency, testedClass);
         injectionState.injectionProviders.restoreConsumedInjectionProviders(currentlyConsumedInjectables);
      }
   }

   public final void injectIntoEligibleFields(
      @Nonnull List<Field> targetFields, @Nonnull Object testedObject, @Nonnull TestedClass testedClass
   ) {
      for (Field field : targetFields) {
         if (targetFieldWasNotAssignedByConstructor(testedObject, field)) {
            Object injectableValue = getValueForFieldIfAvailable(targetFields, testedClass, field);

            if (injectableValue != null && injectableValue != NULL) {
               injectableValue = wrapInProviderIfNeeded(field.getGenericType(), injectableValue);
               FieldReflection.setFieldValue(field, testedObject, injectableValue);
            }
         }
      }
   }

   private static boolean targetFieldWasNotAssignedByConstructor(@Nonnull Object testedObject, @Nonnull Field targetField) {
      if (kindOfInjectionPoint(targetField) != KindOfInjectionPoint.NotAnnotated) {
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
   private Object getValueForFieldIfAvailable(
      @Nonnull List<Field> targetFields, @Nonnull TestedClass testedClass, @Nonnull Field targetField
   ) {
      @Nullable String qualifiedFieldName = getQualifiedName(targetField.getDeclaredAnnotations());
      InjectionProvider injectable = findAvailableInjectableIfAny(targetFields, qualifiedFieldName, testedClass, targetField);

      if (injectable != null) {
         return injectionState.getValueToInject(injectable);
      }

      InjectionProvider fieldToInject = new FieldToInject(targetField);
      Type typeToInject = fieldToInject.getDeclaredType();
      InjectionPoint injectionPoint = new InjectionPoint(typeToInject, fieldToInject.getName(), qualifiedFieldName);
      TestedClass nextTestedClass = typeToInject instanceof TypeVariable<?> ?
         testedClass : new TestedClass(typeToInject, fieldToInject.getClassOfDeclaredType(), testedClass);
      Object testedValue = injectionState.getTestedValue(nextTestedClass, injectionPoint);

      if (testedValue != null) {
         return testedValue;
      }

      if (fullInjection != null) {
         Object newInstance = fullInjection.createOrReuseInstance(nextTestedClass, this, fieldToInject, qualifiedFieldName);

         if (newInstance != null) {
            return newInstance;
         }
      }

      KindOfInjectionPoint kindOfInjectionPoint = kindOfInjectionPoint(targetField);
      throwExceptionIfUnableToInjectRequiredTargetField(kindOfInjectionPoint, targetField);
      return null;
   }

   @Nullable
   private InjectionProvider findAvailableInjectableIfAny(
      @Nonnull List<Field> targetFields, @Nullable String qualifiedTargetFieldName, @Nonnull TestedClass testedClass,
      @Nonnull Field targetField
   ) {
      KindOfInjectionPoint kindOfInjectionPoint = kindOfInjectionPoint(targetField);
      InjectionProviders injectionProviders = injectionState.injectionProviders;
      injectionProviders.setTypeOfInjectionPoint(targetField.getGenericType(), kindOfInjectionPoint);

      if (qualifiedTargetFieldName != null && !qualifiedTargetFieldName.isEmpty()) {
         String injectableName = convertToLegalJavaIdentifierIfNeeded(qualifiedTargetFieldName);
         InjectionProvider matchingInjectable = injectionProviders.findInjectableByTypeAndName(injectableName, testedClass);

         if (matchingInjectable != null) {
            return matchingInjectable;
         }
      }

      String targetFieldName = targetField.getName();

      if (withMultipleTargetFieldsOfSameType(targetFields, testedClass, targetField, injectionProviders)) {
         return injectionProviders.findInjectableByTypeAndName(targetFieldName, testedClass);
      }

      return injectionProviders.getProviderByTypeAndOptionallyName(targetFieldName, testedClass);
   }

   private static boolean withMultipleTargetFieldsOfSameType(
      @Nonnull List<Field> targetFields, @Nonnull TestedClass testedClass, @Nonnull Field targetField,
      @Nonnull InjectionProviders injectionProviders
   ) {
      for (Field anotherTargetField : targetFields) {
         if (
            anotherTargetField != targetField &&
            injectionProviders.isAssignableToInjectionPoint(anotherTargetField.getGenericType(), testedClass)
         ) {
            return true;
         }
      }

      return false;
   }

   private void throwExceptionIfUnableToInjectRequiredTargetField(
      @Nonnull KindOfInjectionPoint kindOfInjectionPoint, @Nonnull Field targetField
   ) {
      if (kindOfInjectionPoint == KindOfInjectionPoint.Required) {
         Type fieldType = targetField.getGenericType();
         String fieldTypeName = fieldType.toString();
         fieldTypeName = TYPE_NAME.matcher(fieldTypeName).replaceAll("");
         String kindOfInjectable = "@Injectable";

         if (fullInjection != null) {
            if (targetField.getType().isInterface()) {
               kindOfInjectable = "@Tested instance of an implementation class";
            }
            else {
               kindOfInjectable = "@Tested object";
            }
         }

         throw new IllegalStateException(
            "Missing " + kindOfInjectable + " for field \"" + fieldTypeName + ' ' + targetField.getName() + "\" in " +
            targetField.getDeclaringClass().getSimpleName());
      }
   }
}