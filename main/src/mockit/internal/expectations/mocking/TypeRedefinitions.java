/*
 * Copyright (c) 2006-2014 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.expectations.mocking;

import java.lang.reflect.*;
import java.util.*;

import mockit.internal.state.*;
import static mockit.internal.util.Utilities.*;

import org.jetbrains.annotations.*;

public class TypeRedefinitions
{
   @NotNull private final List<Class<?>> targetClasses;
   @Nullable protected CaptureOfNewInstances captureOfNewInstances;
   protected int typesRedefined;

   protected TypeRedefinitions() { targetClasses = new ArrayList<Class<?>>(2); }

   protected final void addTargetClass(@NotNull MockedType mockedType)
   {
      Class<?> targetClass = mockedType.getClassType();

      if (targetClass != TypeVariable.class) {
         targetClasses.add(targetClass);
         addDuplicateTargetClassRepresentingMultipleCapturedSetsOfClasses(mockedType, targetClass);
      }
   }

   private void addDuplicateTargetClassRepresentingMultipleCapturedSetsOfClasses(
      @NotNull MockedType mockedType, @NotNull Class<?> targetClass)
   {
      int maxInstancesToCapture = mockedType.getMaxInstancesToCapture();

      if (maxInstancesToCapture > 0 && maxInstancesToCapture < Integer.MAX_VALUE) {
         targetClasses.add(targetClass);
      }
   }

   @NotNull public final List<Class<?>> getTargetClasses() { return targetClasses; }
   @Nullable public final CaptureOfNewInstances getCaptureOfNewInstances() { return captureOfNewInstances; }

   protected static void registerMock(@NotNull MockedType mockedType, @NotNull Object mock)
   {
      TestRun.getExecutingTest().registerMock(mockedType, mock);
   }

   protected final void ensureThatTargetClassesAreInitialized()
   {
      for (Class<?> targetClass : targetClasses) {
         ensureThatClassIsInitialized(targetClass);
      }
   }

   public void cleanUp()
   {
      if (captureOfNewInstances != null) {
         captureOfNewInstances.cleanUp();
         captureOfNewInstances = null;
      }
   }
}
