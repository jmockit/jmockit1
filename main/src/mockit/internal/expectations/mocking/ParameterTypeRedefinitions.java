/*
 * Copyright (c) 2006-2015 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.expectations.mocking;

import java.lang.annotation.*;
import java.lang.reflect.*;
import java.util.*;
import javax.annotation.*;

import mockit.internal.state.*;

public final class ParameterTypeRedefinitions extends TypeRedefinitions
{
   @Nonnull private final Type[] paramTypes;
   @Nonnull private final Annotation[][] paramAnnotations;
   @Nonnull private final Object[] paramValues;
   @Nonnull private final MockedType[] mockParameters;
   @Nonnull private final List<MockedType> injectableParameters;

   public ParameterTypeRedefinitions(@Nonnull Method testMethod, @Nullable Object[] parameterValues)
   {
      TestRun.enterNoMockingZone();

      try {
         paramTypes = testMethod.getGenericParameterTypes();
         paramAnnotations = testMethod.getParameterAnnotations();
         int n = paramTypes.length;
         paramValues = parameterValues == null || parameterValues.length != n ? new Object[n] : parameterValues;
         mockParameters = new MockedType[n];
         injectableParameters = new ArrayList<MockedType>(n);

         String testClassDesc = mockit.external.asm.Type.getInternalName(testMethod.getDeclaringClass());
         String testMethodDesc = testMethod.getName() + mockit.external.asm.Type.getMethodDescriptor(testMethod);

         for (int i = 0; i < n; i++) {
            getMockedTypeFromMockParameterDeclaration(testClassDesc, testMethodDesc, i);
         }

         InstanceFactory[] instanceFactories = redefineMockedTypes();
         ensureThatTargetClassesAreInitialized();
         instantiateMockedTypes(instanceFactories);
      }
      finally {
         TestRun.exitNoMockingZone();
      }
   }

   private void getMockedTypeFromMockParameterDeclaration(
      @Nonnull String testClassDesc, @Nonnull String testMethodDesc, int paramIndex)
   {
      Type paramType = paramTypes[paramIndex];
      Annotation[] annotationsOnParameter = paramAnnotations[paramIndex];

      MockedType mockedType =
         new MockedType(testClassDesc, testMethodDesc, paramIndex, paramType, annotationsOnParameter);
      mockParameters[paramIndex] = mockedType;

      if (mockedType.injectable) {
         injectableParameters.add(mockedType);
         paramValues[paramIndex] = mockedType.providedValue;
      }
   }

   @Nonnull
   private InstanceFactory[] redefineMockedTypes()
   {
      int n = mockParameters.length;
      InstanceFactory[] instanceFactories = new InstanceFactory[n];

      for (int i = 0; i < n; i++) {
         MockedType mockedType = mockParameters[i];

         if (mockedType.isMockableType()) {
            instanceFactories[i] = redefineMockedType(mockedType);
         }
      }

      return instanceFactories;
   }

   @Nullable
   private InstanceFactory redefineMockedType(@Nonnull MockedType mockedType)
   {
      TypeRedefinition typeRedefinition = new TypeRedefinition(mockedType);
      InstanceFactory instanceFactory = typeRedefinition.redefineType();

      if (instanceFactory != null) {
         addTargetClass(mockedType);
      }

      return instanceFactory;
   }

   private void registerCaptureOfNewInstances(@Nonnull MockedType mockedType, @Nonnull Object originalInstance)
   {
      if (captureOfNewInstances == null) {
         captureOfNewInstances = new CaptureOfNewInstances();
      }

      captureOfNewInstances.registerCaptureOfNewInstances(mockedType, originalInstance);
      captureOfNewInstances.makeSureAllSubtypesAreModified(mockedType);
   }

   private void instantiateMockedTypes(@Nonnull InstanceFactory[] instanceFactories)
   {
      for (int i = 0; i < instanceFactories.length; i++) {
         InstanceFactory instanceFactory = instanceFactories[i];

         if (instanceFactory != null) {
            MockedType mockedType = mockParameters[i];
            Object mockedInstance = instantiateMockedType(mockedType, instanceFactory);
            paramValues[i] = mockedInstance;
            mockedType.providedValue = mockedInstance;
         }
      }
   }

   @Nonnull
   private Object instantiateMockedType(@Nonnull MockedType mockedType, @Nonnull InstanceFactory instanceFactory)
   {
      Object mock = instanceFactory.create();
      registerMock(mockedType, mock);

      if (mockedType.withInstancesToCapture()) {
         registerCaptureOfNewInstances(mockedType, mock);
      }

      return mock;
   }

   @Nonnull public List<MockedType> getInjectableParameters() { return injectableParameters; }
   @Nonnull public Object[] getParameterValues() { return paramValues; }
}
