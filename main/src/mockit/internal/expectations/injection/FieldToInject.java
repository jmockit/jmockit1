/*
 * Copyright (c) 2006-2015 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.expectations.injection;

import java.lang.annotation.*;
import java.lang.reflect.*;
import javax.annotation.*;

final class FieldToInject implements InjectionPointProvider
{
   @Nonnull private final Field targetField;

   FieldToInject(@Nonnull Field targetField) { this.targetField = targetField; }

   @Nonnull @Override
   public Type getDeclaredType() { return targetField.getGenericType(); }

   @Nonnull @Override
   public Class<?> getClassOfDeclaredType() { return targetField.getType(); }

   @Nonnull @Override
   public String getName() { return targetField.getName(); }

   @Nonnull @Override
   public Annotation[] getAnnotations() { return targetField.getDeclaredAnnotations(); }
}
