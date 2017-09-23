/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.faking;

import javax.annotation.*;

import mockit.external.asm.*;
import mockit.internal.classGeneration.*;
import static mockit.external.asm.Opcodes.*;

final class InterfaceImplementationGenerator extends BaseImplementationGenerator
{
   InterfaceImplementationGenerator(@Nonnull ClassReader classReader, @Nonnull String implementationClassName)
   {
      super(classReader, implementationClassName);
   }

   @Override
   protected void generateMethodBody(
      int access, @Nonnull String name, @Nonnull String desc, @Nullable String signature, @Nullable String[] exceptions)
   {
      mw = cw.visitMethod(ACC_PUBLIC, name, desc, signature, exceptions);
      generateEmptyImplementation(desc);
   }
}
