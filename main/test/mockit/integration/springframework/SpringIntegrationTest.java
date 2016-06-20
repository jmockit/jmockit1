/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.integration.springframework;

import org.junit.*;

import static org.junit.Assert.*;

import mockit.*;

import org.springframework.beans.factory.annotation.*;
import org.springframework.beans.factory.config.*;
import org.springframework.beans.factory.support.*;
import static org.springframework.beans.factory.config.AutowireCapableBeanFactory.*;

public final class SpringIntegrationTest
{
   private static AutowireCapableBeanFactory beanFactory;
   private static Dependency springDependency;
   private static ExampleSUT springBean;

   @BeforeClass
   public static void applySpringIntegration()
   {
      new BeanFactoryMockUp();

      beanFactory = new DefaultListableBeanFactory();
      springDependency = (Dependency) beanFactory.autowire(DependencyImpl.class, AUTOWIRE_BY_TYPE, false);
      springBean = (ExampleSUT) beanFactory.autowire(ExampleSUT.class, AUTOWIRE_BY_NAME, false);
   }

   public static class ExampleSUT
   {
      @Autowired Collaborator collaborator;
      @Autowired Dependency dependency;
   }

   public interface Dependency {}
   static final class DependencyImpl implements Dependency {}
   static class Collaborator {}

   @Tested DependencyImpl dependency;
   @Tested(fullyInitialized = true) ExampleSUT exampleSUT;

   @Test
   public void lookupTestedObjectsAndInjectedDependenciesThroughTheBeanFactory()
   {
      assertNotNull(exampleSUT.dependency);

      Dependency dependencyBean = (Dependency) beanFactory.getBean("dependency");
      assertSame(dependency, dependencyBean);
      assertNotSame(springDependency, dependencyBean);

      Collaborator collaboratorBean = (Collaborator) beanFactory.getBean("collaborator");
      assertSame(exampleSUT.collaborator, collaboratorBean);

      ExampleSUT sut = (ExampleSUT) beanFactory.getBean("exampleSUT");
      assertSame(exampleSUT, sut);
      assertNotSame(springBean, sut);
   }
}
