/*
 * Copyright (c) 2006-2015 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.expectations.injection;

import java.lang.annotation.*;
import java.lang.reflect.*;
import javax.annotation.*;

final class ConstructorParameter implements InjectionPointProvider
{
   @Nonnull private final Type declaredType;
   @Nonnull private final Annotation[] annotations;
   @Nonnull private final String name;

   ConstructorParameter(@Nonnull Type declaredType, @Nonnull Annotation[] annotations, @Nonnull String name)
   {
      this.declaredType = declaredType;
      this.annotations = annotations;
      this.name = name;
   }

   @Nonnull @Override public Type getDeclaredType() { return declaredType; }
   @Nonnull @Override public Class<?> getClassOfDeclaredType() { return (Class<?>) declaredType; }
   @Nonnull @Override public String getName() { return name; }
   @Nonnull @Override public Annotation[] getAnnotations() { return annotations; }
}
