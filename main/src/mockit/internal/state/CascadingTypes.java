/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.state;

import java.util.*;
import java.util.Map.*;
import java.util.concurrent.*;
import javax.annotation.*;

import mockit.external.asm.*;
import static mockit.internal.util.Utilities.*;

public final class CascadingTypes
{
   @Nonnull private final Map<String, MockedTypeCascade> cascadingTypes;

   CascadingTypes() { cascadingTypes = new ConcurrentHashMap<String, MockedTypeCascade>(4); }

   public void add(boolean fromMockField, @Nonnull java.lang.reflect.Type mockedType)
   {
      Class<?> mockedClass = getClassType(mockedType);
      String mockedTypeDesc = Type.getInternalName(mockedClass);
      add(mockedTypeDesc, fromMockField, mockedType);
   }

   void add(@Nonnull String mockedTypeDesc, boolean fromMockField, @Nonnull java.lang.reflect.Type mockedType)
   {
      if (!cascadingTypes.containsKey(mockedTypeDesc)) {
         cascadingTypes.put(mockedTypeDesc, new MockedTypeCascade(fromMockField, mockedType));
      }
   }

   @Nullable
   public MockedTypeCascade getCascade(@Nonnull String mockedTypeDesc, @Nullable Object mockInstance)
   {
      if (cascadingTypes.isEmpty()) {
         return null;
      }

      MockedTypeCascade cascade = getCascade(mockedTypeDesc);

      if (cascade != null || mockInstance == null) {
         return cascade;
      }

      return getCascade(mockedTypeDesc, mockInstance.getClass());
   }

   @Nullable
   private MockedTypeCascade getCascade(@Nonnull String mockedTypeDesc)
   {
      MockedTypeCascade cascade = cascadingTypes.get(mockedTypeDesc);

      if (cascade != null) {
         return cascade;
      }

      for (Entry<String, MockedTypeCascade> cascadeEntry : cascadingTypes.entrySet()) {
          String cascadingTypeDesc = cascadeEntry.getKey();
          int p = cascadingTypeDesc.indexOf('<');

          if (p > 0 && cascadingTypeDesc.regionMatches(0, mockedTypeDesc, 0, p - 1)) {
            return cascadeEntry.getValue();
         }
      }

      return null;
   }

   @Nullable
   private MockedTypeCascade getCascade(@Nonnull String invokedTypeDesc, @Nonnull Class<?> mockedClass)
   {
      Class<?> typeToLookFor = mockedClass;

      do {
         String typeDesc = Type.getInternalName(typeToLookFor);

         if (invokedTypeDesc.equals(typeDesc)) {
            return null;
         }

         MockedTypeCascade cascade = getCascade(typeDesc);

         if (cascade != null) {
            cascade.mockedClass = mockedClass;
            return cascade;
         }

         cascade = getCascadeForInterface(invokedTypeDesc, typeToLookFor);

         if (cascade != null) {
            return cascade;
         }

         typeToLookFor = typeToLookFor.getSuperclass();
      }
      while (typeToLookFor != Object.class);

      return null;
   }

   @Nullable
   private MockedTypeCascade getCascadeForInterface(@Nonnull String invokedTypeDesc, @Nonnull Class<?> mockedClass)
   {
      for (Class<?> mockedInterface : mockedClass.getInterfaces()) {
         MockedTypeCascade cascade = getCascade(invokedTypeDesc, mockedInterface);

         if (cascade != null) {
            return cascade;
         }
      }

      return null;
   }

   void clearNonSharedCascadingTypes()
   {
      if (!cascadingTypes.isEmpty()) {
         Iterator<MockedTypeCascade> itr = cascadingTypes.values().iterator();

         while (itr.hasNext()) {
            MockedTypeCascade cascade = itr.next();

            if (cascade.fromMockField) {
               cascade.discardCascadedMocks();
            }
            else {
               itr.remove();
            }
         }
      }
   }

   public void clear() { cascadingTypes.clear(); }
}
