/*
 * Copyright (c) 2006-2014 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.expectations.injection;

import java.lang.annotation.*;
import java.lang.reflect.*;
import java.lang.reflect.Type;
import java.util.*;
import javax.inject.*;
import static java.lang.reflect.Modifier.*;

import mockit.*;
import mockit.external.asm4.*;
import mockit.internal.*;
import mockit.internal.expectations.mocking.*;
import mockit.internal.state.*;
import mockit.internal.util.*;
import static mockit.external.asm4.ClassReader.*;
import static mockit.internal.util.Utilities.*;

import org.jetbrains.annotations.*;

public final class TestedClassInstantiations
{
   @Nullable private static final Class<? extends Annotation> INJECT_CLASS;

   static
   {
      Class<? extends Annotation> injectClass;
      ClassLoader cl = TestedClassInstantiations.class.getClassLoader();

      try {
         //noinspection unchecked
         injectClass = (Class<? extends Annotation>) Class.forName("javax.inject.Inject", false, cl);
      }
      catch (ClassNotFoundException ignore) { injectClass = null; }

      INJECT_CLASS = injectClass;
   }

   @NotNull private final List<TestedField> testedFields;
   @NotNull private final List<MockedType> injectableFields;
   @NotNull private List<MockedType> injectables;
   @NotNull private final List<MockedType> consumedInjectables;
   private GenericTypeReflection testedTypeReflection;
   private Object currentTestClassInstance;
   private Type typeOfInjectionPoint;

   private final class TestedField
   {
      @NotNull final Field testedField;
      @Nullable private TestedObjectCreation testedObjectCreation;
      @Nullable private List<Field> targetFields;
      private boolean createAutomatically;

      TestedField(@NotNull Field field) { testedField = field; }

      void instantiateWithInjectableValues()
      {
         Object testedObject = null;

         if (!createAutomatically) {
            testedObject = FieldReflection.getFieldValue(testedField, currentTestClassInstance);
            createAutomatically = testedObject == null && !isFinal(testedField.getModifiers());
         }

         testedTypeReflection = new GenericTypeReflection(testedField);

         boolean requiresJavaxInject = false;
         Class<?> testedClass;

         if (createAutomatically) {
            if (testedObjectCreation == null) {
               testedObjectCreation = new TestedObjectCreation(testedField);
            }

            testedClass = testedObjectCreation.declaredTestedClass;
            testedObject = testedObjectCreation.create();
            FieldReflection.setFieldValue(testedField, currentTestClassInstance, testedObject);

            requiresJavaxInject = testedObjectCreation.constructorAnnotatedWithJavaxInject;
         }
         else {
            testedClass = testedObject == null ? null : testedObject.getClass();
         }

         if (testedObject != null) {
            FieldInjection fieldInjection = new FieldInjection(testedClass, testedObject, requiresJavaxInject);

            if (targetFields == null) {
               targetFields = fieldInjection.findAllTargetInstanceFieldsInTestedClassHierarchy();
            }

            fieldInjection.injectIntoEligibleFields(targetFields);
         }
      }

      void clearIfAutomaticCreation()
      {
         if (createAutomatically) {
            FieldReflection.setFieldValue(testedField, currentTestClassInstance, null);
         }
      }
   }

   public TestedClassInstantiations()
   {
      testedFields = new LinkedList<TestedField>();
      injectableFields = new ArrayList<MockedType>();
      injectables = Collections.emptyList();
      consumedInjectables = new ArrayList<MockedType>();
   }

   public boolean findTestedAndInjectableFields(@NotNull Class<?> testClass)
   {
      Field[] fieldsInTestClass = testClass.getDeclaredFields();

      for (Field field : fieldsInTestClass) {
         if (field.isAnnotationPresent(Tested.class)) {
            testedFields.add(new TestedField(field));
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
      currentTestClassInstance = testClassInstance;

      buildListsOfInjectables();

      for (TestedField testedField : testedFields) {
         testedField.instantiateWithInjectableValues();
         consumedInjectables.clear();
      }
   }

   public void clearTestedFields()
   {
      for (TestedField testedField : testedFields) {
         testedField.clearIfAutomaticCreation();
      }
   }

   private void buildListsOfInjectables()
   {
      ParameterTypeRedefinitions paramTypeRedefs = TestRun.getExecutingTest().getParameterTypeRedefinitions();

      if (paramTypeRedefs == null) {
         injectables = injectableFields;
      }
      else {
         injectables = new ArrayList<MockedType>(injectableFields);
         injectables.addAll(paramTypeRedefs.getInjectableParameters());
      }
   }

   void setTypeOfInjectionPoint(@NotNull Type parameterOrFieldType) { typeOfInjectionPoint = parameterOrFieldType; }

   boolean hasSameTypeAsInjectionPoint(@NotNull MockedType injectable)
   {
      return isSameTypeAsInjectionPoint(injectable.declaredType);
   }

   boolean isSameTypeAsInjectionPoint(Type injectableType)
   {
      if (testedTypeReflection.areMatchingTypes(typeOfInjectionPoint, injectableType)) {
         return true;
      }

      if (INJECT_CLASS != null && typeOfInjectionPoint instanceof ParameterizedType) {
         ParameterizedType parameterizedType = (ParameterizedType) typeOfInjectionPoint;

         if (parameterizedType.getRawType() == Provider.class) {
            Type providedType = parameterizedType.getActualTypeArguments()[0];
            return providedType.equals(injectableType);
         }
      }

      return false;
   }

   @Nullable
   private Object getValueToInject(@NotNull MockedType injectable)
   {
      if (consumedInjectables.contains(injectable)) {
         return null;
      }

      Object value = injectable.getValueToInject(currentTestClassInstance);

      if (value != null) {
         consumedInjectables.add(injectable);
      }

      return value;
   }

   private static Object wrapInProviderIfNeeded(Type type, final Object value)
   {
      if (
         INJECT_CLASS != null && type instanceof ParameterizedType && !(value instanceof Provider) &&
         ((ParameterizedType) type).getRawType() == Provider.class
      ) {
         return new Provider<Object>() { @Override public Object get() { return value; } };
      }

      return value;
   }

   private final class TestedObjectCreation
   {
      @NotNull private final Class<?> declaredTestedClass;
      @NotNull private final Class<?> actualTestedClass;
      private Constructor<?> constructor;
      private List<MockedType> injectablesForConstructor;
      private Type[] parameterTypes;
      boolean constructorAnnotatedWithJavaxInject;

      TestedObjectCreation(@NotNull Field testedField)
      {
         declaredTestedClass = testedField.getType();
         actualTestedClass =
            isAbstract(declaredTestedClass.getModifiers()) ?
               generateSubclass(testedField.getGenericType()) : declaredTestedClass;
      }

      @NotNull private Class<?> generateSubclass(@NotNull Type testedType)
      {
         ClassReader classReader = ClassFile.createReaderOrGetFromCache(declaredTestedClass);
         String subclassName = GeneratedClasses.getNameForGeneratedClass(declaredTestedClass);

         ClassVisitor modifier = new SubclassGenerationModifier(testedType, classReader, subclassName);
         classReader.accept(modifier, SKIP_FRAMES);
         byte[] bytecode = modifier.toByteArray();

         return ImplementationClass.defineNewClass(declaredTestedClass.getClassLoader(), bytecode, subclassName);
      }

      @NotNull Object create()
      {
         new ConstructorSearch().findConstructorAccordingToAccessibilityAndAvailableInjectables();

         if (constructor == null) {
            throw new IllegalArgumentException(
               "No constructor in " + declaredTestedClass + " that can be satisfied by available injectables");
         }

         return new ConstructorInjection().instantiate();
      }

      @Nullable MockedType findNextInjectableForVarargsParameter()
      {
         for (MockedType injectable : injectables) {
            if (hasSameTypeAsInjectionPoint(injectable) && !consumedInjectables.contains(injectable)) {
               return injectable;
            }
         }

         return null;
      }

      private final class ConstructorSearch
      {
         @NotNull private final String testedClassDesc;

         ConstructorSearch()
         {
            testedClassDesc = new ParameterNameExtractor(false).extractNames(declaredTestedClass);
            injectablesForConstructor = new ArrayList<MockedType>();
         }

         void findConstructorAccordingToAccessibilityAndAvailableInjectables()
         {
            constructor = null;

            Constructor<?>[] constructors = actualTestedClass.getDeclaredConstructors();

            if (INJECT_CLASS != null && findSingleInjectAnnotatedConstructor(constructors)) {
               constructorAnnotatedWithJavaxInject = true;
            }
            else {
               findSatisfiedConstructorWithMostParameters(constructors);
            }
         }

         private boolean findSingleInjectAnnotatedConstructor(@NotNull Constructor<?>[] constructors)
         {
            for (Constructor<?> c : constructors) {
               if (c.isAnnotationPresent(INJECT_CLASS)) {
                  List<MockedType> injectablesFound = findAvailableInjectablesForConstructor(c);

                  if (injectablesFound != null) {
                     injectablesForConstructor = injectablesFound;
                     constructor = c;
                  }

                  return true;
               }
            }

            return false;
         }

         private void findSatisfiedConstructorWithMostParameters(@NotNull Constructor<?>[] constructors)
         {
            Arrays.sort(constructors, new Comparator<Constructor<?>>() {
               @Override
               public int compare(Constructor<?> c1, Constructor<?> c2)
               {
                  int m1 = constructorModifiers(c1);
                  int m2 = constructorModifiers(c2);
                  if (m1 == m2) return 0;
                  if (m1 == PUBLIC) return -1;
                  if (m2 == PUBLIC) return 1;
                  if (m1 == PROTECTED) return -1;
                  if (m2 == PROTECTED) return 1;
                  if (m2 == PRIVATE) return -1;
                  return 1;
               }
            });

            for (Constructor<?> c : constructors) {
               List<MockedType> injectablesFound = findAvailableInjectablesForConstructor(c);

               if (
                  injectablesFound != null &&
                  (constructor == null ||
                   constructorModifiers(c) == constructorModifiers(constructor) &&
                   injectablesFound.size() >= injectablesForConstructor.size())
               ) {
                  injectablesForConstructor = injectablesFound;
                  constructor = c;
               }
            }
         }

         private static final int CONSTRUCTOR_ACCESS = PUBLIC + PROTECTED + PRIVATE;
         private int constructorModifiers(Constructor<?> c) { return CONSTRUCTOR_ACCESS & c.getModifiers(); }

         @Nullable
         private List<MockedType> findAvailableInjectablesForConstructor(@NotNull Constructor<?> candidate)
         {
            parameterTypes = candidate.getGenericParameterTypes();
            int n = parameterTypes.length;
            List<MockedType> injectablesFound = new ArrayList<MockedType>(n);
            boolean varArgs = candidate.isVarArgs();

            if (varArgs) {
               n--;
            }

            String constructorDesc = "<init>" + mockit.external.asm4.Type.getConstructorDescriptor(candidate);

            for (int i = 0; i < n; i++) {
               setTypeOfInjectionPoint(parameterTypes[i]);

               String parameterName = ParameterNames.getName(testedClassDesc, constructorDesc, i);
               MockedType injectable = parameterName == null ? null : findInjectable(parameterName);

               if (injectable == null || injectablesFound.contains(injectable)) {
                  return null;
               }

               injectablesFound.add(injectable);
            }

            if (varArgs) {
               MockedType injectable = hasInjectedValuesForVarargsParameter(n);

               if (injectable != null) {
                  injectablesFound.add(injectable);
               }
            }

            return injectablesFound;
         }

         @Nullable private MockedType findInjectable(@NotNull String nameOfInjectionPoint)
         {
            boolean multipleInjectablesFound = false;
            MockedType found = null;

            for (MockedType injectable : injectables) {
               if (hasSameTypeAsInjectionPoint(injectable)) {
                  if (found == null) {
                     found = injectable;
                  }
                  else {
                     if (nameOfInjectionPoint.equals(injectable.mockId)) {
                        return injectable;
                     }

                     multipleInjectablesFound = true;
                  }
               }
            }

            if (multipleInjectablesFound && !nameOfInjectionPoint.equals(found.mockId)) {
               return null;
            }

            return found;
         }

         @Nullable
         private MockedType hasInjectedValuesForVarargsParameter(int varargsParameterIndex)
         {
            getTypeOfInjectionPointFromVarargsParameter(varargsParameterIndex);
            return findNextInjectableForVarargsParameter();
         }
      }

      @NotNull
      private Type getTypeOfInjectionPointFromVarargsParameter(int varargsParameterIndex)
      {
         Type parameterType = parameterTypes[varargsParameterIndex];

         if (parameterType instanceof Class<?>) {
            parameterType = ((Class<?>) parameterType).getComponentType();
         }
         else {
            parameterType = ((GenericArrayType) parameterType).getGenericComponentType();
         }

         setTypeOfInjectionPoint(parameterType);
         return parameterType;
      }

      private final class ConstructorInjection
      {
         @NotNull
         Object instantiate()
         {
            parameterTypes = constructor.getGenericParameterTypes();
            int n = parameterTypes.length;
            Object[] arguments = new Object[n];
            boolean varArgs = constructor.isVarArgs();

            if (varArgs) {
               n--;
            }

            for (int i = 0; i < n; i++) {
               MockedType injectable = injectablesForConstructor.get(i);
               Object value = getArgumentValueToInject(injectable);
               arguments[i] = wrapInProviderIfNeeded(parameterTypes[i], value);
            }

            if (varArgs) {
               arguments[n] = obtainInjectedVarargsArray(n);
            }

            return ConstructorReflection.invoke(constructor, arguments);
         }

         @NotNull
         private Object obtainInjectedVarargsArray(int varargsParameterIndex)
         {
            Type varargsElementType = getTypeOfInjectionPointFromVarargsParameter(varargsParameterIndex);

            List<Object> varargValues = new ArrayList<Object>();
            MockedType injectable;

            while ((injectable = findNextInjectableForVarargsParameter()) != null) {
               Object value = getValueToInject(injectable);

               if (value != null) {
                  value = wrapInProviderIfNeeded(varargsElementType, value);
                  varargValues.add(value);
               }
            }

            int elementCount = varargValues.size();
            Object varargArray = Array.newInstance(getClassType(varargsElementType), elementCount);

            for (int i = 0; i < elementCount; i++) {
               Array.set(varargArray, i, varargValues.get(i));
            }

            return varargArray;
         }

         @NotNull
         private Object getArgumentValueToInject(@NotNull MockedType injectable)
         {
            Object argument = getValueToInject(injectable);

            if (argument == null) {
               assert injectable.mockId != null;
               throw new IllegalArgumentException(
                  "No injectable value available" + missingInjectableDescription(injectable.mockId));
            }

            return argument;
         }

         @NotNull
         private String missingInjectableDescription(@NotNull String name)
         {
            String classDesc = mockit.external.asm4.Type.getInternalName(constructor.getDeclaringClass());
            String constructorDesc = "<init>" + mockit.external.asm4.Type.getConstructorDescriptor(constructor);
            String constructorDescription = new MethodFormatter(classDesc, constructorDesc).toString();

            return
               " for parameter \"" + name + "\" in constructor " +
               constructorDescription.replace("java.lang.", "");
         }
      }
   }

   private final class FieldInjection
   {
      @NotNull private final Class<?> testedClass;
      @NotNull private final Object testedObject;
      private final boolean requiresJavaxInject;
      private boolean foundJavaxInject;

      private FieldInjection(@NotNull Class<?> testedClass, @NotNull Object testedObject, boolean requiresJavaxInject)
      {
         this.testedClass = testedClass;
         this.testedObject = testedObject;
         this.requiresJavaxInject = requiresJavaxInject;
      }

      @NotNull
      List<Field> findAllTargetInstanceFieldsInTestedClassHierarchy()
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
         while (isFromSameModuleOrSystemAsSuperClass(classWithFields));

         discardFieldsNotAnnotatedWithJavaxInjectIfAtLeastOneIsAnnotated(targetFields);

         return targetFields;
      }

      private boolean isEligibleForInjection(@NotNull Field field)
      {
         if (isFinal(field.getModifiers())) return false;
         if (requiresJavaxInject) return isAnnotatedWithJavaxInject(field);
         boolean notStatic = !isStatic(field.getModifiers());
         return INJECT_CLASS == null ? notStatic : isAnnotatedWithJavaxInject(field) || notStatic;
      }

      private boolean isAnnotatedWithJavaxInject(@NotNull Field field)
      {
         boolean annotated = field.isAnnotationPresent(INJECT_CLASS);
         if (annotated) foundJavaxInject = true;
         return annotated;
      }

      private void discardFieldsNotAnnotatedWithJavaxInjectIfAtLeastOneIsAnnotated(@NotNull List<Field> targetFields)
      {
         if (!requiresJavaxInject && foundJavaxInject) {
            ListIterator<Field> itr = targetFields.listIterator();

            while (itr.hasNext()) {
               Field targetField = itr.next();

               if (!targetField.isAnnotationPresent(INJECT_CLASS)) {
                  itr.remove();
               }
            }
         }
      }

      private boolean isFromSameModuleOrSystemAsSuperClass(@NotNull Class<?> superClass)
      {
         if (superClass.getClassLoader() == null) {
            return false;
         }

         if (superClass.getProtectionDomain() == testedClass.getProtectionDomain()) {
            return true;
         }

         String className1 = superClass.getName();
         String className2 = testedClass.getName();
         int p1 = className1.indexOf('.');
         int p2 = className2.indexOf('.');

         if (p1 != p2 || p1 == -1) {
            return false;
         }

         p1 = className1.indexOf('.', p1 + 1);
         p2 = className2.indexOf('.', p2 + 1);

         return p1 == p2 && p1 > 0 && className1.substring(0, p1).equals(className2.substring(0, p2));
      }

      void injectIntoEligibleFields(@NotNull List<Field> targetFields)
      {
         for (Field field : targetFields) {
            if (notAssignedByConstructor(field)) {
               Object injectableValue = getValueForFieldIfAvailable(targetFields, field);

               if (injectableValue != null) {
                  injectableValue = wrapInProviderIfNeeded(field.getGenericType(), injectableValue);
                  FieldReflection.setFieldValue(field, testedObject, injectableValue);
               }
            }
         }
      }

      private boolean notAssignedByConstructor(@NotNull Field field)
      {
         if (INJECT_CLASS != null && field.isAnnotationPresent(INJECT_CLASS)) {
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
         setTypeOfInjectionPoint(fieldToBeInjected.getGenericType());

         String targetFieldName = fieldToBeInjected.getName();
         MockedType mockedType;

         if (withMultipleTargetFieldsOfSameType(targetFields, fieldToBeInjected)) {
            mockedType = findInjectableByTypeAndName(targetFieldName);
         }
         else {
            mockedType = findInjectableByTypeAndOptionallyName(targetFieldName);
         }

         return mockedType == null ? null : getValueToInject(mockedType);
      }

      private boolean withMultipleTargetFieldsOfSameType(
         @NotNull List<Field> targetFields, @NotNull Field fieldToBeInjected)
      {
         for (Field targetField : targetFields) {
            if (targetField != fieldToBeInjected && isSameTypeAsInjectionPoint(targetField.getGenericType())) {
               return true;
            }
         }

         return false;
      }

      @Nullable
      private MockedType findInjectableByTypeAndName(@NotNull String targetFieldName)
      {
         for (MockedType injectable : injectables) {
            if (hasSameTypeAsInjectionPoint(injectable) && targetFieldName.equals(injectable.mockId)) {
               return injectable;
            }
         }

         return null;
      }

      @Nullable
      private MockedType findInjectableByTypeAndOptionallyName(@NotNull String targetFieldName)
      {
         MockedType found = null;

         for (MockedType injectable : injectables) {
            if (hasSameTypeAsInjectionPoint(injectable)) {
               if (targetFieldName.equals(injectable.mockId)) {
                  return injectable;
               }

               if (found == null) {
                  found = injectable;
               }
            }
         }

         return found;
      }
   }
}
