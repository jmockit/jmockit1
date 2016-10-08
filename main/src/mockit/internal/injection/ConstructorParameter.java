/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.injection;

import java.lang.annotation.*;
import java.lang.reflect.*;
import javax.annotation.*;

import static mockit.internal.util.Utilities.*;

final class ConstructorParameter extends InjectionPointProvider
{
   @Nonnull private final Class<?> classOfDeclaredType;
   @Nonnull private final Annotation[] annotations;

   ConstructorParameter(@Nonnull Type declaredType, @Nonnull Annotation[] annotations, @Nonnull String name)
   {
      super(declaredType, name);
      classOfDeclaredType = getClassType(declaredType);
      this.annotations = annotations;
   }

   @Nonnull @Override protected Class<?> getClassOfDeclaredType() { return classOfDeclaredType; }
   @Nonnull @Override Annotation[] getAnnotations() { return annotations; }

   @Override
   public String toString()
   {
      return "parameter " + super.toString();
   }
}
