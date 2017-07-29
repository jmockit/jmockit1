/*
 * Copyright (c) 2006 Rogério Liesenfeld
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
   public SubclassGenerationModifier(
      @Nonnull Class<?> baseClass, @Nonnull Type mockedType,
      @Nonnull ClassReader classReader, @Nonnull String subclassName, boolean copyConstructors)
   {
      super(baseClass, classReader, mockedType, subclassName, copyConstructors);
   }

   @Override
   @SuppressWarnings("AssignmentToMethodParameter")
   protected void generateMethodImplementation(
      @Nonnull String className, int access, @Nonnull String name, @Nonnull String desc,
      @Nullable String signature, @Nullable String[] exceptions)
   {
      if (signature != null && mockedTypeInfo != null) {
         signature = mockedTypeInfo.genericTypeMap.resolveSignature(className, signature);
      }

      mw = cw.visitMethod(ACC_PUBLIC, name, desc, signature, exceptions);

      if (ObjectMethods.isMethodFromObject(name, desc)) {
         generateEmptyImplementation(desc);
      }
      else {
         generateDirectCallToHandler(mw, className, access, name, desc, signature);
         generateReturnWithObjectAtTopOfTheStack(desc);
         mw.visitMaxs(1, 0);
      }
   }
}
