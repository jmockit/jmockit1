/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.injection;

import javax.annotation.*;

abstract class Injector
{
   @Nonnull final TestedClass testedClass;
   @Nonnull final InjectionState injectionState;
   @Nullable final FullInjection fullInjection;

   Injector(@Nonnull TestedClass testedClass, @Nonnull InjectionState state, @Nullable FullInjection injection)
   {
      this.testedClass = testedClass;
      injectionState = state;
      fullInjection = injection;
   }

   abstract void fillOutDependenciesRecursively(@Nonnull Object dependency);
}
