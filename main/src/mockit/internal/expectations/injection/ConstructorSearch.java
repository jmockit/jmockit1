/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.expectations.injection;

import java.lang.annotation.*;
import java.lang.reflect.*;
import java.util.*;
import javax.annotation.*;
import static java.lang.reflect.Modifier.*;

import mockit.internal.expectations.mocking.*;
import mockit.internal.state.*;
import static mockit.internal.expectations.injection.InjectionPoint.*;
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
   Constructor<?> findConstructorAccordingToAccessibilityAndAvailableInjectables()
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
      if (constructors.length > 1) {
         sortConstructorsWithMostAccessibleFirst(constructors);
      }

      for (Constructor<?> candidateConstructor : constructors) {
         List<InjectionPointProvider> providersFound = findParameterProvidersForConstructor(candidateConstructor);

         if (
            providersFound != null &&
            (constructor == null ||
             constructorModifiers(candidateConstructor) == constructorModifiers(constructor) &&
             providersFound.size() >= parameterProviders.size())
         ) {
            parameterProviders = providersFound;
            constructor = candidateConstructor;
         }
      }
   }

   private static void sortConstructorsWithMostAccessibleFirst(@Nonnull Constructor<?>[] constructors)
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
   }

   private static int constructorModifiers(@Nonnull Constructor<?> c) { return CONSTRUCTOR_ACCESS & c.getModifiers(); }

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
            parameterName == null ? null : injectionState.findInjectableByTypeAndOptionallyName(parameterName);

         // TODO: for issue #203
//         if (injectable == null && parameterType instanceof ParameterizedType) {
//            ParameterizedType genericParameterType = (ParameterizedType) parameterType;
//            Class<?> parameterClass = (Class<?>) genericParameterType.getRawType();
//
//            if (Iterable.class.isAssignableFrom(parameterClass)) {
//               Type elementType = genericParameterType.getActualTypeArguments()[0];
//               injectionState.setTypeOfInjectionPoint(elementType);
//
//               List<MockedType> injectablesOfSameType = injectionState.findInjectablesByType();
//
//               if (!injectablesOfSameType.isEmpty()) {
//                  providersFound.addAll(injectablesOfSameType);
//                  continue;
//               }
//            }
//         }

         if (parameterName != null && provider == null && withFullInjection) {
            provider = new ConstructorParameter(parameterType, parameterAnnotations[i], parameterName);
         }

         if (provider == null || providersFound.contains(provider)) {
            printParameterOfCandidateConstructorIfRequested(parameterName, provider);
            return null;
         }

         providersFound.add(provider);
      }

      if (varArgs) {
         Type parameterType = parameterTypes[n];
         MockedType injectable = hasInjectedValuesForVarargsParameter(parameterType);

         if (injectable != null) {
            providersFound.add(injectable);
         }
      }

      return providersFound;
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

   @Nullable
   private MockedType hasInjectedValuesForVarargsParameter(@Nonnull Type parameterType)
   {
      Type varargsElementType = getTypeOfInjectionPointFromVarargsParameter(parameterType);
      injectionState.setTypeOfInjectionPoint(varargsElementType);
      return injectionState.findNextInjectableForInjectionPoint();
   }

   @Override
   public String toString()
   {
      searchResults = new StringBuilder();
      findConstructorAccordingToAccessibilityAndAvailableInjectables();
      String contents = searchResults.toString();
      searchResults = null;
      return contents;
   }
}
