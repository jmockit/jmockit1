/*
 * Copyright (c) 2006-2015 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit;

import static org.junit.Assert.*;
import org.junit.*;

public final class TestedClassWithFieldDITest
{
   static final class UtilityClass
   {
      String name;
      int id;
      Runnable action;
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

   @Tested(availableDuringSetup = true) UtilityClass util;
   @Injectable("util") String utilName;

   @Tested TestedClass tested;
   @Injectable Dependency dependency;

   @Before
   public void setUp()
   {
      assertNotNull(util);
      assertEquals("util", util.name);
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
}
