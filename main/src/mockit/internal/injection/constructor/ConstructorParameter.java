/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.injection.constructor;

import java.lang.annotation.*;
import java.lang.reflect.*;
import javax.annotation.*;

import mockit.internal.injection.*;
import static mockit.internal.util.Utilities.*;

final class ConstructorParameter extends InjectionProvider
{
   @Nonnull private final Class<?> classOfDeclaredType;
   @Nonnull private final Annotation[] annotations;
   @Nullable private final Object value;

   ConstructorParameter(
      @Nonnull Type declaredType, @Nonnull Annotation[] annotations, @Nonnull String name, @Nullable Object value)
   {
      super(declaredType, name);
      classOfDeclaredType = getClassType(declaredType);
      this.annotations = annotations;
      this.value = value;
   }

   @Nonnull @Override public Class<?> getClassOfDeclaredType() { return classOfDeclaredType; }
   @Nonnull @Override public Annotation[] getAnnotations() { return annotations; }
   @Nullable @Override public Object getValue(@Nullable Object owner) { return value; }

   @Override
   public String toString() { return "parameter " + super.toString(); }
}
