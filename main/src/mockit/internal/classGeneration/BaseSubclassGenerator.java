/*
 * Copyright (c) 2006-2014 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.classGeneration;

import mockit.external.asm4.*;
import mockit.internal.*;

import org.jetbrains.annotations.*;

public class BaseSubclassGenerator extends BaseClassModifier
{
   protected BaseSubclassGenerator(@NotNull ClassReader classReader)
   {
      super(classReader);
   }
}
