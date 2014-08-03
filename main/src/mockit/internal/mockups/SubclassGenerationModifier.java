/*
 * Copyright (c) 2006-2014 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.mockups;

import java.lang.reflect.Type;

import mockit.external.asm4.*;
import mockit.internal.classGeneration.*;
import mockit.internal.util.*;
import static mockit.external.asm4.Opcodes.*;

import org.jetbrains.annotations.*;

public final class SubclassGenerationModifier extends BaseSubclassGenerator
{
   public SubclassGenerationModifier(
      @NotNull Class<?> baseClass, @NotNull ClassReader classReader,
      @NotNull Type mockedType, @NotNull String subclassName)
   {
      super(baseClass, classReader, mockedType, subclassName, true);
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

      if (ObjectMethods.isMethodFromObject(name, desc)) {
         generateEmptyImplementation(desc);
      }
      else {
         // TODO: generate call to @Mock method
         generateReturnWithObjectAtTopOfTheStack(desc);
         mw.visitMaxs(1, 0);
      }
   }
}
