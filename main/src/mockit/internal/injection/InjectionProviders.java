/*
 * Copyright (c) 2006 JMockit developers
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.injection;

import java.lang.reflect.*;
import java.util.*;
import javax.annotation.*;
import javax.inject.*;

import mockit.internal.reflection.*;
import static mockit.internal.injection.InjectionPoint.INJECT_CLASS;
import static mockit.internal.util.Utilities.getClassType;

public final class InjectionProviders
{
   @Nonnull private List<InjectionProvider> injectables;
   @Nonnull private List<InjectionProvider> consumedInjectionProviders;
   private Type typeOfInjectionPoint;

   InjectionProviders(@Nonnull LifecycleMethods lifecycleMethods) {
      injectables = Collections.emptyList();
      consumedInjectionProviders = new ArrayList<>();
   }

   boolean setInjectables(@SuppressWarnings("ParameterHidesMemberVariable") @Nonnull List<? extends InjectionProvider> injectables) {
      if (injectables.isEmpty()) {
         this.injectables = Collections.emptyList();
         return false;
      }

      this.injectables = new ArrayList<>(injectables);
      return true;
   }

   @Nonnull
   List<InjectionProvider> addInjectables(@Nonnull List<? extends InjectionProvider> injectablesToAdd) {
      if (!injectablesToAdd.isEmpty()) {
         if (injectables.isEmpty()) {
            injectables = new ArrayList<>(injectablesToAdd);
         }
         else {
            injectables.addAll(injectablesToAdd);
         }
      }

      return injectables;
   }

   public void setTypeOfInjectionPoint(@Nonnull Type typeOfInjectionPoint) { this.typeOfInjectionPoint = typeOfInjectionPoint; }

   @Nullable
   public InjectionProvider getProviderByTypeAndOptionallyName(@Nonnull String nameOfInjectionPoint, @Nonnull TestedClass testedClass) {
      Type elementTypeOfIterable = getElementTypeIfIterable(typeOfInjectionPoint);

      if (elementTypeOfIterable != null) {
         return findInjectablesByTypeOnly(elementTypeOfIterable, testedClass);
      }

      return findInjectableByTypeAndOptionallyName(nameOfInjectionPoint, testedClass);
   }

   @Nullable
   private static Type getElementTypeIfIterable(@Nonnull Type injectableType) {
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
   public InjectionProvider findNextInjectableForInjectionPoint(@Nonnull TestedClass testedClass) {
      for (InjectionProvider injectable : injectables) {
         if (hasTypeAssignableToInjectionPoint(injectable, testedClass) && !consumedInjectionProviders.contains(injectable)) {
            return injectable;
         }
      }

      return null;
   }

   private boolean hasTypeAssignableToInjectionPoint(@Nonnull InjectionProvider injectable, @Nonnull TestedClass testedClass) {
      Type declaredType = injectable.getDeclaredType();
      return isAssignableToInjectionPoint(declaredType, testedClass);
   }

   boolean isAssignableToInjectionPoint(@Nonnull Type injectableType, @Nonnull TestedClass testedClass) {
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
   private InjectionProvider findInjectablesByTypeOnly(@Nonnull Type elementType, @Nonnull TestedClass testedClass) {
      GenericTypeReflection typeReflection = testedClass.reflection;
      MultiValuedProvider found = null;

      for (InjectionProvider injectable : injectables) {
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
   private InjectionProvider findInjectableByTypeAndOptionallyName(@Nonnull String nameOfInjectionPoint, @Nonnull TestedClass testedClass) {
      InjectionProvider foundInjectable = null;

      for (InjectionProvider injectable : injectables) {
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
   InjectionProvider findInjectableByTypeAndName(@Nonnull String nameOfInjectionPoint, @Nonnull TestedClass testedClass) {
      for (InjectionProvider injectable : injectables) {
         if (hasTypeAssignableToInjectionPoint(injectable, testedClass) && nameOfInjectionPoint.equals(injectable.getName())) {
            return injectable;
         }
      }

      return null;
   }

   @Nullable
   Object getValueToInject(@Nonnull InjectionProvider injectionProvider, @Nullable Object currentTestClassInstance) {
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
   public List<InjectionProvider> saveConsumedInjectionProviders() {
      List<InjectionProvider> previouslyConsumed = consumedInjectionProviders;
      consumedInjectionProviders = new ArrayList<>();
      return previouslyConsumed;
   }

   public void restoreConsumedInjectionProviders(@Nonnull List<InjectionProvider> previouslyConsumed) {
      consumedInjectionProviders = previouslyConsumed;
   }
}
