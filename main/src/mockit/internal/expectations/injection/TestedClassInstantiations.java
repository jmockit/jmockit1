/*
 * Copyright (c) 2006-2014 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.expectations.injection;

import java.io.*;
import java.lang.annotation.*;
import java.lang.reflect.*;
import java.lang.reflect.Type;
import java.util.*;
import javax.inject.*;
import javax.persistence.*;
import javax.xml.parsers.*;
import static java.lang.reflect.Modifier.*;

import mockit.*;
import mockit.external.asm4.*;
import mockit.internal.*;
import mockit.internal.expectations.mocking.*;
import mockit.internal.state.*;
import mockit.internal.util.*;
import static mockit.external.asm4.ClassReader.*;
import static mockit.internal.util.ClassLoad.*;
import static mockit.internal.util.ConstructorReflection.*;
import static mockit.internal.util.Utilities.*;

import org.jetbrains.annotations.*;
import org.xml.sax.*;
import org.xml.sax.helpers.*;

public final class TestedClassInstantiations
{
   @Nullable private static final Class<? extends Annotation> INJECT_CLASS;
   @Nullable private static final Class<? extends Annotation> PERSISTENCE_UNIT_CLASS;
   @Nullable private static final Class<? extends Annotation> PERSISTENCE_CONTEXT_CLASS;
   @Nullable private static final Class<?> ENTITY_MANAGER_FACTORY_CLASS;
   @Nullable private static final Class<?> ENTITY_MANAGER_CLASS;
   private static final boolean WITH_INJECTION_API_IN_CLASSPATH;

   static
   {
      INJECT_CLASS = searchTypeInClasspath("javax.inject.Inject");
      PERSISTENCE_UNIT_CLASS = searchTypeInClasspath("javax.persistence.PersistenceUnit");
      PERSISTENCE_CONTEXT_CLASS = searchTypeInClasspath("javax.persistence.PersistenceContext");
      ENTITY_MANAGER_FACTORY_CLASS = searchTypeInClasspath("javax.persistence.EntityManagerFactory");
      ENTITY_MANAGER_CLASS = searchTypeInClasspath("javax.persistence.EntityManager");
      WITH_INJECTION_API_IN_CLASSPATH = INJECT_CLASS != null || PERSISTENCE_UNIT_CLASS != null;
   }

   @NotNull private final List<TestedField> testedFields;
   @NotNull private final List<MockedType> injectableFields;
   @NotNull private List<MockedType> injectables;
   @NotNull private List<MockedType> consumedInjectables;
   private GenericTypeReflection testedTypeReflection;
   private Object currentTestClassInstance;
   private Type typeOfInjectionPoint;

   private final class TestedField
   {
      @NotNull final Field testedField;
      @NotNull final Tested metadata;
      @NotNull private final TestedObjectCreation testedObjectCreation;
      @Nullable private List<Field> targetFields;
      private boolean createAutomatically;

      TestedField(@NotNull Field field, @NotNull Tested metadata)
      {
         testedField = field;
         this.metadata = metadata;
         testedObjectCreation = new TestedObjectCreation(field);
      }

      void instantiateWithInjectableValues()
      {
         Object testedObject = null;

         if (!createAutomatically) {
            testedObject = FieldReflection.getFieldValue(testedField, currentTestClassInstance);
            createAutomatically = testedObject == null && !isFinal(testedField.getModifiers());
         }

         testedTypeReflection = new GenericTypeReflection(testedField);

         boolean requiresAnnotation = false;
         Class<?> testedClass;

         if (createAutomatically) {
            testedClass = testedObjectCreation.declaredTestedClass;
            testedObject = testedObjectCreation.create();
            FieldReflection.setFieldValue(testedField, currentTestClassInstance, testedObject);

            requiresAnnotation = testedObjectCreation.constructorAnnotatedWithJavaxInject;
         }
         else {
            testedClass = testedObject == null ? null : testedObject.getClass();
         }

         if (testedObject != null) {
            FieldInjection fieldInjection =
               new FieldInjection(testedClass, testedObject, requiresAnnotation, metadata.fullyInitialized());

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
         Tested testedMetadata = field.getAnnotation(Tested.class);

         if (testedMetadata != null) {
            TestedField testedField = new TestedField(field, testedMetadata);
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

         Class<?> generatedSubclass =
            ImplementationClass.defineNewClass(declaredTestedClass.getClassLoader(), bytecode, subclassName);

         TestRun.mockFixture().registerMockedClass(generatedSubclass);
         return generatedSubclass;
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
            Object[] arguments = n == 0 ? NO_ARGS : new Object[n];
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

            return invoke(constructor, arguments);
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
      @NotNull private final Map<Object, Object> instantiatedDependencies;
      private final boolean requiresAnnotation;
      private boolean foundAnnotations;
      @Nullable private String defaultPersistenceUnitName;

      FieldInjection(
         @NotNull Class<?> testedClass, @NotNull Object testedObject, boolean requiresAnnotation, boolean fullInjection)
      {
         this.testedClass = testedClass;
         this.testedObject = testedObject;
         this.requiresAnnotation = requiresAnnotation;
         instantiatedDependencies = fullInjection ? new HashMap<Object, Object>() : Collections.emptyMap();
      }

      private FieldInjection(
         @NotNull Object transitiveDependency, boolean requiresAnnotation,
         @NotNull Map<Object, Object> instantiatedDependencies)
      {
         testedClass = transitiveDependency.getClass();
         testedObject = transitiveDependency;
         this.requiresAnnotation = requiresAnnotation;
         this.instantiatedDependencies = instantiatedDependencies;
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
         while (isClassFromSameModuleOrSystemAsTestedClass(classWithFields));

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

      private boolean isAnnotated(@NotNull Field field)
      {
         return
            INJECT_CLASS != null && field.isAnnotationPresent(INJECT_CLASS) ||
            PERSISTENCE_UNIT_CLASS != null && (
               field.isAnnotationPresent(PERSISTENCE_CONTEXT_CLASS) || field.isAnnotationPresent(PERSISTENCE_UNIT_CLASS)
            );
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

      private boolean isClassFromSameModuleOrSystemAsTestedClass(@NotNull Class<?> anotherClass)
      {
         if (anotherClass.getClassLoader() == null) {
            return false;
         }

         if (anotherClass.getProtectionDomain() == testedClass.getProtectionDomain()) {
            return true;
         }

         String className1 = anotherClass.getName();
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
         setTypeOfInjectionPoint(fieldToBeInjected.getGenericType());

         String targetFieldName = fieldToBeInjected.getName();
         MockedType mockedType;

         if (withMultipleTargetFieldsOfSameType(targetFields, fieldToBeInjected)) {
            mockedType = findInjectableByTypeAndName(targetFieldName);
         }
         else {
            mockedType = findInjectableByTypeAndOptionallyName(targetFieldName);
         }

         if (mockedType != null) {
            return getValueToInject(mockedType);
         }

         if (instantiatedDependencies != Collections.emptyMap()) {
            return newInstanceCreatedWithNoArgsConstructorIfAvailable(fieldToBeInjected);
         }

         return null;
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

      @Nullable
      private Object newInstanceCreatedWithNoArgsConstructorIfAvailable(@NotNull Field fieldToBeInjected)
      {
         Object dependencyKey = getDependencyKey(fieldToBeInjected);
         Object dependency = instantiatedDependencies.get(dependencyKey);

         if (dependency == null) {
            Class<?> dependencyClass = fieldToBeInjected.getType();

            if (dependencyClass.isInterface()) {
               dependency = newInstanceIfKnownStandardInterface(fieldToBeInjected, dependencyKey);
            }
            else {
               dependency = newInstanceUsingDefaultConstructorIfAvailable(dependencyClass);
            }

            if (dependency != null) {
               fillOutDependenciesRecursively(dependency);
               instantiatedDependencies.put(dependencyKey, dependency);
            }
         }

         return dependency;
      }

      @NotNull
      private Object getDependencyKey(@NotNull Field fieldToBeInjected)
      {
         String id = null;

         for (Annotation annotation : fieldToBeInjected.getDeclaredAnnotations()) {
            Class<? extends Annotation> annotationType = annotation.annotationType();
            String candidateId = null;

            if (annotationType == PERSISTENCE_UNIT_CLASS) {
               candidateId = ((PersistenceUnit) annotation).unitName();
            }
            else if (annotationType == PERSISTENCE_CONTEXT_CLASS) {
               candidateId = ((PersistenceContext) annotation).unitName();
            }

            if (candidateId != null && !candidateId.isEmpty()) {
               id = candidateId;
               break;
            }
         }

         Class<?> dependencyClass = fieldToBeInjected.getType();
         return id == null ? dependencyClass : dependencyClass.getName() + ':' + id;
      }

      @Nullable
      private Object newInstanceIfKnownStandardInterface(
         @NotNull Field fieldToBeInjected, @NotNull Object dependencyKey)
      {
         Class<?> dependencyClass = fieldToBeInjected.getType();

         if (dependencyClass == ENTITY_MANAGER_FACTORY_CLASS) {
            String persistenceUnitName;

            if (dependencyKey instanceof String) {
               persistenceUnitName = extractIdFromDependencyKey((String) dependencyKey);
            }
            else {
               persistenceUnitName = discoverNameOfDefaultPersistenceUnit();
            }

            EntityManagerFactory emFactory = Persistence.createEntityManagerFactory(persistenceUnitName);
            return emFactory;
         }

         if (dependencyClass == ENTITY_MANAGER_CLASS) {
            return findOrCreateEntityManager(dependencyKey);
         }

         return null;
      }

      @NotNull
      private String extractIdFromDependencyKey(@NotNull String dependencyKey)
      {
         int p = dependencyKey.indexOf(':');
         return dependencyKey.substring(p + 1);
      }

      @NotNull
      private String discoverNameOfDefaultPersistenceUnit()
      {
         if (defaultPersistenceUnitName != null) {
            return defaultPersistenceUnitName;
         }

         defaultPersistenceUnitName = "<unknown>";
         InputStream xmlFile = getClass().getResourceAsStream("/META-INF/persistence.xml");

         if (xmlFile != null) {
            try {
               SAXParser parser = SAXParserFactory.newInstance().newSAXParser();
               parser.parse(xmlFile, new DefaultHandler() {
                  @Override
                  public void startElement(String uri, String localName, String qName, Attributes attributes)
                  {
                     if ("persistence-unit".equals(qName)) {
                        defaultPersistenceUnitName = attributes.getValue("name");
                     }
                  }
               });
               xmlFile.close();
            }
            catch (ParserConfigurationException ignore) {}
            catch (SAXException ignore) {}
            catch (IOException ignore) {}
         }

         return defaultPersistenceUnitName;
      }

      @Nullable
      private Object findOrCreateEntityManager(@NotNull Object dependencyKey)
      {
         String persistenceUnitName;
         Object emFactoryKey;

         if (dependencyKey instanceof String) {
            persistenceUnitName = extractIdFromDependencyKey((String) dependencyKey);
            emFactoryKey = EntityManagerFactory.class.getName() + ':' + persistenceUnitName;
         }
         else {
            persistenceUnitName = "";
            emFactoryKey = EntityManagerFactory.class;
         }

         EntityManagerFactory emFactory = (EntityManagerFactory) instantiatedDependencies.get(emFactoryKey);

         if (emFactory == null) {
            emFactory = Persistence.createEntityManagerFactory(persistenceUnitName);
         }

         return emFactory == null ? null : emFactory.createEntityManager();
      }

      private void fillOutDependenciesRecursively(@NotNull Object dependency)
      {
         if (isClassFromSameModuleOrSystemAsTestedClass(dependency.getClass())) {
            FieldInjection recursiveInjection =
               new FieldInjection(dependency, requiresAnnotation, instantiatedDependencies);

            List<Field> targetFields = recursiveInjection.findAllTargetInstanceFieldsInTestedClassHierarchy();

            if (!targetFields.isEmpty()) {
               List<MockedType> currentlyConsumedInjectables = consumedInjectables;
               consumedInjectables = new ArrayList<MockedType>();
               recursiveInjection.injectIntoEligibleFields(targetFields);
               consumedInjectables = currentlyConsumedInjectables;
            }
         }
      }
   }
}
