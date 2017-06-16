/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.injection;

import java.lang.annotation.*;
import java.lang.reflect.*;
import javax.annotation.*;

/**
 * Provides type, name, and value(s) for an injection point, which is either a field to be injected or a parameter in
 * the chosen constructor of a tested class.
 */
public abstract class InjectionProvider
{
   public static final Object NULL = Void.class;

   @Nonnull protected final Type declaredType;
   @Nonnull protected final String name;
   @Nullable public InjectionProvider parent;

   protected InjectionProvider(@Nonnull Type declaredType, @Nonnull String name)
   {
      this.declaredType = declaredType;
      this.name = name;
   }

   @Nonnull public final Type getDeclaredType() { return declaredType; }
   @Nonnull public abstract Class<?> getClassOfDeclaredType();
   @Nonnull public final String getName() { return name; }
   @Nonnull public Annotation[] getAnnotations() { throw new UnsupportedOperationException("No annotations"); }
   @Nullable public Object getValue(@Nullable Object owner) { return null; }

   @Override
   public String toString()
   {
      Class<?> type = getClassOfDeclaredType();
      return '"' + type.getSimpleName() + ' ' + name + '"';
   }
}
