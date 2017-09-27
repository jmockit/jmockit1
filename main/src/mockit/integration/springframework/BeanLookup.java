/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.integration.springframework;

import javax.annotation.*;

import mockit.internal.injection.*;

import org.springframework.beans.factory.*;

final class BeanLookup
{
   @Nonnull
   static Object getBean(@Nonnull BeanExporter beanExporter, @Nonnull String name)
   {
      Object bean = beanExporter.getBean(name);

      if (bean == null) {
         throw new NoSuchBeanDefinitionException(name);
      }

      return bean;
   }

   @Nonnull @SuppressWarnings("unchecked")
   static <T> T getBean(@Nonnull BeanExporter beanExporter, @Nonnull String name, @Nullable Class<T> requiredType)
   {
      if (requiredType == null) {
         return (T) getBean(beanExporter, name);
      }

      T bean = (T) beanExporter.getBean(name);

      if (bean != null) {
         Class<?> actualType = bean.getClass();

         if (!requiredType.isAssignableFrom(actualType)) {
            throw new BeanNotOfRequiredTypeException(name, requiredType, actualType);
         }
      }
      else {
         bean = beanExporter.getBean(requiredType);

         if (bean == null) {
            throw new NoSuchBeanDefinitionException(requiredType, "with bean name \"" + name + '"');
         }
      }

      return bean;
   }

   @Nonnull
   static <T> T getBean(@Nonnull BeanExporter beanExporter, @Nonnull Class<T> requiredType)
   {
      T bean = beanExporter.getBean(requiredType);

      if (bean == null) {
         throw new NoSuchBeanDefinitionException(requiredType);
      }

      return bean;
   }
}
