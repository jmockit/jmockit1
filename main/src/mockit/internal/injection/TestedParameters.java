/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.injection;

import java.lang.annotation.*;
import javax.annotation.*;

import mockit.*;
import mockit.internal.expectations.mocking.*;
import mockit.internal.state.*;
import mockit.internal.util.*;

public final class TestedParameters
{
   @Nonnull private final TestMethod testMethod;
   @Nonnull private final InjectionState injectionState;

   public TestedParameters(@Nonnull TestMethod testMethod)
   {
      this.testMethod = testMethod;

      TestedClassInstantiations testedClasses = TestRun.getTestedClassInstantiations();
      injectionState = testedClasses == null ? new InjectionState() : testedClasses.injectionState;
   }

   public void createTestedParameters(
      @Nonnull Object testClassInstance, @Nonnull ParameterTypeRedefinitions paramTypeRedefs)
   {
      injectionState.buildListsOfInjectables(testClassInstance, paramTypeRedefs);

      for (int n = testMethod.getParameterCount(), i = 0; i < n; i++) {
         TestedParameter testedParameter = createTestedParameterIfApplicable(i);

         if (testedParameter != null) {
            instantiateTestedObject(testClassInstance, testedParameter);
         }
      }
   }

   @Nullable
   private TestedParameter createTestedParameterIfApplicable(@Nonnegative int parameterIndex)
   {
      Annotation[] parameterAnnotations = testMethod.getParameterAnnotations(parameterIndex);

      for (Annotation parameterAnnotation : parameterAnnotations) {
         Tested testedMetadata = TestedObject.getTestedAnnotationIfPresent(parameterAnnotation);

         if (testedMetadata != null) {
            return new TestedParameter(injectionState, testMethod, parameterIndex, testedMetadata);
         }
      }

      return null;
   }

   private void instantiateTestedObject(@Nonnull Object testClassInstance, @Nonnull TestedParameter testedObject)
   {
      try {
         testedObject.instantiateWithInjectableValues(testClassInstance);
      }
      finally {
         injectionState.resetConsumedInjectionProviders();
      }
   }
}
