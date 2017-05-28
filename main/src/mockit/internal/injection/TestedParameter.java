/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.injection;

import javax.annotation.*;

import mockit.*;
import mockit.internal.state.*;
import mockit.internal.util.*;

final class TestedParameter extends TestedObject
{
   @Nonnull private final TestMethod testMethod;
   @Nonnegative private final int parameterIndex;

   TestedParameter(
      @Nonnull InjectionState injectionState, @Nonnull TestMethod testMethod, @Nonnegative int parameterIndex,
      @Nonnull Tested metadata)
   {
      super(
         injectionState, metadata, ParameterNames.getName(testMethod, parameterIndex),
         testMethod.getParameterType(parameterIndex), testMethod.getParameterClass(parameterIndex));
      this.testMethod = testMethod;
      this.parameterIndex = parameterIndex;
   }

   @Nullable @Override
   Object getExistingTestedInstanceIfApplicable(@Nonnull Object testClassInstance)
   {
      Object testedObject = null;

      if (!createAutomatically) {
         String providedValue = metadata.value();

         if (!providedValue.isEmpty()) {
            Class<?> parameterClass = testMethod.getParameterClass(parameterIndex);
            testedObject = Utilities.convertFromString(parameterClass, providedValue);

            if (testedObject != null) {
               testMethod.setParameterValue(parameterIndex, testedObject);
            }
         }

         createAutomatically = testedObject == null;
      }

      return testedObject;
   }

   @Override
   void setInstance(@Nonnull Object testClassInstance, @Nullable Object testedInstance)
   {
      testMethod.setParameterValue(parameterIndex, testedInstance);
   }
}
