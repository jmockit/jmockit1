/*
 * Copyright (c) 2006-2014 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.expectations.mocking;

import mockit.internal.util.*;

import org.jetbrains.annotations.*;

public abstract class InstanceFactory
{
   @Nullable protected Object lastInstance;

   @NotNull public abstract Object create();

   @Nullable public final Object getLastInstance() { return lastInstance; }
   public final void clearLastInstance() { lastInstance = null; }

   static final class InterfaceInstanceFactory extends InstanceFactory
   {
      @NotNull private final Object emptyProxy;

      InterfaceInstanceFactory(@NotNull Object emptyProxy) { this.emptyProxy = emptyProxy; }

      @Override
      @NotNull public Object create() { lastInstance = emptyProxy; return emptyProxy; }
   }

   static final class ClassInstanceFactory extends InstanceFactory
   {
      @NotNull private final Class<?> concreteClass;

      ClassInstanceFactory(@NotNull Class<?> concreteClass) { this.concreteClass = concreteClass; }

      @Override
      @NotNull public Object create()
      {
         lastInstance = ConstructorReflection.newUninitializedInstance(concreteClass);
         return lastInstance;
      }
   }

   static final class EnumInstanceFactory extends InstanceFactory
   {
      @NotNull private final Object anEnumValue;

      EnumInstanceFactory(@NotNull Class<?> enumClass) { anEnumValue = enumClass.getEnumConstants()[0]; }

      @Override
      @NotNull public Object create() { lastInstance = anEnumValue; return anEnumValue; }
   }
}
