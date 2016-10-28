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
import mockit.internal.state.*;
import mockit.internal.util.*;
import static mockit.internal.injection.InjectionPoint.*;
import static mockit.internal.util.Utilities.getClassType;

/**
 * Holds state used throughout the injection process while it's in progress for a given set of tested objects.
 */
final class InjectionState implements BeanExporter
{
   @Nonnull private static final Map<InjectionPoint, Object> globalDependencies =
      new ConcurrentHashMap<InjectionPoint, Object>(2);

   @Nonnull private final Map<InjectionPoint, Object> testedObjects;
   @Nonnull private final Map<InjectionPoint, Object> instantiatedDependencies;
   @Nonnull private List<MockedType> injectables;
   @Nonnull private List<InjectionPointProvider> consumedInjectables;
   @Nonnull final LifecycleMethods lifecycleMethods;
   private GenericTypeReflection testedTypeReflection;
   private Object currentTestClassInstance;
   private Type typeOfInjectionPoint;

   InjectionState()
   {
      testedObjects = new HashMap<InjectionPoint, Object>();
      instantiatedDependencies = new HashMap<InjectionPoint, Object>();
      injectables = Collections.emptyList();
      consumedInjectables = new ArrayList<InjectionPointProvider>();
      lifecycleMethods = new LifecycleMethods();
   }

   void buildListsOfInjectables(@Nonnull Object testClassInstance, @Nonnull List<MockedType> injectables)
   {
      currentTestClassInstance = testClassInstance;
      this.injectables = new ArrayList<MockedType>(injectables);

      ParameterTypeRedefinitions paramTypeRedefs = TestRun.getExecutingTest().getParameterRedefinitions();

      if (paramTypeRedefs != null) {
         this.injectables.addAll(paramTypeRedefs.getInjectableParameters());
      }

      getServletConfigForInitMethodsIfAny(testClassInstance);
   }

   private void getServletConfigForInitMethodsIfAny(@Nonnull Object testClassInstance)
   {
      if (SERVLET_CLASS != null) {
         for (InjectionPointProvider injectable : injectables) {
            if (injectable.getDeclaredType() == ServletConfig.class) {
               lifecycleMethods.servletConfig = injectable.getValue(testClassInstance);
               break;
            }
         }
      }
   }

   Object getCurrentTestClassInstance() { return currentTestClassInstance; }

   void setTestedTypeReflection(@Nonnull GenericTypeReflection reflection) { testedTypeReflection = reflection; }

   void setTypeOfInjectionPoint(@Nonnull Type typeOfInjectionPoint)
   {
      this.typeOfInjectionPoint = typeOfInjectionPoint;
   }

   private boolean hasSameTypeAsInjectionPoint(@Nonnull InjectionPointProvider injectable)
   {
      Type declaredType = injectable.getDeclaredType();
      return isSameTypeAsInjectionPoint(declaredType);
   }

   boolean isSameTypeAsInjectionPoint(@Nonnull Type injectableType)
   {
      if (testedTypeReflection.areMatchingTypes(typeOfInjectionPoint, injectableType)) {
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
   MockedType findNextInjectableForInjectionPoint()
   {
      for (MockedType injectable : injectables) {
         if (hasSameTypeAsInjectionPoint(injectable) && !consumedInjectables.contains(injectable)) {
            return injectable;
         }
      }

      return null;
   }

   @Nonnull
   List<MockedType> findInjectablesByType()
   {
      List<MockedType> found = new ArrayList<MockedType>();

      for (MockedType injectable : injectables) {
         if (hasSameTypeAsInjectionPoint(injectable) && !consumedInjectables.contains(injectable)) {
            found.add(injectable);
         }
      }

      return found;
   }

   @Nullable
   InjectionPointProvider getProviderByTypeAndOptionallyName(@Nonnull String nameOfInjectionPoint)
   {
      Type elementTypeOfIterable = getElementTypeIfIterable(typeOfInjectionPoint);

      if (elementTypeOfIterable != null) {
         return findInjectablesByTypeOnly(elementTypeOfIterable);
      }

      return findInjectableByTypeAndOptionallyName(nameOfInjectionPoint);
   }

   @Nullable
   Object getValueForParameterFromTestedField(@Nonnull String nameOfInjectionPoint)
   {
      InjectionPoint injectionPoint = new InjectionPoint(typeOfInjectionPoint, nameOfInjectionPoint);
      return testedObjects.get(injectionPoint);
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
   private InjectionPointProvider findInjectablesByTypeOnly(@Nonnull Type elementType)
   {
      MultiValuedProvider found = null;

      for (MockedType injectable : injectables) {
         Type injectableType = injectable.getDeclaredType();
         Type elementTypeOfIterable = getElementTypeIfIterable(injectableType);

         if (
            elementTypeOfIterable != null && testedTypeReflection.areMatchingTypes(elementType, elementTypeOfIterable)
         ) {
            return injectable;
         }

         if (isSameTypeAsInjectionPoint(injectableType)) {
            if (found == null) {
               found = new MultiValuedProvider(elementType);
            }

            found.addInjectable(injectable);
         }
      }

      return found;
   }

   @Nullable
   private MockedType findInjectableByTypeAndOptionallyName(@Nonnull String nameOfInjectionPoint)
   {
      MockedType found = null;

      for (MockedType injectable : injectables) {
         if (hasSameTypeAsInjectionPoint(injectable)) {
            if (nameOfInjectionPoint.equals(injectable.getName())) {
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
   MockedType findInjectableByTypeAndName(@Nonnull String nameOfInjectionPoint)
   {
      for (MockedType injectable : injectables) {
         if (hasSameTypeAsInjectionPoint(injectable) && nameOfInjectionPoint.equals(injectable.getName())) {
            return injectable;
         }
      }

      return null;
   }

   @Nullable
   Object getValueToInject(@Nonnull InjectionPointProvider injectable)
   {
      if (consumedInjectables.contains(injectable)) {
         return null;
      }

      Object value = injectable.getValue(currentTestClassInstance);

      if (value != null) {
         consumedInjectables.add(injectable);
      }

      return value;
   }

   void resetConsumedInjectables()
   {
      consumedInjectables.clear();
   }

   @Nonnull
   List<InjectionPointProvider> saveConsumedInjectables()
   {
      List<InjectionPointProvider> previousConsumedInjectables = consumedInjectables;
      consumedInjectables = new ArrayList<InjectionPointProvider>();
      return previousConsumedInjectables;
   }

   void restoreConsumedInjectables(@Nonnull List<InjectionPointProvider> previousConsumedInjectables)
   {
      consumedInjectables = previousConsumedInjectables;
   }

   void saveTestedObject(@Nonnull InjectionPoint key, @Nonnull Object testedObject)
   {
      testedObjects.put(key, testedObject);
   }

   @Nullable
   Object getTestedInstance(@Nonnull Type testedType, @Nonnull String nameOfInjectionPoint)
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

   @Nullable @SuppressWarnings("unchecked")
   <D> D getGlobalDependency(@Nonnull InjectionPoint key) { return (D) globalDependencies.get(key); }

   @Nullable
   Object getInstantiatedDependency(
      @Nonnull TestedClass testedClass, @Nonnull InjectionPointProvider injectionProvider,
      @Nonnull InjectionPoint dependencyKey)
   {
      Object dependency = testedObjects.get(dependencyKey);

      if (dependency == null) {
         dependency = findMatchingObject(testedObjects, testedClass.reflection, dependencyKey);

         if (dependency == null) {
            dependency = instantiatedDependencies.get(dependencyKey);

            if (dependency == null) {
               dependency = findMatchingObject(instantiatedDependencies, null, dependencyKey);

               if (dependency == null) {
                  dependency = findMatchingObject(globalDependencies, null, dependencyKey);
               }
            }
         }
      }

      return dependency;
   }

   @Nullable
   private Object findMatchingObject(
      @Nonnull Map<InjectionPoint, Object> availableObjects, @Nullable GenericTypeReflection reflection,
      @Nonnull InjectionPoint injectionPoint)
   {
      Type dependencyType = injectionPoint.type;

      for (Entry<InjectionPoint, Object> injectionPointAndObject : availableObjects.entrySet()) {
         InjectionPoint dependencyIP = injectionPointAndObject.getKey();
         Object dependencyObject = injectionPointAndObject.getValue();

         if (
            injectionPoint.equals(dependencyIP) ||
            reflection != null && reflection.areMatchingTypes(dependencyType, dependencyIP.type)
         ) {
            return dependencyObject;
         }
      }

      return null;
   }

   void saveInstantiatedDependency(@Nonnull InjectionPoint dependencyKey, @Nonnull Object dependency)
   {
      instantiatedDependencies.put(dependencyKey, dependency);
   }

   void saveGlobalDependency(@Nonnull InjectionPoint dependencyKey, @Nonnull Object dependency)
   {
      globalDependencies.put(dependencyKey, dependency);
   }

   void clearTestedObjectsAndInstantiatedDependencies()
   {
      testedObjects.clear();
      instantiatedDependencies.clear();
   }

   @Override
   public Object getBean(@Nonnull String name)
   {
      for (InjectionPointProvider injectable : injectables) {
         if (name.equals(injectable.getName())) {
            return injectable.getValue(currentTestClassInstance);
         }
      }

      Object bean = findByName(testedObjects, name);

      if (bean == null) {
         bean = findByName(instantiatedDependencies, name);

         if (bean == null) {
            bean = findByName(globalDependencies, name);
         }
      }

      return bean;
   }

   @Nullable
   private Object findByName(@Nonnull Map<InjectionPoint, Object> injectionPointsAndObjects, @Nonnull String name)
   {
      for (Entry<InjectionPoint, Object> injectionPointAndObject : injectionPointsAndObjects.entrySet()) {
         InjectionPoint injectionPoint = injectionPointAndObject.getKey();

         if (name.equals(injectionPoint.name)) {
            return injectionPointAndObject.getValue();
         }
      }

      return null;
   }
}
