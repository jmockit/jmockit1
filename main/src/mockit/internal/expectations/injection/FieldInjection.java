package mockit.internal.expectations.injection;

import java.io.*;
import java.lang.annotation.*;
import java.lang.reflect.*;
import java.util.*;
import javax.persistence.*;
import javax.xml.parsers.*;
import static java.lang.reflect.Modifier.*;

import mockit.internal.expectations.mocking.*;
import mockit.internal.util.*;
import static mockit.internal.expectations.injection.InjectionPoint.*;
import static mockit.internal.util.ConstructorReflection.*;

import org.jetbrains.annotations.*;
import org.xml.sax.*;
import org.xml.sax.helpers.*;

final class FieldInjection
{
   @NotNull private final InjectionState injectionState;
   @NotNull private final Class<?> testedClass;
   @NotNull private final Object testedObject;
   @NotNull private final Map<Object, Object> instantiatedDependencies;
   private final boolean requiresAnnotation;
   private boolean foundAnnotations;
   @Nullable private String defaultPersistenceUnitName;

   FieldInjection(
      @NotNull InjectionState injectionState,
      @NotNull Class<?> testedClass, @NotNull Object testedObject, boolean requiresAnnotation, boolean fullInjection)
   {
      this.injectionState = injectionState;
      this.testedClass = testedClass;
      this.testedObject = testedObject;
      this.requiresAnnotation = requiresAnnotation;
      instantiatedDependencies = fullInjection ? new HashMap<Object, Object>() : Collections.emptyMap();
   }

   FieldInjection(
      @NotNull InjectionState injectionState,
      @NotNull Object transitiveDependency, boolean requiresAnnotation,
      @NotNull Map<Object, Object> instantiatedDependencies)
   {
      this.injectionState = injectionState;
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

      if (instantiatedDependencies != Collections.emptyMap()) {
         return newInstanceCreatedWithNoArgsConstructorIfAvailable(fieldToBeInjected);
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
            Class<?> instantiatedClass = dependency.getClass();

            if (isClassFromSameModuleOrSystemAsTestedClass(instantiatedClass)) {
               fillOutDependenciesRecursively(dependency);
               executePostConstructMethodIfAny(instantiatedClass, dependency);
            }

            instantiatedDependencies.put(dependencyKey, dependency);
         }
      }

      return dependency;
   }

   @NotNull
   private static Object getDependencyKey(@NotNull Field fieldToBeInjected)
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
   private static String extractIdFromDependencyKey(@NotNull String dependencyKey)
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
         persistenceUnitName = null;
         emFactoryKey = EntityManagerFactory.class;
      }

      EntityManagerFactory emFactory = (EntityManagerFactory) instantiatedDependencies.get(emFactoryKey);

      if (emFactory == null) {
         if (persistenceUnitName == null) {
            persistenceUnitName = discoverNameOfDefaultPersistenceUnit();
         }

         emFactory = Persistence.createEntityManagerFactory(persistenceUnitName);
      }

      return emFactory == null ? null : emFactory.createEntityManager();
   }

   private void fillOutDependenciesRecursively(@NotNull Object dependency)
   {
      FieldInjection recursiveInjection =
         new FieldInjection(injectionState, dependency, requiresAnnotation, instantiatedDependencies);

      List<Field> targetFields = recursiveInjection.findAllTargetInstanceFieldsInTestedClassHierarchy();

      if (!targetFields.isEmpty()) {
         List<MockedType> currentlyConsumedInjectables = injectionState.saveConsumedInjectables();
         recursiveInjection.injectIntoEligibleFields(targetFields);
         injectionState.restoreConsumedInjectables(currentlyConsumedInjectables);
      }
   }
}
