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
public interface InjectionPointProvider
{
   @Nonnull Type getDeclaredType();
   @Nonnull Class<?> getClassOfDeclaredType();
   @Nonnull String getName();
   @Nonnull Annotation[] getAnnotations();
}
