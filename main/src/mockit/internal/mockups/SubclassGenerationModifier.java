/*
 * Copyright (c) 2006-2014 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.mockups;

import mockit.external.asm.*;
import mockit.internal.classGeneration.*;
import static mockit.external.asm.Opcodes.*;

import org.jetbrains.annotations.*;

public final class SubclassGenerationModifier extends BaseSubclassGenerator
{
   public SubclassGenerationModifier(
      @NotNull Class<?> baseClass, @NotNull ClassReader classReader, @NotNull String subclassName)
   {
      super(baseClass, classReader, null, subclassName, true);
   }

   @Override
   protected void generateMethodImplementation(
      String className, int access, @NotNull String name, @NotNull String desc,
      @Nullable String signature, @Nullable String[] exceptions)
   {
      mw = cw.visitMethod(ACC_PUBLIC, name, desc, signature, exceptions);
      generateEmptyImplementation(desc);
   }
}
