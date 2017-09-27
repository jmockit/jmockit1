/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.integration.springframework;

import javax.annotation.*;

import mockit.internal.injection.*;
import mockit.internal.state.*;

import org.springframework.beans.factory.*;
import org.springframework.web.context.support.*;

/**
 * A {@link org.springframework.web.context.WebApplicationContext} implementation which exposes the
 * {@linkplain mockit.Tested @Tested} objects and their injected dependencies declared in the current test class.
 */
public final class TestWebApplicationContext extends StaticWebApplicationContext
{
   @Override @Nonnull
   public Object getBean(@Nonnull String name)
   {
      BeanExporter beanExporter = getBeanExporter();
      Object bean = BeanLookup.getBean(beanExporter, name);
      return bean;
   }

   @Nonnull
   private static BeanExporter getBeanExporter()
   {
      TestedClassInstantiations testedClasses = TestRun.getTestedClassInstantiations();

      if (testedClasses == null) {
         throw new BeanDefinitionStoreException("Test class does not define any @Tested fields");
      }

      return testedClasses.getBeanExporter();
   }

   @Override @Nonnull
   public <T> T getBean(@Nonnull String name, @Nullable Class<T> requiredType)
   {
      BeanExporter beanExporter = getBeanExporter();
      T bean = BeanLookup.getBean(beanExporter, name, requiredType);
      return bean;
   }

   @Override @Nonnull
   public <T> T getBean(@Nonnull Class<T> requiredType)
   {
      BeanExporter beanExporter = getBeanExporter();
      T bean = BeanLookup.getBean(beanExporter, requiredType);
      return bean;
   }
}
