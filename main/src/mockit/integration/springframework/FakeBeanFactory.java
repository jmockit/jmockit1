/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.integration.springframework;

import javax.annotation.*;

import mockit.*;
import mockit.internal.injection.*;
import mockit.internal.state.*;

import org.springframework.beans.factory.support.*;

/**
 * If applied, this fake will take over calls to {@link AbstractBeanFactory#getBean(String)} and
 * {@link AbstractBeanFactory#getBean(String, Class)} in any implementation class, returning instead a
 * {@link Tested @Tested} or {@link Injectable @Injectable} object with the given field name, or a dependency object
 * injected at any level into a <tt>@Tested</tt> object.
 * <p/>
 * In case said calls come (indirectly) from a test class having no <tt>@Tested</tt> fields, bean lookup will proceed
 * into the actual <tt>getBean</tt> implementation method.
 * <p/>
 * Note this fake is only useful if the code under test makes direct calls to Spring's <tt>getBean</tt> methods.
 */
public final class FakeBeanFactory extends MockUp<DefaultListableBeanFactory>
{
   @Mock
   public Object getBean(@Nonnull Invocation invocation, @Nonnull String name)
   {
      TestedClassInstantiations testedClasses = TestRun.getTestedClassInstantiations();

      if (testedClasses == null) {
         return invocation.proceed();
      }

      BeanExporter beanExporter = testedClasses.getBeanExporter();
      Object bean = BeanLookup.getBean(beanExporter, name);
      return bean;
   }

   @Mock
   public <T> T getBean(@Nonnull Invocation invocation, @Nonnull String name, @Nullable Class<T> requiredType)
   {
      TestedClassInstantiations testedClasses = TestRun.getTestedClassInstantiations();

      if (testedClasses == null) {
         return invocation.proceed();
      }

      BeanExporter beanExporter = testedClasses.getBeanExporter();
      T bean = BeanLookup.getBean(beanExporter, name, requiredType);
      return bean;
   }

   @Mock
   public <T> T getBean(@Nonnull Invocation invocation, @Nonnull Class<T> requiredType)
   {
      TestedClassInstantiations testedClasses = TestRun.getTestedClassInstantiations();

      if (testedClasses == null) {
         return invocation.proceed();
      }

      BeanExporter beanExporter = testedClasses.getBeanExporter();
      T bean = BeanLookup.getBean(beanExporter, requiredType);
      return bean;
   }
}
