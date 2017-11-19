/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.injection;

import java.lang.reflect.*;
import java.util.*;
import java.util.Map.*;
import java.util.concurrent.*;
import javax.annotation.*;
import javax.inject.*;
import javax.servlet.*;

import mockit.internal.expectations.mocking.*;
import mockit.internal.reflection.*;
import mockit.internal.state.*;
import static mockit.internal.injection.InjectionPoint.*;
import static mockit.internal.util.Utilities.getClassType;

/**
 * Holds state used throughout the injection process while it's in progress for a given set of tested objects.
 */
public final class InjectionState
{
   @Nonnull private static final Map<InjectionPoint, Object> globalDependencies =
      new ConcurrentHashMap<InjectionPoint, Object>(2);

   @Nonnull private final Map<InjectionPoint, Object> testedObjects;
   @Nonnull private final Map<InjectionPoint, Object> instantiatedDependencies;
   @Nonnull private List<MockedType> injectables;
   @Nonnull private List<InjectionProvider> consumedInjectionProviders;
   @Nonnull public final LifecycleMethods lifecycleMethods;
   @Nonnull final InterfaceResolution interfaceResolution;
   @Nullable private BeanExporter beanExporter;
   private Object currentTestClassInstance;
   private Type typeOfInjectionPoint;

   InjectionState()
   {
      testedObjects = new LinkedHashMap<InjectionPoint, Object>();
      instantiatedDependencies = new LinkedHashMap<InjectionPoint, Object>();
      injectables = Collections.emptyList();
      consumedInjectionProviders = new ArrayList<InjectionProvider>();
      lifecycleMethods = new LifecycleMethods();
      interfaceResolution = new InterfaceResolution();
   }

   boolean setInjectables(@Nonnull List<MockedType> injectables)
   {
      if (injectables.isEmpty()) {
         this.injectables = Collections.emptyList();
         return false;
      }

      this.injectables = new ArrayList<MockedType>(injectables);
      return true;
   }

   void buildListOfInjectableFields(@Nonnull Object testClassInstance, @Nonnull List<MockedType> injectables)
   {
      currentTestClassInstance = testClassInstance;
      setInjectables(injectables);
      getServletConfigForInitMethodsIfAny(testClassInstance);
   }

   void buildListsOfInjectables(@Nonnull Object testClassInstance, @Nonnull List<MockedType> injectables)
   {
      currentTestClassInstance = testClassInstance;
      setInjectables(injectables);

      ParameterTypeRedefinitions paramTypeRedefs = TestRun.getExecutingTest().getParameterRedefinitions();

      if (paramTypeRedefs != null) {
         addInjectables(paramTypeRedefs);
      }

      getServletConfigForInitMethodsIfAny(testClassInstance);
   }

   private void addInjectables(@Nonnull ParameterTypeRedefinitions paramTypeRedefs)
   {
      List<MockedType> injectableParameters = paramTypeRedefs.getInjectableParameters();

      if (!injectableParameters.isEmpty()) {
         if (injectables.isEmpty()) {
            injectables = new ArrayList<MockedType>(injectableParameters);
         }
         else {
            injectables.addAll(injectableParameters);
         }
      }
   }

   private void getServletConfigForInitMethodsIfAny(@Nonnull Object testClassInstance)
   {
      if (SERVLET_CLASS != null) {
         for (InjectionProvider injectable : injectables) {
            if (injectable.getDeclaredType() == ServletConfig.class) {
               lifecycleMethods.servletConfig = injectable.getValue(testClassInstance);
               break;
            }
         }
      }
   }

   void buildListsOfInjectables(@Nonnull Object testClassInstance, @Nonnull ParameterTypeRedefinitions paramTypeRedefs)
   {
      currentTestClassInstance = testClassInstance;
      addInjectables(paramTypeRedefs);
      getServletConfigForInitMethodsIfAny(testClassInstance);
   }

   Object getCurrentTestClassInstance() { return currentTestClassInstance; }

   public void setTypeOfInjectionPoint(@Nonnull Type typeOfInjectionPoint)
   {
      this.typeOfInjectionPoint = typeOfInjectionPoint;
   }

   public boolean isAssignableToInjectionPoint(@Nonnull Type injectableType, @Nonnull TestedClass testedClass)
   {
      if (testedClass.reflection.areMatchingTypes(typeOfInjectionPoint, injectableType)) {
         return true;
      }

      if (typeOfInjectionPoint instanceof ParameterizedType) {
         ParameterizedType parameterizedType = (ParameterizedType) typeOfInjectionPoint;
         Class<?> classOfInjectionPoint = (Class<?>) parameterizedType.getRawType();

         if (
            Iterable.class.isAssignableFrom(classOfInjectionPoint) ||
            INJECT_CLASS != null && Provider.class.isAssignableFrom(classOfInjectionPoint)
         ) {
            Type providedType = parameterizedType.getActualTypeArguments()[0];

            if (providedType.equals(injectableType)) {
               return true;
            }

            Class<?> injectableClass = getClassType(injectableType);
            Class<?> providedClass = getClassType(providedType);

            return providedClass.isAssignableFrom(injectableClass);
         }
      }

      return false;
   }

   @Nullable
   public MockedType findNextInjectableForInjectionPoint(@Nonnull TestedClass testedClass)
   {
      for (MockedType injectable : injectables) {
         if (
            hasTypeAssignableToInjectionPoint(injectable, testedClass) &&
            !consumedInjectionProviders.contains(injectable)
         ) {
            return injectable;
         }
      }

      return null;
   }

   private boolean hasTypeAssignableToInjectionPoint(
      @Nonnull InjectionProvider injectable, @Nonnull TestedClass testedClass)
   {
      Type declaredType = injectable.getDeclaredType();
      return isAssignableToInjectionPoint(declaredType, testedClass);
   }

   @Nonnull
   List<MockedType> findInjectablesByType(@Nonnull TestedClass testedClass)
   {
      List<MockedType> found = new ArrayList<MockedType>();

      for (MockedType injectable : injectables) {
         if (
            hasTypeAssignableToInjectionPoint(injectable, testedClass) &&
            !consumedInjectionProviders.contains(injectable)
         ) {
            found.add(injectable);
         }
      }

      return found;
   }

   @Nullable
   public InjectionProvider getProviderByTypeAndOptionallyName(
      @Nonnull String nameOfInjectionPoint, @Nonnull TestedClass testedClass)
   {
      Type elementTypeOfIterable = getElementTypeIfIterable(typeOfInjectionPoint);

      if (elementTypeOfIterable != null) {
         return findInjectablesByTypeOnly(elementTypeOfIterable, testedClass);
      }

      return findInjectableByTypeAndOptionallyName(nameOfInjectionPoint, testedClass);
   }

   @Nullable
   private static Type getElementTypeIfIterable(@Nonnull Type injectableType)
   {
      if (injectableType instanceof ParameterizedType) {
         ParameterizedType parameterizedType = (ParameterizedType) injectableType;
         Class<?> classOfInjectionPoint = (Class<?>) parameterizedType.getRawType();

         if (Iterable.class.isAssignableFrom(classOfInjectionPoint)) {
            return parameterizedType.getActualTypeArguments()[0];
         }
      }

      return null;
   }

   @Nullable
   private InjectionProvider findInjectablesByTypeOnly(@Nonnull Type elementType, @Nonnull TestedClass testedClass)
   {
      GenericTypeReflection typeReflection = testedClass.reflection;
      MultiValuedProvider found = null;

      for (MockedType injectable : injectables) {
         Type injectableType = injectable.getDeclaredType();
         Type elementTypeOfIterable = getElementTypeIfIterable(injectableType);

         if (elementTypeOfIterable != null && typeReflection.areMatchingTypes(elementType, elementTypeOfIterable)) {
            return injectable;
         }

         if (isAssignableToInjectionPoint(injectableType, testedClass)) {
            if (found == null) {
               found = new MultiValuedProvider(elementType);
            }

            found.addInjectable(injectable);
         }
      }

      return found;
   }

   @Nullable
   private InjectionProvider findInjectableByTypeAndOptionallyName(
      @Nonnull String nameOfInjectionPoint, @Nonnull TestedClass testedClass)
   {
      MockedType foundInjectable = null;

      for (MockedType injectable : injectables) {
         if (hasTypeAssignableToInjectionPoint(injectable, testedClass)) {
            if (nameOfInjectionPoint.equals(injectable.getName())) {
               return injectable;
            }

            if (foundInjectable == null) {
               foundInjectable = injectable;
            }
         }
      }

      return foundInjectable;
   }

   @Nullable
   public MockedType findInjectableByTypeAndName(@Nonnull String nameOfInjectionPoint, @Nonnull TestedClass testedClass)
   {
      for (MockedType injectable : injectables) {
         if (
            hasTypeAssignableToInjectionPoint(injectable, testedClass) &&
            nameOfInjectionPoint.equals(injectable.getName())
         ) {
            return injectable;
         }
      }

      return null;
   }

   @Nullable
   public Object getValueToInject(@Nonnull InjectionProvider injectionProvider)
   {
      if (consumedInjectionProviders.contains(injectionProvider)) {
         return null;
      }

      Object value = injectionProvider.getValue(currentTestClassInstance);

      if (value != null) {
         consumedInjectionProviders.add(injectionProvider);
      }

      return value;
   }

   void resetConsumedInjectionProviders() { consumedInjectionProviders.clear(); }

   @Nonnull
   public List<InjectionProvider> saveConsumedInjectionProviders()
   {
      List<InjectionProvider> previouslyConsumed = consumedInjectionProviders;
      consumedInjectionProviders = new ArrayList<InjectionProvider>();
      return previouslyConsumed;
   }

   public void restoreConsumedInjectionProviders(@Nonnull List<InjectionProvider> previouslyConsumed)
   {
      consumedInjectionProviders = previouslyConsumed;
   }

   void saveTestedObject(@Nonnull InjectionPoint key, @Nonnull Object testedObject)
   {
      testedObjects.put(key, testedObject);
   }

   @Nullable
   Object getTestedInstance(@Nonnull Type testedType, @Nonnull String nameOfInjectionPoint)
   {
      Object testedInstance = instantiatedDependencies.isEmpty() ?
         null : findPreviouslyInstantiatedDependency(testedType, nameOfInjectionPoint);

      if (testedInstance == null) {
         testedInstance = testedObjects.isEmpty() ?
            null : getValueFromExistingTestedObject(testedType, nameOfInjectionPoint);
      }

      return testedInstance;
   }

   @Nullable
   private Object findPreviouslyInstantiatedDependency(@Nonnull Type testedType, @Nonnull String nameOfInjectionPoint)
   {
      InjectionPoint injectionPoint = new InjectionPoint(testedType, nameOfInjectionPoint);
      Object dependency = instantiatedDependencies.get(injectionPoint);

      if (dependency == null) {
         injectionPoint = new InjectionPoint(testedType);
         dependency = instantiatedDependencies.get(injectionPoint);

         if (dependency == null) {
            dependency = findMatchingObject(instantiatedDependencies, null, injectionPoint);
         }
      }

      return dependency;
   }

   @Nullable
   private Object getValueFromExistingTestedObject(@Nonnull Type testedType, @Nonnull String nameOfInjectionPoint)
   {
      InjectionPoint injectionPoint = new InjectionPoint(testedType, nameOfInjectionPoint);

      for (Object testedObject : testedObjects.values()) {
         Object fieldValue = getValueFromFieldOfEquivalentTypeAndName(injectionPoint, testedObject);

         if (fieldValue != null) {
            return fieldValue;
         }
      }

      return null;
   }

   @Nullable
   private static Object getValueFromFieldOfEquivalentTypeAndName(
      @Nonnull InjectionPoint injectionPoint, @Nonnull Object testedObject)
   {
      for (Field internalField : testedObject.getClass().getDeclaredFields()) {
         Type fieldType = internalField.getGenericType();
         String qualifiedName = getQualifiedName(internalField.getDeclaredAnnotations());
         boolean qualified = qualifiedName != null;
         String fieldName = qualified ? qualifiedName : internalField.getName();
         InjectionPoint internalInjectionPoint = new InjectionPoint(fieldType, fieldName, qualified);

         if (internalInjectionPoint.equals(injectionPoint)) {
            Object fieldValue = FieldReflection.getFieldValue(internalField, testedObject);
            return fieldValue;
         }
      }

      return null;
   }

   @Nullable @SuppressWarnings("unchecked")
   public static <D> D getGlobalDependency(@Nonnull InjectionPoint key) { return (D) globalDependencies.get(key); }

   @Nullable
   public Object getTestedValue(@Nonnull TestedClass testedClass, @Nonnull InjectionPoint injectionPoint)
   {
      Object testedValue = testedObjects.get(injectionPoint);

      if (testedValue == null) {
         testedValue = findMatchingObject(testedObjects, testedClass, injectionPoint);
      }

      return testedValue;
   }

   @Nullable
   public Object getInstantiatedDependency(@Nullable TestedClass testedClass, @Nonnull InjectionPoint dependencyKey)
   {
      Object dependency = testedObjects.get(dependencyKey);

      if (dependency == null) {
         dependency = findMatchingObject(testedObjects, testedClass, dependencyKey);

         if (dependency == null) {
            dependency = instantiatedDependencies.get(dependencyKey);

            if (dependency == null) {
               dependency = findMatchingObject(instantiatedDependencies, testedClass, dependencyKey);

               if (dependency == null) {
                  dependency = findMatchingObject(globalDependencies, testedClass, dependencyKey);
               }
            }
         }
      }

      return dependency;
   }

   @Nullable
   private static Object findMatchingObject(
      @Nonnull Map<InjectionPoint, Object> availableObjects, @Nullable TestedClass testedClass,
      @Nonnull InjectionPoint injectionPoint)
   {
      if (availableObjects.isEmpty()) {
         return null;
      }

      GenericTypeReflection reflection = testedClass == null ? null : testedClass.reflection;
      Type dependencyType = injectionPoint.type;
      Object found = null;

      for (Entry<InjectionPoint, Object> injectionPointAndObject : availableObjects.entrySet()) {
         InjectionPoint dependencyIP = injectionPointAndObject.getKey();
         Object dependencyObject = injectionPointAndObject.getValue();

         if (injectionPoint.equals(dependencyIP)) {
            return dependencyObject;
         }

         if (reflection != null) {
            if (reflection.areMatchingTypes(dependencyType, dependencyIP.type)) {
               found = dependencyObject;
            }
            else {
               continue;
            }
         }

         if (injectionPoint.hasSameName(dependencyIP)) {
            return dependencyObject;
         }
      }

      return injectionPoint.qualified ? null : found;
   }

   public void saveInstantiatedDependency(@Nonnull InjectionPoint dependencyKey, @Nonnull Object dependency)
   {
      instantiatedDependencies.put(dependencyKey, dependency);
   }

   public static void saveGlobalDependency(@Nonnull InjectionPoint dependencyKey, @Nonnull Object dependency)
   {
      globalDependencies.put(dependencyKey, dependency);
   }

   void clearTestedObjectsAndInstantiatedDependencies()
   {
      testedObjects.clear();
      instantiatedDependencies.clear();
   }

   @Nonnull
   BeanExporter getBeanExporter()
   {
      if (beanExporter == null) {
         beanExporter = new BeanExporter(this);
      }

      return beanExporter;
   }

   @Nullable
   public Class<?> resolveInterface(@Nonnull Class<?> anInterface)
   {
      return interfaceResolution.resolveInterface(anInterface, currentTestClassInstance);
   }
}
