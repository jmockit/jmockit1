/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.injection;

import javax.annotation.*;

import mockit.internal.injection.field.*;
import mockit.internal.injection.full.*;

public final class BeanExporter
{
   @Nonnull private final InjectionState injectionState;

   BeanExporter(@Nonnull InjectionState injectionState) { this.injectionState = injectionState; }

   @Nullable
   public Object getBean(@Nonnull String name)
   {
      InjectionPoint injectionPoint = new InjectionPoint(Object.class, name, true);
      Object bean = injectionState.getInstantiatedDependency(null, injectionPoint);
      return bean;
   }

   @Nullable
   public <T> T getBean(@Nonnull Class<T> beanType)
   {
      TestedClass testedClass = new TestedClass(beanType, beanType);
      String beanName = getBeanNameFromType(beanType);
      FullInjection injection = new FullInjection(injectionState, beanType, beanName);
      Injector injector = new FieldInjection(injectionState, injection);

      @SuppressWarnings("unchecked")
      T bean = (T) injection.createOrReuseInstance(testedClass, injector, null, beanName);
      return bean;
   }

   @Nonnull
   private static String getBeanNameFromType(@Nonnull Class<?> beanType)
   {
      String name = beanType.getSimpleName();
      return Character.toLowerCase(name.charAt(0)) + name.substring(1);
   }
}
