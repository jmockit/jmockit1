/*
 * Copyright (c) 2006-2014 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.mockups;

import mockit.external.asm.*;
import mockit.internal.classGeneration.*;
import static mockit.external.asm.Opcodes.*;

import org.jetbrains.annotations.*;

public final class InterfaceImplementationGenerator extends BaseImplementationGenerator
{
   public InterfaceImplementationGenerator(@NotNull ClassReader classReader, @NotNull String implementationClassName)
   {
      super(classReader, implementationClassName);
   }

   @Override
   protected void generateMethodBody(
      int access, @NotNull String name, @NotNull String desc, @Nullable String signature, @Nullable String[] exceptions)
   {
      mw = cw.visitMethod(ACC_PUBLIC, name, desc, signature, exceptions);
      generateEmptyImplementation(desc);
   }
}
