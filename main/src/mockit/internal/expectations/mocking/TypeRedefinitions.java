/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.expectations.mocking;

import java.lang.reflect.*;
import java.util.*;
import javax.annotation.*;

import mockit.internal.state.*;

public class TypeRedefinitions
{
   @Nonnull private final List<Class<?>> targetClasses;
   @Nullable protected CaptureOfNewInstances captureOfNewInstances;

   protected TypeRedefinitions() { targetClasses = new ArrayList<Class<?>>(2); }

   protected final void addTargetClass(@Nonnull MockedType mockedType)
   {
      Class<?> targetClass = mockedType.getClassType();

      if (targetClass != TypeVariable.class) {
         targetClasses.add(targetClass);
         addDuplicateTargetClassRepresentingMultipleCapturedSetsOfClasses(mockedType, targetClass);
      }
   }

   private void addDuplicateTargetClassRepresentingMultipleCapturedSetsOfClasses(
      @Nonnull MockedType mockedType, @Nonnull Class<?> targetClass)
   {
      int maxInstancesToCapture = mockedType.getMaxInstancesToCapture();

      if (maxInstancesToCapture > 0 && maxInstancesToCapture < Integer.MAX_VALUE) {
         targetClasses.add(targetClass);
      }
   }

   @Nonnull public final List<Class<?>> getTargetClasses() { return targetClasses; }
   @Nullable public final CaptureOfNewInstances getCaptureOfNewInstances() { return captureOfNewInstances; }

   protected static void registerMock(@Nonnull MockedType mockedType, @Nonnull Object mock)
   {
      TestRun.getExecutingTest().registerMock(mockedType, mock);
   }

   public void cleanUp()
   {
      if (captureOfNewInstances != null) {
         captureOfNewInstances.cleanUp();
         captureOfNewInstances = null;
      }
   }
}
