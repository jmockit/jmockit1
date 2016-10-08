/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.injection;

import java.lang.annotation.*;
import java.lang.reflect.*;
import javax.annotation.*;

final class FieldToInject extends InjectionPointProvider
{
   @Nonnull private final Field targetField;

   FieldToInject(@Nonnull Field targetField)
   {
      super(targetField.getGenericType(), targetField.getName());
      this.targetField = targetField;
   }

   @Nonnull @Override protected Class<?> getClassOfDeclaredType() { return targetField.getType(); }
   @Nonnull @Override Annotation[] getAnnotations() { return targetField.getDeclaredAnnotations(); }

   @Override
   public String toString()
   {
      return "field " + super.toString();
   }
}
