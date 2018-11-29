/*
 * Copyright (c) 2006 JMockit developers
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.injection;

import java.lang.reflect.*;
import java.util.*;
import java.util.Map.*;
import java.util.concurrent.*;
import javax.annotation.*;

import mockit.internal.reflection.*;
import static mockit.internal.injection.InjectionPoint.*;

/**
 * Holds state used throughout the injection process while it's in progress for a given set of tested objects.
 */
public final class InjectionState
{
   @Nonnull private static final Map<InjectionPoint, Object> globalDependencies = new ConcurrentHashMap<>(2);

   @Nonnull private final Map<InjectionPoint, Object> testedObjects;
   @Nonnull private final Map<InjectionPoint, Object> instantiatedDependencies;
   @Nonnull public final InjectionProviders injectionProviders;
   @Nonnull public final LifecycleMethods lifecycleMethods;
   @Nonnull final InterfaceResolution interfaceResolution;
   @Nullable private BeanExporter beanExporter;
   private Object currentTestClassInstance;

   InjectionState() {
      testedObjects = new LinkedHashMap<>();
      instantiatedDependencies = new LinkedHashMap<>();
      lifecycleMethods = new LifecycleMethods();
      injectionProviders = new InjectionProviders(lifecycleMethods);
      interfaceResolution = new InterfaceResolution();
   }

   void setInjectables(@Nonnull Object testClassInstance, @Nonnull List<? extends InjectionProvider> injectables) {
      currentTestClassInstance = testClassInstance;
      injectionProviders.setInjectables(injectables);
      lifecycleMethods.getServletConfigForInitMethodsIfAny(injectables, testClassInstance);
   }

   void addInjectables(@Nonnull Object testClassInstance, @Nonnull List<? extends InjectionProvider> injectablesToAdd) {
      currentTestClassInstance = testClassInstance;
      List<InjectionProvider> injectables = injectionProviders.addInjectables(injectablesToAdd);
      lifecycleMethods.getServletConfigForInitMethodsIfAny(injectables, testClassInstance);
   }

   Object getCurrentTestClassInstance() { return currentTestClassInstance; }

   @Nullable
   public Object getValueToInject(@Nonnull InjectionProvider injectionProvider) {
      return injectionProviders.getValueToInject(injectionProvider, currentTestClassInstance);
   }

   void saveTestedObject(@Nonnull InjectionPoint key, @Nonnull Object testedObject) {
      testedObjects.put(key, testedObject);
   }

   @Nullable
   Object getTestedInstance(@Nonnull Type testedType, @Nonnull String nameOfInjectionPoint) {
      Object testedInstance = instantiatedDependencies.isEmpty() ?
         null : findPreviouslyInstantiatedDependency(testedType, nameOfInjectionPoint);

      if (testedInstance == null) {
         testedInstance = testedObjects.isEmpty() ? null : getValueFromExistingTestedObject(testedType, nameOfInjectionPoint);
      }

      return testedInstance;
   }

   @Nullable
   private Object findPreviouslyInstantiatedDependency(@Nonnull Type testedType, @Nonnull String nameOfInjectionPoint) {
      InjectionPoint injectionPoint = new InjectionPoint(testedType, nameOfInjectionPoint);
      Object dependency = instantiatedDependencies.get(injectionPoint);

      if (dependency == null) {
         //noinspection ReuseOfLocalVariable
         injectionPoint = new InjectionPoint(testedType);
         dependency = instantiatedDependencies.get(injectionPoint);

         if (dependency == null) {
            dependency = findMatchingObject(instantiatedDependencies, null, injectionPoint);
         }
      }

      return dependency;
   }

   @Nullable
   private Object getValueFromExistingTestedObject(@Nonnull Type testedType, @Nonnull String nameOfInjectionPoint) {
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
   private static Object getValueFromFieldOfEquivalentTypeAndName(@Nonnull InjectionPoint injectionPoint, @Nonnull Object testedObject) {
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
   public Object getTestedValue(@Nonnull TestedClass testedClass, @Nonnull InjectionPoint injectionPoint) {
      Object testedValue = testedObjects.get(injectionPoint);

      if (testedValue == null) {
         testedValue = findMatchingObject(testedObjects, testedClass, injectionPoint);
      }

      return testedValue;
   }

   @Nullable
   public Object getInstantiatedDependency(@Nullable TestedClass testedClass, @Nonnull InjectionPoint dependencyKey) {
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
      @Nonnull Map<InjectionPoint, Object> availableObjects, @Nullable TestedClass testedClass, @Nonnull InjectionPoint injectionPoint
   ) {
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

   public void saveInstantiatedDependency(@Nonnull InjectionPoint dependencyKey, @Nonnull Object dependency) {
      instantiatedDependencies.put(dependencyKey, dependency);
   }

   public static void saveGlobalDependency(@Nonnull InjectionPoint dependencyKey, @Nonnull Object dependency) {
      globalDependencies.put(dependencyKey, dependency);
   }

   void clearTestedObjectsAndInstantiatedDependencies() {
      testedObjects.clear();
      instantiatedDependencies.clear();
   }

   @Nonnull
   BeanExporter getBeanExporter() {
      if (beanExporter == null) {
         beanExporter = new BeanExporter(this);
      }

      return beanExporter;
   }

   @Nullable
   public Class<?> resolveInterface(@Nonnull Class<?> anInterface) {
      return interfaceResolution.resolveInterface(anInterface, currentTestClassInstance);
   }
}
