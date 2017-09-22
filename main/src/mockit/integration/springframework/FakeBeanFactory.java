/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.integration.springframework;

import mockit.*;
import mockit.internal.injection.*;
import mockit.internal.state.*;

import org.springframework.beans.factory.*;
import org.springframework.beans.factory.support.*;

/**
 * If applied, this fake will take over calls to {@link AbstractBeanFactory#getBean(String)} in any implementation
 * class, returning instead a {@link Tested @Tested} or {@link Injectable @Injectable} object with the given field name,
 * or a dependency object injected at any level into a {@code @Tested} object.
 * <p/>
 * In case said calls come (indirectly) from a test class having no {@code @Tested} fields, bean lookup will proceed
 * into the actual {@code getBean(String)} implementation method.
 * <p/>
 * Note this fake is only useful if the code under test makes direct calls to Spring's {@code getBean(name)} method.
 */
public final class FakeBeanFactory extends MockUp<AbstractBeanFactory>
{
   @Mock
   public static Object getBean(Invocation invocation, String name)
   {
      TestedClassInstantiations testedClasses = TestRun.getTestedClassInstantiations();

      if (testedClasses == null) {
         return invocation.proceed();
      }

      Object bean = testedClasses.getBeanExporter().getBean(name);

      if (bean == null) {
         throw new NoSuchBeanDefinitionException(name);
      }

      return bean;
   }
}
