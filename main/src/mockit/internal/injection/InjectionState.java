/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.injection;

import java.lang.reflect.*;
import java.lang.reflect.Type;
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
   @Nonnull private List<MockedType> consumedInjectables;
   @Nonnull final LifecycleMethods lifecycleMethods;
   private GenericTypeReflection testedTypeReflection;
   private Object currentTestClassInstance;
   Type typeOfInjectionPoint;

   InjectionState()
   {
      testedObjects = new HashMap<InjectionPoint, Object>();
      instantiatedDependencies = new HashMap<InjectionPoint, Object>();
      injectables = Collections.emptyList();
      consumedInjectables = new ArrayList<MockedType>();
      lifecycleMethods = new LifecycleMethods();
   }

   void buildListsOfInjectables(@Nonnull Object testClassInstance, @Nonnull List<MockedType> injectableFields)
   {
      currentTestClassInstance = testClassInstance;
      injectables = new ArrayList<MockedType>(injectableFields);

      ParameterTypeRedefinitions paramTypeRedefs = TestRun.getExecutingTest().getParameterRedefinitions();

      if (paramTypeRedefs != null) {
         injectables.addAll(paramTypeRedefs.getInjectableParameters());
      }

      getServletConfigForInitMethodsIfAny(testClassInstance);
   }

   private void getServletConfigForInitMethodsIfAny(@Nonnull Object testClassInstance)
   {
      if (SERVLET_CLASS != null) {
         for (MockedType injectable : injectables) {
            if (injectable.declaredType == ServletConfig.class) {
               lifecycleMethods.servletConfig = injectable.getValueToInject(testClassInstance);
               break;
            }
         }
      }
   }

   Object getCurrentTestClassInstance() { return currentTestClassInstance; }

   void setTestedField(@Nonnull Field testedField) { testedTypeReflection = new GenericTypeReflection(testedField); }

   void setTypeOfInjectionPoint(@Nonnull Type typeOfInjectionPoint)
   {
      this.typeOfInjectionPoint = typeOfInjectionPoint;
   }

   private boolean hasSameTypeAsInjectionPoint(@Nonnull MockedType injectable)
   {
      return isSameTypeAsInjectionPoint(injectable.declaredType);
   }

   boolean isSameTypeAsInjectionPoint(@Nonnull Type injectableType)
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
   MockedType findInjectableByTypeAndOptionallyName(@Nonnull String nameOfInjectionPoint)
   {
      MockedType found = null;

      for (MockedType injectable : injectables) {
         if (hasSameTypeAsInjectionPoint(injectable)) {
            if (nameOfInjectionPoint.equals(injectable.mockId)) {
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
         if (hasSameTypeAsInjectionPoint(injectable) && nameOfInjectionPoint.equals(injectable.mockId)) {
            return injectable;
         }
      }

      return null;
   }

   @Nullable
   Object getValueToInject(@Nonnull MockedType injectable)
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

   void resetConsumedInjectables()
   {
      consumedInjectables.clear();
   }

   @Nonnull
   List<MockedType> saveConsumedInjectables()
   {
      List<MockedType> previousConsumedInjectables = consumedInjectables;
      consumedInjectables = new ArrayList<MockedType>();
      return previousConsumedInjectables;
   }

   void restoreConsumedInjectables(@Nonnull List<MockedType> previousConsumedInjectables)
   {
      consumedInjectables = previousConsumedInjectables;
   }

   void saveTestedObject(@Nonnull InjectionPoint key, @Nonnull Object testedObject)
   {
      testedObjects.put(key, testedObject);
   }

   @Nullable
   Object getTestedInstance(@Nonnull Type testedType)
   {
      InjectionPoint injectionPoint = new InjectionPoint(testedType);
      return instantiatedDependencies.get(injectionPoint);
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
         dependency = findMatchingObject(testedObjects, testedClass.genericType, dependencyKey);

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
