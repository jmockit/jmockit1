/*
 * Copyright (c) 2006-2014 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.expectations.mocking;

import java.lang.reflect.*;
import static java.lang.reflect.Modifier.*;

import org.jetbrains.annotations.*;

import static mockit.external.asm.Opcodes.*;

public abstract class FieldTypeRedefinitions extends TypeRedefinitions
{
   private static final int FIELD_ACCESS_MASK = ACC_SYNTHETIC + ACC_STATIC;

   protected FieldTypeRedefinitions(@NotNull Object objectWithMockFields) { super(objectWithMockFields); }

   protected final void redefineFieldTypes(@NotNull Class<?> classWithMockFields)
   {
      Class<?> superClass = classWithMockFields.getSuperclass();

      //noinspection UnnecessaryFullyQualifiedName
      if (
         superClass != null && superClass != Object.class &&
         superClass != mockit.Expectations.class && superClass != mockit.NonStrictExpectations.class
      ) {
         redefineFieldTypes(superClass);
      }

      Field[] fields = classWithMockFields.getDeclaredFields();

      for (Field candidateField : fields) {
         int fieldModifiers = candidateField.getModifiers();

         if ((fieldModifiers & FIELD_ACCESS_MASK) == 0) {
            redefineFieldType(candidateField, fieldModifiers);
         }
      }

      ensureThatTargetClassesAreInitialized();
   }

   private void redefineFieldType(@NotNull Field field, int modifiers)
   {
      MockedType mockedType = new MockedType(field);

      if (mockedType.isMockableType()) {
         redefineTypeForMockField(mockedType, field, isFinal(modifiers));
         typesRedefined++;

         registerCaptureOfNewInstances(mockedType);
      }
   }

   protected abstract void redefineTypeForMockField(
      @NotNull MockedType mockedType, @NotNull Field mockField, boolean isFinal);

   private void registerCaptureOfNewInstances(@NotNull MockedType mockedType)
   {
      if (mockedType.getMaxInstancesToCapture() <= 0) {
         return;
      }

      if (captureOfNewInstances == null) {
         captureOfNewInstances = new CaptureOfNewInstancesForFields();
      }

      captureOfNewInstances.registerCaptureOfNewInstances(mockedType, null);
   }

   /**
    * Returns true iff the mock instance concrete class is not mocked in some test, ie it's a class
    * which only appears in the code under test.
    */
   public abstract boolean captureNewInstanceForApplicableMockField(@NotNull Object mock);
}
