/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.injection.field;

import java.lang.annotation.*;
import java.lang.reflect.*;
import javax.annotation.*;

import mockit.internal.injection.*;

final class FieldToInject extends InjectionProvider
{
   @Nonnull private final Field targetField;

   FieldToInject(@Nonnull Field targetField)
   {
      super(targetField.getGenericType(), targetField.getName());
      this.targetField = targetField;
   }

   @Nonnull @Override public Class<?> getClassOfDeclaredType() { return targetField.getType(); }
   @Nonnull @Override public Annotation[] getAnnotations() { return targetField.getDeclaredAnnotations(); }

   @Override
   public String toString() { return "field " + super.toString(); }
}
