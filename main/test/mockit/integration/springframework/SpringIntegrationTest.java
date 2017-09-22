/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.integration.springframework;

import org.junit.*;
import org.junit.rules.*;

import static org.junit.Assert.*;

import mockit.*;

import org.springframework.beans.factory.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.beans.factory.support.*;

public final class SpringIntegrationTest
{
   @BeforeClass
   public static void applySpringIntegration()
   {
      new FakeBeanFactory();
   }

   @Rule public final ExpectedException thrown = ExpectedException.none();

   public static class ExampleSUT
   {
      @Autowired Collaborator collaborator;
      @Autowired Dependency dependency;
   }

   public interface Dependency {}
   static final class DependencyImpl implements Dependency {}
   static class Collaborator { @Autowired Runnable action; }

   @Tested DependencyImpl dependency;
   @Tested(fullyInitialized = true) ExampleSUT exampleSUT;
   @Injectable Runnable action;

   @Test
   public void lookupTestedObjectsAndInjectedDependenciesThroughTheBeanFactory()
   {
      BeanFactory beanFactory = new DefaultListableBeanFactory();
      assertTestedObjectsAndDependencies(beanFactory);
   }

   void assertTestedObjectsAndDependencies(BeanFactory beanFactory)
   {
      assertSame(dependency, exampleSUT.dependency);

      Dependency dependencyBean = (Dependency) beanFactory.getBean("dependency");
      assertSame(dependency, dependencyBean);

      Collaborator collaboratorBean = (Collaborator) beanFactory.getBean("collaborator");
      assertSame(exampleSUT.collaborator, collaboratorBean);

      ExampleSUT sut = (ExampleSUT) beanFactory.getBean("exampleSUT");
      assertSame(exampleSUT, sut);

      Runnable mockAction = (Runnable) beanFactory.getBean("action");
      assertSame(action, mockAction);

      thrown.expect(NoSuchBeanDefinitionException.class);
      beanFactory.getBean("undefined");
   }

   @Test
   public void lookupTestedObjectsAndInjectedDependenciesThroughStrutsIntegration()
   {
      BeanFactory beanFactory = new TestWebApplicationContext();
      assertTestedObjectsAndDependencies(beanFactory);
   }
}
