/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.injection;

import javax.annotation.*;

import mockit.internal.injection.full.*;

public class Injector
{
   @Nonnull protected final InjectionState injectionState;
   @Nullable protected final FullInjection fullInjection;

   protected Injector(@Nonnull InjectionState state, @Nullable FullInjection fullInjection)
   {
      injectionState = state;
      this.fullInjection = fullInjection;
   }

   public void fillOutDependenciesRecursively(@Nonnull Object dependency, @Nonnull TestedClass testedClass) {}
}
