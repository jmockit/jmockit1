/*
 * Copyright (c) 2006-2014 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.expectations.mocking;

import java.util.*;
import java.util.Map.*;
import java.lang.reflect.*;

import org.jetbrains.annotations.*;

import mockit.internal.expectations.injection.*;
import mockit.internal.state.*;
import mockit.internal.util.*;

public final class SharedFieldTypeRedefinitions extends FieldTypeRedefinitions
{
   @NotNull private final Map<MockedType, InstanceFactory> mockInstanceFactories;
   @NotNull private final List<MockedType> finalMockFields;
   @Nullable private TestedClassInstantiations testedClassInstantiations;

   public SharedFieldTypeRedefinitions(@NotNull Object objectWithMockFields)
   {
      super(objectWithMockFields);
      mockInstanceFactories = new HashMap<MockedType, InstanceFactory>();
      finalMockFields = new ArrayList<MockedType>();
   }

   public void redefineTypesForTestClass()
   {
      Class<?> testClass = parentObject.getClass();
      TestRun.enterNoMockingZone();

      try {
         testedClassInstantiations = new TestedClassInstantiations();

         if (!testedClassInstantiations.findTestedAndInjectableFields(testClass)) {
            testedClassInstantiations = null;
         }

         clearTargetClasses();
         redefineFieldTypes(testClass);
      }
      finally {
         TestRun.exitNoMockingZone();
      }
   }

   @Override
   protected void redefineTypeForMockField(@NotNull MockedType mockedType, @NotNull Field mockField, boolean isFinal)
   {
      TypeRedefinition typeRedefinition = new TypeRedefinition(mockedType);
      boolean redefined;

      if (isFinal) {
         redefined = typeRedefinition.redefineTypeForFinalField();

         if (redefined) {
            finalMockFields.add(mockedType);
         }
      }
      else {
         InstanceFactory factory = typeRedefinition.redefineType();
         redefined = factory != null;

         if (redefined) {
            mockInstanceFactories.put(mockedType, factory);
         }
      }

      if (redefined) {
         addTargetClass(mockedType);
      }
   }

   public void assignNewInstancesToMockFields(@NotNull Object target)
   {
      TestRun.getExecutingTest().clearInjectableAndNonStrictMocks();

      for (Entry<MockedType, InstanceFactory> metadataAndFactory : mockInstanceFactories.entrySet()) {
         MockedType mockedType = metadataAndFactory.getKey();
         InstanceFactory instanceFactory = metadataAndFactory.getValue();

         Object mock = assignNewInstanceToMockField(target, mockedType, instanceFactory);
         registerMock(mockedType, mock);
      }

      obtainAndRegisterInstancesOfFinalFields(target);
   }

   private void obtainAndRegisterInstancesOfFinalFields(@NotNull Object target)
   {
      for (MockedType metadata : finalMockFields) {
         assert metadata.field != null;
         Object mock = FieldReflection.getFieldValue(metadata.field, target);

         if (mock != null) {
            registerMock(metadata, mock);
         }
      }
   }

   @NotNull
   private Object assignNewInstanceToMockField(
      @NotNull Object target, @NotNull MockedType mockedType, @NotNull InstanceFactory instanceFactory)
   {
      Field mockField = mockedType.field;
      assert mockField != null;
      Object mock = FieldReflection.getFieldValue(mockField, target);

      if (mock == null) {
         try {
            mock = instanceFactory.create();
         }
         catch (NoClassDefFoundError e) {
            StackTrace.filterStackTrace(e);
            e.printStackTrace();
            throw e;
         }
         catch (ExceptionInInitializerError e) {
            StackTrace.filterStackTrace(e);
            e.printStackTrace();
            throw e;
         }

         FieldReflection.setFieldValue(mockField, target, mock);

         if (mockedType.getMaxInstancesToCapture() > 0) {
            assert captureOfNewInstances != null;
            CaptureOfNewInstancesForFields capture = (CaptureOfNewInstancesForFields) captureOfNewInstances;
            capture.resetCaptureCount(mockField);
         }
      }

      return mock;
   }

   @Override
   public boolean captureNewInstanceForApplicableMockField(@NotNull Object mock)
   {
      if (captureOfNewInstances == null) {
         return false;
      }

      Object fieldOwner = TestRun.getCurrentTestInstance();
      return captureOfNewInstances.captureNewInstance(fieldOwner, mock);
   }

   @Nullable public TestedClassInstantiations getTestedClassInstantiations() { return testedClassInstantiations; }

   @Override
   public void cleanUp()
   {
      TestRun.getExecutingTest().clearCascadingTypes();
      super.cleanUp();
   }
}
