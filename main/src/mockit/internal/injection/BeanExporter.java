/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.injection;

import javax.annotation.*;

public interface BeanExporter
{
   @Nullable Object getBean(@Nonnull String name);
}
