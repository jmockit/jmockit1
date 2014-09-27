/*
 * Copyright (c) 2006-2014 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.classGeneration;

import mockit.external.asm.*;

import org.jetbrains.annotations.*;

/**
 * Generates a concrete subclass for an {@code abstract} base class.
 */
public final class ConcreteSubclass<T> extends ImplementationClass<T>
{
   public ConcreteSubclass(@NotNull Class<?> baseClass) { super(baseClass); }

   @NotNull @Override
   protected ClassVisitor createMethodBodyGenerator(@NotNull ClassReader typeReader)
   {
      return new BaseSubclassGenerator(sourceClass, typeReader, null, generatedClassName, false) {
         @Override
         protected void generateMethodImplementation(
            String className, int access, @NotNull String name, @NotNull String desc,
            @Nullable String signature, @Nullable String[] exceptions)
         {
            mw = cw.visitMethod(Opcodes.ACC_PUBLIC, name, desc, signature, exceptions);
            generateEmptyImplementation(desc);
         }
      };
   }
}
