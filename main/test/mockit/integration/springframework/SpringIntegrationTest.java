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

   @Test
   public void lookupTestedObjectsAndInjectedDependenciesThroughStrutsIntegration()
   {
      BeanFactory beanFactory = new TestWebApplicationContext();
      assertTestedObjectsAndDependencies(beanFactory);
   }

   @Test
   public void lookUpBeanByNameWithUnknownNameUsingBeanFactory()
   {
      BeanFactory beanFactory = new DefaultListableBeanFactory();
      assertNoSuchBeanDefinitionForUnknownBeanName(beanFactory);
   }

   @Test
   public void lookUpBeanByNameWithUnknownNameUsingStrutsIntegration()
   {
      BeanFactory beanFactory = new TestWebApplicationContext();
      assertNoSuchBeanDefinitionForUnknownBeanName(beanFactory);
   }

   @Test
   public void lookUpBeanByNameAndTypeWithUnknownNameAndTypeUsingBeanFactory()
   {
      BeanFactory beanFactory = new DefaultListableBeanFactory();
      assertNoSuchBeanDefinitionForUnknownBeanNameAndType(beanFactory);
   }

   @Test
   public void lookUpBeanByNameAndTypeWithUnknownNameAndTypeUsingStrutsIntegration()
   {
      BeanFactory beanFactory = new TestWebApplicationContext();
      assertNoSuchBeanDefinitionForUnknownBeanNameAndType(beanFactory);
   }

   @Test
   public void lookUpBeanByNameAndTypeWithWrongTypeUsingBeanFactory()
   {
      BeanFactory beanFactory = new DefaultListableBeanFactory();
      assertBeanNotOfRequiredTypeForWrongBeanType(beanFactory);
   }

   @Test
   public void lookUpBeanByNameAndTypeWithWrongTypeUsingStrutsIntegration()
   {
      BeanFactory beanFactory = new TestWebApplicationContext();
      assertBeanNotOfRequiredTypeForWrongBeanType(beanFactory);
   }

   void assertTestedObjectsAndDependencies(BeanFactory beanFactory)
   {
      assertSame(dependency, exampleSUT.dependency);

      // Look-up bean by name only.
      Dependency dependencyBean = (Dependency) beanFactory.getBean("dependency");
      assertSame(dependency, dependencyBean);

      // Look-up bean by name and type.
      dependencyBean = beanFactory.getBean("dependency", Dependency.class);
      assertSame(dependency, dependencyBean);

      Collaborator collaboratorBean = (Collaborator) beanFactory.getBean("collaborator");
      assertSame(exampleSUT.collaborator, collaboratorBean);

      ExampleSUT sut = (ExampleSUT) beanFactory.getBean("exampleSUT");
      assertSame(exampleSUT, sut);

      Runnable mockAction = beanFactory.getBean("action", Runnable.class);
      assertSame(action, mockAction);

   }

   void assertNoSuchBeanDefinitionForUnknownBeanName(BeanFactory beanFactory)
   {
      thrown.expect(NoSuchBeanDefinitionException.class);
      thrown.expectMessage("undefined");
      beanFactory.getBean("undefined");
   }

   void assertNoSuchBeanDefinitionForUnknownBeanNameAndType(BeanFactory beanFactory)
   {
      thrown.expect(NoSuchBeanDefinitionException.class);
      thrown.expectMessage("undefined");
      thrown.expectMessage("Process");
      beanFactory.getBean("undefined", Process.class);
   }

   void assertBeanNotOfRequiredTypeForWrongBeanType(BeanFactory beanFactory)
   {
      thrown.expect(BeanNotOfRequiredTypeException.class);
      thrown.expectMessage("Collaborator");
      beanFactory.getBean("dependency", Collaborator.class);
   }
}
