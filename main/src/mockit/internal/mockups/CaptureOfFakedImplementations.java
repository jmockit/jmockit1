/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.mockups;

import java.lang.reflect.Type;
import javax.annotation.*;

import mockit.*;
import mockit.external.asm.*;
import mockit.internal.*;
import mockit.internal.capturing.*;
import static mockit.internal.util.Utilities.getClassType;

public final class CaptureOfFakedImplementations extends CaptureOfImplementations<Void>
{
   private final MockClassSetup mockClassSetup;

   public CaptureOfFakedImplementations(@Nonnull MockUp<?> mockUp, @Nonnull Type baseType)
   {
      Class<?> baseClassType = getClassType(baseType);
      mockClassSetup = new MockClassSetup(baseClassType, baseType, mockUp, null);
   }

   @Nonnull @Override
   protected BaseClassModifier createModifier(
      @Nullable ClassLoader cl, @Nonnull ClassReader cr, @Nonnull Class<?> baseType, Void typeMetadata)
   {
      return mockClassSetup.createClassModifier(cr);
   }

   @Override
   protected void redefineClass(@Nonnull Class<?> realClass, @Nonnull byte[] modifiedClass)
   {
      mockClassSetup.applyClassModifications(realClass, modifiedClass);
   }

   @Nullable
   public <T> Class<T> apply()
   {
      @SuppressWarnings("unchecked") Class<T> baseType = (Class<T>) mockClassSetup.realClass;
      Class<T> baseClassType = baseType;
      Class<T> fakedClass = null;

      if (baseType.isInterface()) {
         fakedClass = new FakedImplementationClass<T>(mockClassSetup.mockUp).createImplementation(baseType);
         baseClassType = fakedClass;
      }

      if (baseClassType != Object.class) {
         redefineClass(baseClassType, baseType, null);
      }

      makeSureAllSubtypesAreModified(baseType, false, null);
      return fakedClass;
   }
}
