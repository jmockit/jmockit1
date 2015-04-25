/*
 * Copyright (c) 2006-2015 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.expectations.injection;

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
   private List<MockedType> injectablesForConstructor;
   private Constructor<?> constructor;
   private StringBuilder searchResults;

   ConstructorSearch(@Nonnull InjectionState injectionState, @Nonnull Class<?> testedClass)
   {
      this.injectionState = injectionState;
      this.testedClass = testedClass;
      Class<?> declaredClass = isGeneratedClass(testedClass.getName()) ? testedClass.getSuperclass() : testedClass;
      testedClassDesc = new ParameterNameExtractor(false).extractNames(declaredClass);
      injectablesForConstructor = new ArrayList<MockedType>();
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

   private void findSatisfiedConstructorWithMostParameters(@Nonnull Constructor<?>[] constructors)
   {
      if (constructors.length > 1) {
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

      for (Constructor<?> candidateConstructor : constructors) {
         List<MockedType> injectablesFound = findAvailableInjectablesForConstructor(candidateConstructor);

         if (
            injectablesFound != null &&
            (constructor == null ||
             constructorModifiers(candidateConstructor) == constructorModifiers(constructor) &&
             injectablesFound.size() >= injectablesForConstructor.size())
         ) {
            injectablesForConstructor = injectablesFound;
            constructor = candidateConstructor;
         }
      }
   }

   private static int constructorModifiers(@Nonnull Constructor<?> c) { return CONSTRUCTOR_ACCESS & c.getModifiers(); }

   @Nullable
   private List<MockedType> findAvailableInjectablesForConstructor(@Nonnull Constructor<?> candidate)
   {
      Type[] parameterTypes = candidate.getGenericParameterTypes();
      int n = parameterTypes.length;
      List<MockedType> injectablesFound = new ArrayList<MockedType>(n);
      boolean varArgs = candidate.isVarArgs();

      if (varArgs) {
         n--;
      }

      printCandidateConstructorNameIfRequested(candidate);

      String constructorDesc = "<init>" + mockit.external.asm.Type.getConstructorDescriptor(candidate);

      for (int i = 0; i < n; i++) {
         injectionState.setTypeOfInjectionPoint(parameterTypes[i]);

         String parameterName = ParameterNames.getName(testedClassDesc, constructorDesc, i);
         MockedType injectable =
            parameterName == null ? null : injectionState.findInjectableByTypeAndOptionallyName(parameterName);

         if (injectable == null || injectablesFound.contains(injectable)) {
            printParameterOfCandidateConstructorIfRequested(parameterName, injectable);
            return null;
         }

         injectablesFound.add(injectable);
      }

      if (varArgs) {
         MockedType injectable = hasInjectedValuesForVarargsParameter(parameterTypes, n);

         if (injectable != null) {
            injectablesFound.add(injectable);
         }
      }

      return injectablesFound;
   }

   private void printCandidateConstructorNameIfRequested(@Nonnull Constructor<?> candidate)
   {
      if (searchResults != null) {
         String constructorDesc = candidate.toGenericString().replace("java.lang.", "");
         searchResults.append("\r\n  ").append(constructorDesc).append("\r\n");
      }
   }

   private void printParameterOfCandidateConstructorIfRequested(
      @Nullable String parameterName, @Nullable MockedType injectableFound)
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
   private MockedType hasInjectedValuesForVarargsParameter(@Nonnull Type[] parameterTypes, int varargsParameterIndex)
   {
      Type varargsElementType = getTypeOfInjectionPointFromVarargsParameter(parameterTypes, varargsParameterIndex);
      injectionState.setTypeOfInjectionPoint(varargsElementType);
      return injectionState.findNextInjectableForInjectionPoint();
   }

   @Nonnull
   List<MockedType> getInjectables() { return injectablesForConstructor; }

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
