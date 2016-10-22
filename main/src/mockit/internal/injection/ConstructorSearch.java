/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.injection;

import java.lang.annotation.*;
import java.lang.reflect.*;
import java.util.*;
import javax.annotation.*;
import static java.lang.reflect.Modifier.*;

import mockit.internal.state.*;
import mockit.internal.util.*;
import static mockit.internal.injection.InjectionPoint.*;
import static mockit.internal.util.GeneratedClasses.*;

final class ConstructorSearch
{
   private static final int CONSTRUCTOR_ACCESS = PUBLIC + PROTECTED + PRIVATE;

   @Nonnull private final InjectionState injectionState;
   @Nonnull private final Class<?> testedClass;
   @Nonnull private final String testedClassDesc;
   @Nonnull List<InjectionPointProvider> parameterProviders;
   private final boolean withFullInjection;
   @Nullable private Constructor<?> constructor;
   @Nullable private StringBuilder searchResults;

   ConstructorSearch(@Nonnull InjectionState injectionState, @Nonnull Class<?> testedClass, boolean withFullInjection)
   {
      this.injectionState = injectionState;
      this.testedClass = testedClass;
      Class<?> declaredClass = isGeneratedClass(testedClass.getName()) ? testedClass.getSuperclass() : testedClass;
      testedClassDesc = new ParameterNameExtractor().extractNames(declaredClass);
      parameterProviders = new ArrayList<InjectionPointProvider>();
      this.withFullInjection = withFullInjection;
   }

   @Nullable
   Constructor<?> findConstructorToUse()
   {
      constructor = null;
      Constructor<?>[] constructors = testedClass.getDeclaredConstructors();

      if (!findSingleAnnotatedConstructor(constructors)) {
         findSatisfiedConstructorWithMostParameters(constructors);
      }

      return constructor;
   }

   private boolean findSingleAnnotatedConstructor(@Nonnull Constructor<?>[] constructors)
   {
      for (Constructor<?> c : constructors) {
         if (isAnnotated(c) != KindOfInjectionPoint.NotAnnotated) {
            List<InjectionPointProvider> providersFound = findParameterProvidersForConstructor(c);

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
      List<InjectionPointProvider> incompleteProviders = null;

      for (Constructor<?> candidateConstructor : constructors) {
         List<InjectionPointProvider> providersFound = findParameterProvidersForConstructor(candidateConstructor);

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

   private static boolean containsUnresolvedProvider(@Nonnull List<InjectionPointProvider> providersFound)
   {
      for (InjectionPointProvider provider : providersFound) {
         if (provider instanceof ConstructorParameter && provider.getValue(null) == null) {
            return true;
         }
      }

      return false;
   }

   private boolean isLargerConstructor(
      @Nonnull Constructor<?> candidateConstructor, @Nonnull List<InjectionPointProvider> providersFound,
      @Nonnull Constructor<?> previousSatisfiableConstructor, @Nonnull List<InjectionPointProvider> previousProviders)
   {
      return
         getModifiers(candidateConstructor) == getModifiers(previousSatisfiableConstructor) &&
         providersFound.size() >= previousProviders.size();
   }

   private static int getModifiers(@Nonnull Constructor<?> c) { return CONSTRUCTOR_ACCESS & c.getModifiers(); }

   @Nullable
   private List<InjectionPointProvider> findParameterProvidersForConstructor(@Nonnull Constructor<?> candidate)
   {
      Type[] parameterTypes = candidate.getGenericParameterTypes();
      Annotation[][] parameterAnnotations = candidate.getParameterAnnotations();
      int n = parameterTypes.length;
      List<InjectionPointProvider> providersFound = new ArrayList<InjectionPointProvider>(n);
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
         InjectionPointProvider provider =
            findOrCreateInjectionPointProvider(parameterType, parameterName, parameterAnnotations[i]);

         if (provider == null || providersFound.contains(provider)) {
            printParameterOfCandidateConstructorIfRequested(parameterName, provider);
            return null;
         }

         providersFound.add(provider);
      }

      if (varArgs) {
         Type parameterType = parameterTypes[n];
         InjectionPointProvider injectable = hasInjectedValuesForVarargsParameter(parameterType);

         if (injectable != null) {
            providersFound.add(injectable);
         }
      }

      return providersFound;
   }

   @Nullable
   private InjectionPointProvider findOrCreateInjectionPointProvider(
      @Nonnull Type parameterType, @Nullable String parameterName, @Nonnull Annotation[] parameterAnnotations)
   {
      if (parameterName == null) {
         return null;
      }

      InjectionPointProvider provider = injectionState.getProviderByTypeAndOptionallyName(parameterName);

      if (provider == null && withFullInjection) {
         Object valueForParameter = injectionState.getValueForParameterFromTestedField(parameterName);
         provider = new ConstructorParameter(parameterType, parameterAnnotations, parameterName, valueForParameter);
      }

      return provider;
   }

   @Nullable
   private InjectionPointProvider hasInjectedValuesForVarargsParameter(@Nonnull Type parameterType)
   {
      Type varargsElementType = getTypeOfInjectionPointFromVarargsParameter(parameterType);
      injectionState.setTypeOfInjectionPoint(varargsElementType);
      return injectionState.findNextInjectableForInjectionPoint();
   }

   private void selectConstructorWithUnresolvedParameterIfMoreAccessible(
      @Nullable Constructor<?> unresolvedConstructor, List<InjectionPointProvider> incompleteProviders)
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
   String getDescription()
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
      @Nullable String parameterName, @Nullable InjectionPointProvider injectableFound)
   {
      if (searchResults != null) {
         searchResults.append("    disregarded because ");

         if (parameterName == null) {
            searchResults.append("parameter names are not available");
         }
         else {
            searchResults.append("no injectable was found for parameter \"").append(parameterName).append('"');

            if (injectableFound != null) {
               searchResults.append(" that hadn't been used already");
            }
         }
      }
   }
}
