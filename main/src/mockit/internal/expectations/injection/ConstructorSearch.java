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

import org.jetbrains.annotations.*;

final class ConstructorSearch
{
   private static final int CONSTRUCTOR_ACCESS = PUBLIC + PROTECTED + PRIVATE;

   @NotNull private final InjectionState injectionState;
   @NotNull private final String testedClassDesc;
   private List<MockedType> injectablesForConstructor;
   private Constructor<?> constructor;

   ConstructorSearch(@NotNull InjectionState injectionState, @NotNull Class<?> testedClass)
   {
      this.injectionState = injectionState;
      testedClassDesc = new ParameterNameExtractor(false).extractNames(testedClass);
      injectablesForConstructor = new ArrayList<MockedType>();
   }

   @Nullable
   Constructor<?> findConstructorAccordingToAccessibilityAndAvailableInjectables(@NotNull Class<?> testedClass)
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

      for (Constructor<?> c : constructors) {
         List<MockedType> injectablesFound = findAvailableInjectablesForConstructor(c);

         if (
            injectablesFound != null &&
            (constructor == null ||
             constructorModifiers(c) == constructorModifiers(constructor) &&
             injectablesFound.size() >= injectablesForConstructor.size())
         ) {
            injectablesForConstructor = injectablesFound;
            constructor = c;
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

      String constructorDesc = "<init>" + mockit.external.asm4.Type.getConstructorDescriptor(candidate);

      for (int i = 0; i < n; i++) {
         injectionState.setTypeOfInjectionPoint(parameterTypes[i]);

         String parameterName = ParameterNames.getName(testedClassDesc, constructorDesc, i);
         MockedType injectable =
            parameterName == null ? null : injectionState.findInjectableByTypeAndOptionallyName(parameterName);

         if (injectable == null || injectablesFound.contains(injectable)) {
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

   @Nullable
   private MockedType hasInjectedValuesForVarargsParameter(@NotNull Type[] parameterTypes, int varargsParameterIndex)
   {
      Type varargsElementType = getTypeOfInjectionPointFromVarargsParameter(parameterTypes, varargsParameterIndex);
      injectionState.setTypeOfInjectionPoint(varargsElementType);
      return injectionState.findNextInjectableForInjectionPoint();
   }

   @NotNull
   List<MockedType> getInjectables() { return injectablesForConstructor; }
}
