/*
 * Copyright (c) 2006-2014 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal;

import org.jetbrains.annotations.*;

/**
 * Identifies a class by its loader and name rather than by the {@code Class} object, which isn't available during
 * initial class transformation.
 */
public final class ClassIdentification
{
   @Nullable public final ClassLoader loader;
   @NotNull public final String name;

   public ClassIdentification(@Nullable ClassLoader loader, @NotNull String name)
   {
      this.loader = loader;
      this.name = name;
   }

   @NotNull public Class<?> getLoadedClass()
   {
      try {
         return Class.forName(name, false, loader);
      }
      catch (ClassNotFoundException e) {
         throw new RuntimeException(e);
      }
   }

   @Override
   public boolean equals(Object o)
   {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;

      ClassIdentification that = (ClassIdentification) o;

      if (loader != that.loader) return false;
      return name.equals(that.name);
   }

   @Override
   public int hashCode()
   {
      return loader == null ? name.hashCode() : 31 * loader.hashCode() + name.hashCode();
   }
}
