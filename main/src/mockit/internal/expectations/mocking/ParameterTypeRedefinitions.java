/*
 * Copyright (c) 2006-2014 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.expectations.mocking;

import java.lang.annotation.*;
import java.lang.reflect.*;
import java.util.*;

import org.jetbrains.annotations.*;

import mockit.internal.state.*;

public final class ParameterTypeRedefinitions extends TypeRedefinitions
{
   @NotNull private final Type[] paramTypes;
   @NotNull private final Annotation[][] paramAnnotations;
   @NotNull private final Object[] paramValues;
   @NotNull private final MockedType[] mockParameters;
   @NotNull private final List<MockedType> injectableParameters;

   public ParameterTypeRedefinitions(
      @NotNull Object owner, @NotNull Method testMethod, @Nullable Object[] parameterValues)
   {
      super(owner);

      TestRun.enterNoMockingZone();

      try {
         paramTypes = testMethod.getGenericParameterTypes();
         paramAnnotations = testMethod.getParameterAnnotations();
         int n = paramTypes.length;
         paramValues = parameterValues == null || parameterValues.length != n ? new Object[n] : parameterValues;
         mockParameters = new MockedType[n];
         injectableParameters = new ArrayList<MockedType>(n);

         String testClassDesc = mockit.external.asm4.Type.getInternalName(testMethod.getDeclaringClass());
         String testMethodDesc = testMethod.getName() + mockit.external.asm4.Type.getMethodDescriptor(testMethod);

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
      @NotNull String testClassDesc, @NotNull String testMethodDesc, int paramIndex)
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

   @NotNull private InstanceFactory[] redefineMockedTypes()
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

   @NotNull private InstanceFactory redefineMockedType(@NotNull MockedType mockedType)
   {
      TypeRedefinition typeRedefinition = new TypeRedefinition(mockedType);
      InstanceFactory instanceFactory = typeRedefinition.redefineType();

      addTargetClass(mockedType);
      typesRedefined++;

      return instanceFactory;
   }

   private void registerCaptureOfNewInstances(@NotNull MockedType mockedType, @NotNull Object originalInstance)
   {
      if (captureOfNewInstances == null) {
         captureOfNewInstances = new CaptureOfNewInstances();
      }

      captureOfNewInstances.registerCaptureOfNewInstances(mockedType, originalInstance);
      captureOfNewInstances.makeSureAllSubtypesAreModified(mockedType.getClassType());
   }

   private void instantiateMockedTypes(@NotNull InstanceFactory[] instanceFactories)
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

   @NotNull
   private Object instantiateMockedType(@NotNull MockedType mockedType, @NotNull InstanceFactory instanceFactory)
   {
      Object mock = instanceFactory.create();
      registerMock(mockedType, mock);

      if (mockedType.withInstancesToCapture()) {
         registerCaptureOfNewInstances(mockedType, mock);
      }

      return mock;
   }

   @NotNull public List<MockedType> getInjectableParameters() { return injectableParameters; }
   @NotNull public Object[] getParameterValues() { return paramValues; }
}
