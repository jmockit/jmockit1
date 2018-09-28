/*
 * Copyright (c) 2006 JMockit developers
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.expectations.mocking;

import java.lang.reflect.Type;
import javax.annotation.*;

import mockit.asm.*;
import mockit.asm.jvmConstants.*;
import mockit.internal.classGeneration.*;
import mockit.internal.util.*;

public final class SubclassGenerationModifier extends BaseSubclassGenerator
{
   public SubclassGenerationModifier(
      @Nonnull Class<?> baseClass, @Nonnull Type mockedType, @Nonnull ClassReader cr, @Nonnull String subclassName, boolean copyConstructors
   ) {
      super(baseClass, cr, mockedType, subclassName, copyConstructors);
   }

   @Override
   protected void generateMethodImplementation(
      @Nonnull String className, int access, @Nonnull String name, @Nonnull String desc, @Nullable String signature,
      @Nullable String[] exceptions
   ) {
      if (signature != null && mockedTypeInfo != null) {
         signature = mockedTypeInfo.genericTypeMap.resolveSignature(className, signature);
      }

      mw = cw.visitMethod(Access.PUBLIC, name, desc, signature, exceptions);

      if (ObjectMethods.isMethodFromObject(name, desc)) {
         generateEmptyImplementation(desc);
      }
      else {
         generateDirectCallToHandler(className, access, name, desc, signature);
         generateReturnWithObjectAtTopOfTheStack(desc);
         mw.visitMaxStack(1);
      }
   }
}
