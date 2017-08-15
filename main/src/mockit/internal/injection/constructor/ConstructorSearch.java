/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.injection.constructor;

import java.lang.annotation.*;
import java.lang.reflect.*;
import java.util.*;
import javax.annotation.*;
import static java.lang.reflect.Modifier.*;

import mockit.internal.injection.*;
import mockit.internal.state.*;
import mockit.internal.util.*;
import static mockit.internal.injection.InjectionPoint.*;

public final class ConstructorSearch
{
   private static final int CONSTRUCTOR_ACCESS = PUBLIC + PROTECTED + PRIVATE;

   @Nonnull private final InjectionState injectionState;
   @Nonnull private final TestedClass testedClass;
   @Nonnull private final String testedClassDesc;
   @Nonnull public List<InjectionProvider> parameterProviders;
   private final boolean withFullInjection;
   @Nullable private Constructor<?> constructor;
   @Nullable private StringBuilder searchResults;

   public ConstructorSearch(
      @Nonnull InjectionState injectionState, @Nonnull TestedClass testedClass, boolean withFullInjection)
   {
      this.injectionState = injectionState;
      this.testedClass = testedClass;
      Class<?> declaredClass = testedClass.getDeclaredClass();
      testedClassDesc = new ParameterNameExtractor().extractNames(declaredClass);
      parameterProviders = new ArrayList<InjectionProvider>();
      this.withFullInjection = withFullInjection;
   }

   @Nullable
   public Constructor<?> findConstructorToUse()
   {
      constructor = null;
      Class<?> declaredClass = testedClass.targetClass;
      Constructor<?>[] constructors = declaredClass.getDeclaredConstructors();

      if (!findSingleAnnotatedConstructor(constructors)) {
         findSatisfiedConstructorWithMostParameters(constructors);
      }

      return constructor;
   }

   private boolean findSingleAnnotatedConstructor(@Nonnull Constructor<?>[] constructors)
   {
      for (Constructor<?> c : constructors) {
         if (kindOfInjectionPoint(c) != KindOfInjectionPoint.NotAnnotated) {
            List<InjectionProvider> providersFound = findParameterProvidersForConstructor(c);

            if (providersFound != null) {
               parameterProviders = providersFound;
               constructor = c;
            }

            return true;
         }
      }

      return false;
   }

   private void findSatisfiedConstructorWithMostParameters(@Nonnull Constructor<?>[] constructors)
   {
      sortConstructorsWithMostAccessibleFirst(constructors);

      Constructor<?> unresolvedConstructor = null;
      List<InjectionProvider> incompleteProviders = null;

      for (Constructor<?> candidateConstructor : constructors) {
         List<InjectionProvider> providersFound = findParameterProvidersForConstructor(candidateConstructor);

         if (providersFound != null) {
            if (withFullInjection && containsUnresolvedProvider(providersFound)) {
               if (
                  unresolvedConstructor == null ||
                  isLargerConstructor(candidateConstructor, providersFound, unresolvedConstructor, incompleteProviders)
               ) {
                  unresolvedConstructor = candidateConstructor;
                  incompleteProviders = providersFound;
               }
            }
            else if (
               constructor == null ||
               isLargerConstructor(candidateConstructor, providersFound, constructor, parameterProviders)
            ) {
               constructor = candidateConstructor;
               parameterProviders = providersFound;
            }
         }
      }

      selectConstructorWithUnresolvedParameterIfMoreAccessible(unresolvedConstructor, incompleteProviders);
   }

   private static void sortConstructorsWithMostAccessibleFirst(@Nonnull Constructor<?>[] constructors)
   {
      if (constructors.length > 1) {
         Arrays.sort(constructors, CONSTRUCTOR_COMPARATOR);
      }
   }

   private static final Comparator<Constructor<?>> CONSTRUCTOR_COMPARATOR = new Comparator<Constructor<?>>() {
      @Override
      public int compare(Constructor<?> c1, Constructor<?> c2) { return compareAccessibility(c1, c2); }
   };

   private static int compareAccessibility(@Nonnull Constructor<?> c1, @Nonnull Constructor<?> c2)
   {
      int m1 = getModifiers(c1);
      int m2 = getModifiers(c2);
      if (m1 == m2) return 0;
      if (m1 == PUBLIC) return -1;
      if (m2 == PUBLIC) return 1;
      if (m1 == PROTECTED) return -1;
      if (m2 == PROTECTED) return 1;
      if (m2 == PRIVATE) return -1;
      return 1;
   }

   private static boolean containsUnresolvedProvider(@Nonnull List<InjectionProvider> providersFound)
   {
      for (InjectionProvider provider : providersFound) {
         if (provider instanceof ConstructorParameter && provider.getValue(null) == null) {
            return true;
         }
      }

      return false;
   }

   private boolean isLargerConstructor(
      @Nonnull Constructor<?> candidateConstructor, @Nonnull List<InjectionProvider> providersFound,
      @Nonnull Constructor<?> previousSatisfiableConstructor, @Nonnull List<InjectionProvider> previousProviders)
   {
      return
         getModifiers(candidateConstructor) == getModifiers(previousSatisfiableConstructor) &&
         providersFound.size() >= previousProviders.size();
   }

   private static int getModifiers(@Nonnull Constructor<?> c) { return CONSTRUCTOR_ACCESS & c.getModifiers(); }

   @Nullable
   private List<InjectionProvider> findParameterProvidersForConstructor(@Nonnull Constructor<?> candidate)
   {
      Type[] parameterTypes = candidate.getGenericParameterTypes();
      Annotation[][] parameterAnnotations = candidate.getParameterAnnotations();
      int n = parameterTypes.length;
      List<InjectionProvider> providersFound = new ArrayList<InjectionProvider>(n);
      boolean varArgs = candidate.isVarArgs();

      if (varArgs) {
         n--;
      }

      printCandidateConstructorNameIfRequested(candidate);

      String constructorDesc = "<init>" + mockit.external.asm.Type.getConstructorDescriptor(candidate);

      for (int i = 0; i < n; i++) {
         Type parameterType = parameterTypes[i];
         injectionState.setTypeOfInjectionPoint(parameterType);

         String parameterName = ParameterNames.getName(testedClassDesc, constructorDesc, i);
         Annotation[] appliedAnnotations = parameterAnnotations[i];
         InjectionProvider provider = findOrCreateInjectionProvider(parameterType, parameterName, appliedAnnotations);

         if (provider == null || providersFound.contains(provider)) {
            printParameterOfCandidateConstructorIfRequested(parameterName, provider);
            return null;
         }

         providersFound.add(provider);
      }

      if (varArgs) {
         Type parameterType = parameterTypes[n];
         InjectionProvider injectable = hasInjectedValuesForVarargsParameter(parameterType);

         if (injectable != null) {
            providersFound.add(injectable);
         }
      }

      return providersFound;
   }

   @Nullable
   private InjectionProvider findOrCreateInjectionProvider(
      @Nonnull Type parameterType, @Nullable String parameterName, @Nonnull Annotation[] parameterAnnotations)
   {
      String qualifiedName = getQualifiedName(parameterAnnotations);

      if (parameterName == null && qualifiedName == null) {
         return null;
      }

      boolean qualified = qualifiedName != null;
      String targetName = qualified ? qualifiedName : parameterName;
      InjectionProvider provider = injectionState.getProviderByTypeAndOptionallyName(targetName, testedClass);

      if (provider != null) {
         return provider;
      }

      InjectionPoint injectionPoint = new InjectionPoint(parameterType, targetName, qualifiedName);
      Object valueForParameter = injectionState.getTestedValue(testedClass, injectionPoint);

      if (valueForParameter == null && !withFullInjection) {
         return null;
      }

      return new ConstructorParameter(parameterType, parameterAnnotations, targetName, valueForParameter);
   }

   @Nullable
   private InjectionProvider hasInjectedValuesForVarargsParameter(@Nonnull Type parameterType)
   {
      Type varargsElementType = getTypeOfInjectionPointFromVarargsParameter(parameterType);
      injectionState.setTypeOfInjectionPoint(varargsElementType);
      return injectionState.findNextInjectableForInjectionPoint(testedClass);
   }

   private void selectConstructorWithUnresolvedParameterIfMoreAccessible(
      @Nullable Constructor<?> unresolvedConstructor, List<InjectionProvider> incompleteProviders)
   {
      if (
         unresolvedConstructor != null &&
         (constructor == null || compareAccessibility(unresolvedConstructor, constructor) < 0)
      ) {
         constructor = unresolvedConstructor;
         parameterProviders = incompleteProviders;
      }
   }

   // Methods used only when no satisfiable constructor is found //////////////////////////////////////////////////////

   @Nonnull
   public String getDescription()
   {
      searchResults = new StringBuilder();
      findConstructorToUse();
      String contents = searchResults.toString();
      searchResults = null;
      return contents;
   }

   private void printCandidateConstructorNameIfRequested(@Nonnull Constructor<?> candidate)
   {
      if (searchResults != null) {
         String constructorDesc = candidate.toGenericString().replace("java.lang.", "");
         searchResults.append("\r\n  ").append(constructorDesc).append("\r\n");
      }
   }

   private void printParameterOfCandidateConstructorIfRequested(
      @Nullable String parameterName, @Nullable InjectionProvider injectableFound)
   {
      if (searchResults != null) {
         searchResults.append("    disregarded because ");

         if (parameterName == null) {
            searchResults.append("parameter names are not available");
         }
         else {
            searchResults.append(
               "no tested/injectable value was found for parameter \"").append(parameterName).append('"');

            if (injectableFound != null) {
               searchResults.append(" that hadn't been used already");
            }
         }
      }
   }
}
