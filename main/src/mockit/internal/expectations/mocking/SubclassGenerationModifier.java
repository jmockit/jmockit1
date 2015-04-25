/*
 * Copyright (c) 2006-2015 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.expectations.mocking;

import java.lang.reflect.Type;
import javax.annotation.*;

import mockit.external.asm.*;
import mockit.internal.classGeneration.*;
import mockit.internal.util.*;
import static mockit.external.asm.Opcodes.*;
import static mockit.internal.expectations.mocking.MockedTypeModifier.*;

public final class SubclassGenerationModifier extends BaseSubclassGenerator
{
   @Nullable private final MockingConfiguration mockingCfg;

   public SubclassGenerationModifier(
      @Nonnull Class<?> baseClass, @Nonnull Type mockedType,
      @Nonnull ClassReader classReader, @Nonnull String subclassName)
   {
      super(baseClass, classReader, mockedType, subclassName, true);
      mockingCfg = null;
   }

   SubclassGenerationModifier(
      @Nonnull Class<?> baseClass, @Nullable MockingConfiguration mockingConfiguration, @Nonnull Type mockedType,
      @Nonnull ClassReader classReader, @Nonnull String subclassName)
   {
      super(baseClass, classReader, mockedType, subclassName, false);
      mockingCfg = mockingConfiguration;
   }

   @Override
   @SuppressWarnings("AssignmentToMethodParameter")
   protected void generateMethodImplementation(
      @Nonnull String className, int access, @Nonnull String name, @Nonnull String desc,
      @Nullable String signature, @Nullable String[] exceptions)
   {
      if (signature != null && mockedTypeInfo != null) {
         signature = mockedTypeInfo.genericTypeMap.resolveReturnType(signature);
      }

      mw = cw.visitMethod(ACC_PUBLIC, name, desc, signature, exceptions);

      boolean noFiltersToMatch = mockingCfg == null;

      if (
         noFiltersToMatch && !ObjectMethods.isMethodFromObject(name, desc) ||
         !noFiltersToMatch && mockingCfg.matchesFilters(name, desc)
      ) {
         generateDirectCallToHandler(mw, className, access, name, desc, signature);
         generateReturnWithObjectAtTopOfTheStack(desc);
         mw.visitMaxs(1, 0);
      }
      else {
         generateEmptyImplementation(desc);
      }
   }
}
