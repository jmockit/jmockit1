/*
 * Copyright (c) 2006-2014 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.expectations.injection;

import java.lang.reflect.*;
import java.util.*;
import static java.lang.reflect.Modifier.*;

import mockit.internal.expectations.mocking.*;
import mockit.internal.state.*;
import static mockit.internal.expectations.injection.InjectionPoint.*;
import static mockit.internal.util.GeneratedClasses.*;

import org.jetbrains.annotations.*;

final class ConstructorSearch
{
   private static final int CONSTRUCTOR_ACCESS = PUBLIC + PROTECTED + PRIVATE;

   @NotNull private final InjectionState injectionState;
   @NotNull private final Class<?> testedClass;
   @NotNull private final String testedClassDesc;
   private List<MockedType> injectablesForConstructor;
   private Constructor<?> constructor;
   private StringBuilder searchResults;

   ConstructorSearch(@NotNull InjectionState injectionState, @NotNull Class<?> testedClass)
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

      if (INJECT_CLASS == null || !findSingleInjectAnnotatedConstructor(constructors)) {
         findSatisfiedConstructorWithMostParameters(constructors);
      }

      return constructor;
   }

   private boolean findSingleInjectAnnotatedConstructor(@NotNull Constructor<?>[] constructors)
   {
      for (Constructor<?> c : constructors) {
         if (c.isAnnotationPresent(INJECT_CLASS)) {
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

   private void findSatisfiedConstructorWithMostParameters(@NotNull Constructor<?>[] constructors)
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

   private static int constructorModifiers(@NotNull Constructor<?> c) { return CONSTRUCTOR_ACCESS & c.getModifiers(); }

   @Nullable
   private List<MockedType> findAvailableInjectablesForConstructor(@NotNull Constructor<?> candidate)
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

   private void printCandidateConstructorNameIfRequested(@NotNull Constructor<?> candidate)
   {
      if (searchResults != null) {
         searchResults.append("\r\n  ").append(candidate).append("\r\n");
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
   private MockedType hasInjectedValuesForVarargsParameter(@NotNull Type[] parameterTypes, int varargsParameterIndex)
   {
      Type varargsElementType = getTypeOfInjectionPointFromVarargsParameter(parameterTypes, varargsParameterIndex);
      injectionState.setTypeOfInjectionPoint(varargsElementType);
      return injectionState.findNextInjectableForInjectionPoint();
   }

   @NotNull
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
