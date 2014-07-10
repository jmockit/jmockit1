/*
 * Copyright (c) 2006-2012 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit;

import static org.junit.Assert.*;
import org.junit.*;

public final class TestedClassWithConstructorAndFieldDITest
{
   public static class TestedClass
   {
      protected final int i;

      // Suppose this is injected by some DI framework or Java EE container:
      @SuppressWarnings("UnusedDeclaration") protected Dependency dependency;

      public TestedClass() { i = -1; }
      public TestedClass(int i) { this.i = i; }

      public boolean doSomeOperation() { return dependency.doSomething() > 0; }
   }

   static class Dependency { int doSomething() { return -1; } }

   @SuppressWarnings("UnusedDeclaration")
   public static class AnotherTestedClass extends TestedClass
   {
      Runnable runnable;
      Dependency dependency3;
      Dependency dependency2;

      public AnotherTestedClass() { super(-2); }
      public AnotherTestedClass(int value, Dependency dependency1) { super(value); dependency = dependency1; }

      @Override
      public boolean doSomeOperation()
      {
         boolean b = dependency2.doSomething() > 0;
         return super.doSomeOperation() && b;
      }
   }

   @Tested AnotherTestedClass tested;
   @Injectable Dependency dependency;
   @Injectable Runnable mock2;
   @Injectable Dependency dependency2;

   @Test
   public void exerciseTestedSubclassObjectWithFieldsInjectedByTypeAndName()
   {
      assertEquals(-2, tested.i);
      assertSame(mock2, tested.runnable);
      assertSame(dependency, tested.dependency);
      assertSame(dependency2, tested.dependency2);
      assertNull(tested.dependency3);
      assertFalse(tested.doSomeOperation());

      new Verifications() {{
         mock2.run(); times = 0;
         dependency.doSomething(); times = 1;
         dependency2.doSomething();
      }};
   }

   @Test
   public void exerciseTestedSubclassObjectWithFieldsInjectedFromMockFieldsAndMockParameter(
      @Injectable Dependency dependency3)
   {
      assertEquals(-2, tested.i);
      assertSame(dependency, tested.dependency);
      assertSame(dependency2, tested.dependency2);
      assertSame(dependency3, tested.dependency3);
      assertFalse(tested.doSomeOperation());
   }

   @Test
   public void exerciseTestedSubclassObjectUsingConstructorAndFieldInjection(
      @Injectable("45") int value, @Injectable Dependency dependency1)
   {
      assertEquals(45, tested.i);
      assertSame(dependency1, tested.dependency);
      assertSame(dependency2, tested.dependency2);
      assertNull(tested.dependency3);
      assertSame(mock2, tested.runnable);
      assertFalse(tested.doSomeOperation());
   }
}
