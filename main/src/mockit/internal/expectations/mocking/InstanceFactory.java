/*
 * Copyright (c) 2006-2015 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.expectations.mocking;

import mockit.internal.util.*;

import org.jetbrains.annotations.*;

public abstract class InstanceFactory
{
   @NotNull final Class<?> concreteClass;
   @Nullable Object lastInstance;

   InstanceFactory(@NotNull Class<?> concreteClass) { this.concreteClass = concreteClass; }

   @NotNull public abstract Object create();

   @Nullable public final Object getLastInstance() { return lastInstance; }
   public abstract void clearLastInstance();

   static final class InterfaceInstanceFactory extends InstanceFactory
   {
      @Nullable private Object emptyProxy;

      InterfaceInstanceFactory(@NotNull Object emptyProxy)
      {
         super(emptyProxy.getClass());
         this.emptyProxy = emptyProxy;
      }

      @Override @NotNull
      public Object create()
      {
         if (emptyProxy == null) {
            emptyProxy = ConstructorReflection.newInstanceUsingDefaultConstructor(concreteClass);
         }

         lastInstance = emptyProxy;
         return emptyProxy;
      }

      @Override
      public void clearLastInstance()
      {
         emptyProxy = null;
         lastInstance = null;
      }
   }

   static final class ClassInstanceFactory extends InstanceFactory
   {
      ClassInstanceFactory(@NotNull Class<?> concreteClass) { super(concreteClass); }

      @Override @NotNull
      public Object create()
      {
         lastInstance = ConstructorReflection.newUninitializedInstance(concreteClass);
         return lastInstance;
      }

      @Override
      public void clearLastInstance() { lastInstance = null; }
   }
}
