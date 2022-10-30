/*
 * Copyright (c) 2006 JMockit developers
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
 * injected at any level into a <code>@Tested</code> object.
 * <p>
 * In case said calls come (indirectly) from a test class having no <code>@Tested</code> fields, bean lookup will proceed
 * into the actual <code>getBean</code> implementation method.
 * <p>
 * Note this fake is only useful if the code under test makes direct calls to Spring's <code>getBean</code> methods.
 */
public final class FakeBeanFactory extends MockUp<DefaultListableBeanFactory>
{
   @Mock
   public static Object getBean(@Nonnull Invocation invocation, @Nonnull String name) {
      TestedClassInstantiations testedClasses = TestRun.getTestedClassInstantiations();

      if (testedClasses == null) {
         return invocation.proceed();
      }

      BeanExporter beanExporter = testedClasses.getBeanExporter();
      Object bean = BeanLookup.getBean(beanExporter, name);
      return bean;
   }

   @Mock
   public static <T> T getBean(@Nonnull Invocation invocation, @Nonnull String name, @Nullable Class<T> requiredType) {
      TestedClassInstantiations testedClasses = TestRun.getTestedClassInstantiations();

      if (testedClasses == null) {
         return invocation.proceed();
      }

      BeanExporter beanExporter = testedClasses.getBeanExporter();
      T bean = BeanLookup.getBean(beanExporter, name, requiredType);
      return bean;
   }

   @Mock
   public static <T> T getBean(@Nonnull Invocation invocation, @Nonnull Class<T> requiredType) {
      TestedClassInstantiations testedClasses = TestRun.getTestedClassInstantiations();

      if (testedClasses == null) {
         return invocation.proceed();
      }

      BeanExporter beanExporter = testedClasses.getBeanExporter();
      T bean = BeanLookup.getBean(beanExporter, requiredType);
      return bean;
   }
}
