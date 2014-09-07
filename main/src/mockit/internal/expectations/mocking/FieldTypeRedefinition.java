/*
 * Copyright (c) 2006-2014 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.expectations.mocking;

import java.lang.reflect.*;

import mockit.internal.state.*;

import org.jetbrains.annotations.*;

final class FieldTypeRedefinition extends TypeRedefinition
{
   private boolean usePartialMocking;

   FieldTypeRedefinition(@NotNull MockedType typeMetadata) { super(typeMetadata); }

   boolean redefineTypeForTestedField()
   {
      usePartialMocking = true;
      return redefineTypeForFieldNotSet();
   }

   @Override
   void configureClassModifier(@NotNull ExpectationsModifier modifier)
   {
      if (usePartialMocking) {
         modifier.useDynamicMocking(true);
      }
   }

   boolean redefineTypeForFinalField()
   {
      if (targetClass == TypeVariable.class || !typeMetadata.injectable && targetClass.isInterface()) {
         throw new IllegalArgumentException("Final mock field \"" + typeMetadata.mockId + "\" must be of a class type");
      }

      return redefineTypeForFieldNotSet();
   }

   private boolean redefineTypeForFieldNotSet()
   {
      typeMetadata.buildMockingConfiguration();
      boolean redefined = redefineMethodsAndConstructorsInTargetType();

      if (redefined) {
         TestRun.mockFixture().registerMockedClass(targetClass);
      }

      return redefined;
   }
}
