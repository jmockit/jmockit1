/*
 * Copyright (c) 2006-2012 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit;

import java.util.*;
import java.util.concurrent.*;

import static java.util.Arrays.*;
import static org.junit.Assert.*;
import org.junit.*;

public final class TestedClassWithConstructorDI4Test
{
   static class GenericClass<T> { T doSomething() { return null; } }

   public static final class TestedClass
   {
      final GenericClass<String> go;
      final List<Integer> values;
      final Callable<Number> action1;
      private Callable<Number> action2;
      private Callable<Number> action3;

      public TestedClass(GenericClass<String> go, List<Integer> values, Callable<Number>... actions)
      {
         this.go = go;
         this.values = values;
         action1 = actions[0];
         if (actions.length > 1) action2 = actions[1];
         if (actions.length > 2) action3 = actions[2];
      }
   }

   @Tested TestedClass tested;
   @Injectable Callable<Number> action1;
   @Injectable final GenericClass<String> mockGO = new GenericClass<String>(); // still mocked
   @Injectable final List<Integer> numbers = asList(1, 2, 3); // not mocked when interface

   @Before
   public void recordCommonExpectations()
   {
      new NonStrictExpectations() {{ mockGO.doSomething(); result = "test"; }};
   }

   @After
   public void verifyCommonExpectations()
   {
      new Verifications() {{ mockGO.doSomething(); times = 1; }};
   }

   @Test
   public void exerciseTestedObjectWithValuesInjectedFromMockFields()
   {
      assertNotNull(tested.go);
      assertEquals(asList(1, 2, 3), tested.values);
      assertSame(action1, tested.action1);
      assertEquals("test", mockGO.doSomething());
      assertNull(new GenericClass<String>().doSomething());
   }

   @Test
   public void exerciseTestedObjectWithValuesInjectedFromMockParameters(
      @Injectable Callable<Number> action2, @Injectable Callable<Number> action3)
   {
      assertNotNull(tested.go);
      assertEquals(asList(1, 2, 3), tested.values);
      assertSame(action1, tested.action1);
      assertSame(action2, tested.action2);
      assertSame(action3, tested.action3);
      assertEquals("test", mockGO.doSomething());
      assertNull(new GenericClass().doSomething());
   }
}
