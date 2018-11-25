/*
 * Copyright (c) 2006 JMockit developers
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.injection.field;

import javax.annotation.*;

import mockit.internal.injection.*;
import mockit.internal.injection.full.*;

public final class FieldInjection extends Injector
{
   public FieldInjection(@Nonnull InjectionState injectionState, @Nullable FullInjection fullInjection) {
      super(injectionState, fullInjection);
   }
}
