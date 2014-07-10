/*
 * Copyright (c) 2006-2012 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit;

import static org.junit.Assert.*;
import org.junit.*;

public final class TestedClassWithConstructorDI2Test
{
   public static final class TestedClass
   {
      private final Dependency dependency1;
      private final Dependency dependency2;
      private final Dependency dependency3;

      public TestedClass(Dependency dependency1, Runnable r, Dependency dependency2, Dependency dependency3)
      {
         this.dependency1 = dependency1;
         this.dependency2 = dependency2;
         this.dependency3 = dependency3;
         r.run();
      }

      public int doSomeOperation()
      {
         return dependency1.doSomething() + dependency2.doSomething();
      }
   }

   static class Dependency { int doSomething() { return -1; } }

   @Tested TestedClass tested;
   @Injectable Runnable task;
   @Injectable Dependency dependency1;
   @Injectable Dependency dependency2;

   @Test
   public void exerciseTestedObjectWithDependenciesOfSameTypeInjectedThroughConstructor(
      @Injectable Dependency dependency3)
   {
      assertSame(dependency3, tested.dependency3);

      new Expectations() {{
         dependency1.doSomething(); result = 23;
         dependency2.doSomething(); result = 5;
      }};

      assertEquals(28, tested.doSomeOperation());
   }

   @Test
   public void exerciseTestedObjectWithExtraInjectableParameter(
      @Injectable Dependency dependency3, @Injectable Dependency mock4)
   {
      assertSame(dependency1, tested.dependency1);
      assertSame(dependency2, tested.dependency2);
      assertSame(dependency3, tested.dependency3);
   }
}
