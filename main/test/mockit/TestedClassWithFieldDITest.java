/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit;

import org.junit.*;
import static org.junit.Assert.*;

public final class TestedClassWithFieldDITest
{
   static final class UtilityClass
   {
      String name;
      int id;
      Runnable action;
      Collaborator collaborator;
   }

   public static class TestedClass
   {
      static Runnable globalAction;

      protected final int i;
      protected Dependency dependency;

      public TestedClass() { i = -1; }
      public TestedClass(int i) { this.i = i; }

      public boolean doSomeOperation() { return dependency.doSomething() > 0; }
   }

   static class Dependency { int doSomething() { return -1; } }
   public static class Collaborator {}

   @Tested(availableDuringSetup = true) UtilityClass util;
   @Tested(availableDuringSetup = true, fullyInitialized = true) UtilityClass util2;
   @Injectable("util") String utilName;

   @Tested TestedClass tested;
   @Injectable Dependency dependency;

   @Before
   public void setUp()
   {
      assertUtilObjectsAreAvailable();
   }

   void assertUtilObjectsAreAvailable()
   {
      assertNotNull(util);
      assertEquals("util", util.name);
      assertNull(util.collaborator);

      assertNotNull(util2);
      assertEquals("util", util2.name);
      assertNotNull(util2.collaborator);
   }

   @After
   public void tearDown()
   {
      assertUtilObjectsAreAvailable();
   }

   @Test
   public void exerciseTestedObjectWithFieldInjectedByType()
   {
      assertEquals(-1, tested.i);
      assertSame(dependency, tested.dependency);

      new Expectations() {{
         dependency.doSomething(); result = 23; times = 1;
      }};

      assertTrue(tested.doSomeOperation());
   }

   @Test
   public void exerciseTestedObjectCreatedThroughConstructorAndFieldInjection(@Injectable("123") int value)
   {
      assertEquals(0, util.id);
      assertEquals(123, tested.i);
      assertSame(dependency, tested.dependency);
   }

   @Test
   public void ignoreStaticFieldsWhenDoingFieldInjection(@Injectable Runnable action)
   {
      assertNull(util.action);
      assertNull(TestedClass.globalAction);
   }

   @Tested("123") int id;
   @Tested UtilityClass tested2;

   @Test
   public void createTestedObjectInjectingItWithValueProvidedInPreviousTestedField()
   {
      assertEquals(123, tested2.id);
   }
}
