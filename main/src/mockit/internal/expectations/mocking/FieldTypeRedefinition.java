/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.expectations.mocking;

import java.lang.reflect.*;
import javax.annotation.*;

import mockit.internal.state.*;

final class FieldTypeRedefinition extends TypeRedefinition
{
   private boolean usePartialMocking;

   FieldTypeRedefinition(@Nonnull MockedType typeMetadata) { super(typeMetadata); }

   boolean redefineTypeForTestedField()
   {
      usePartialMocking = true;
      return redefineTypeForFieldNotSet();
   }

   @Override
   void configureClassModifier(@Nonnull ExpectationsModifier modifier)
   {
      if (usePartialMocking) {
         modifier.useDynamicMocking(true);
      }
   }

   @SuppressWarnings("ConstantConditions")
   boolean redefineTypeForFinalField()
   {
      if (targetClass == TypeVariable.class || !typeMetadata.injectable && targetClass.isInterface()) {
         String mockFieldName = typeMetadata.getName();
         throw new IllegalArgumentException("Final mock field \"" + mockFieldName + "\" must be of a class type");
      }

      return redefineTypeForFieldNotSet();
   }

   private boolean redefineTypeForFieldNotSet()
   {
      boolean redefined = redefineMethodsAndConstructorsInTargetType();

      if (redefined) {
         TestRun.mockFixture().registerMockedClass(targetClass);
      }

      return redefined;
   }
}
