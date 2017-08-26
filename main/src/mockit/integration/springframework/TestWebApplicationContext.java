/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.integration.springframework;

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
   @Override
   public Object getBean(String name)
   {
      TestedClassInstantiations testedClasses = TestRun.getTestedClassInstantiations();

      if (testedClasses == null) {
         throw new BeanDefinitionStoreException("Test class does not define any @Tested fields");
      }

      Object bean = testedClasses.getBeanExporter().getBean(name);

      if (bean == null) {
         throw new NoSuchBeanDefinitionException(name);
      }

      return bean;
   }
}
