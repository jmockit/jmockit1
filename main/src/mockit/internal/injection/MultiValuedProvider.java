/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.injection;

import java.lang.reflect.*;
import java.util.*;
import javax.annotation.*;

import static mockit.internal.util.Utilities.getClassType;

final class MultiValuedProvider extends InjectionPointProvider
{
   @Nonnull private final List<InjectionPointProvider> individualProviders;

   MultiValuedProvider(@Nonnull Type elementType)
   {
      super(elementType, "");
      individualProviders = new ArrayList<InjectionPointProvider>();
   }

   void addInjectable(@Nonnull InjectionPointProvider provider)
   {
      individualProviders.add(provider);
   }

   @Nonnull @Override protected Class<?> getClassOfDeclaredType() { return getClassType(declaredType); }

   @Nullable @Override
   protected Object getValue(@Nullable Object owner)
   {
      List<Object> values = new ArrayList<Object>(individualProviders.size());

      for (InjectionPointProvider provider : individualProviders) {
         Object value = provider.getValue(owner);
         values.add(value);
      }

      return values;
   }
}
