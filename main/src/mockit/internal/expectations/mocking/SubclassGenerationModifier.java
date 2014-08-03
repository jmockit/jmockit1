/*
 * Copyright (c) 2006-2014 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.expectations.mocking;

import java.lang.reflect.Type;

import mockit.external.asm4.*;
import mockit.internal.classGeneration.*;
import mockit.internal.util.*;
import static mockit.external.asm4.Opcodes.*;
import static mockit.internal.expectations.mocking.MockedTypeModifier.*;

import org.jetbrains.annotations.*;

public final class SubclassGenerationModifier extends BaseSubclassGenerator
{
   @Nullable private final MockingConfiguration mockingCfg;

   public SubclassGenerationModifier(
      @NotNull Class<?> baseClass, @NotNull Type mockedType,
      @NotNull ClassReader classReader, @NotNull String subclassName)
   {
      super(baseClass, classReader, mockedType, subclassName, true);
      mockingCfg = null;
   }

   SubclassGenerationModifier(
      @NotNull Class<?> baseClass, @Nullable MockingConfiguration mockingConfiguration, @NotNull Type mockedType,
      @NotNull ClassReader classReader, @NotNull String subclassName)
   {
      super(baseClass, classReader, mockedType, subclassName, false);
      mockingCfg = mockingConfiguration;
   }

   @Override
   @SuppressWarnings("AssignmentToMethodParameter")
   protected void generateMethodImplementation(
      String className, int access, @NotNull String name, @NotNull String desc,
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
