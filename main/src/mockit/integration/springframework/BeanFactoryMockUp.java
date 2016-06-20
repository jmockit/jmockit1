/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.integration.springframework;

import mockit.*;
import mockit.internal.injection.*;
import mockit.internal.state.*;

import org.springframework.beans.factory.support.*;

public final class BeanFactoryMockUp extends MockUp<AbstractBeanFactory>
{
   @Mock
   public Object getBean(Invocation invocation, String name)
   {
      TestedClassInstantiations testedClasses = TestRun.getTestedClassInstantiations();

      if (testedClasses == null) {
         return invocation.proceed();
      }

      Object bean = testedClasses.getBeanExporter().getBean(name);
      return bean;
   }
}
