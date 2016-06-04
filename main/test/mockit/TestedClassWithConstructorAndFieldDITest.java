/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit;

import static org.junit.Assert.*;
import org.junit.*;

class BaseTest
{
   static class Dependency { int doSomething() { return -1; } }

   public static class TestedClass
   {
      protected final int i;
      protected Dependency dependency;
      Runnable action;

      public TestedClass() { i = -1; }
      public TestedClass(int i) { this.i = i; }

      public boolean doSomeOperation() { return dependency.doSomething() > 0; }
   }

   @Tested TestedClass tested1;
   @Injectable Dependency dependency;

   final void verifyTestedObjectFromBaseTestClass(int expectedValueForIntField)
   {
      assertEquals(expectedValueForIntField, tested1.i);
      assertSame(dependency, tested1.dependency);
      assertNotNull(tested1.action);
   }
}

public final class TestedClassWithConstructorAndFieldDITest extends BaseTest
{
   @SuppressWarnings("unused")
   public static class AnotherTestedClass extends TestedClass
   {
      Runnable anotherAction;
      Dependency dependency3;
      Dependency dependency2;

      public AnotherTestedClass() { super(-2); }

      public AnotherTestedClass(int value, Dependency dependency1)
      {
         super(value);
         //noinspection UnnecessarySuperQualifier
         super.dependency = dependency1;
      }

      @Override
      public boolean doSomeOperation()
      {
         boolean b = dependency2.doSomething() > 0;
         return super.doSomeOperation() && b;
      }
   }

   @Tested AnotherTestedClass tested2;
   @Injectable Runnable anotherAction;
   @Injectable Dependency dependency2;

   @Test
   public void exerciseTestedSubclassObjectWithFieldsInjectedByTypeAndName()
   {
      verifyTestedObjectFromBaseTestClass(-1);

      assertEquals(-2, tested2.i);
      assertSame(anotherAction, tested2.anotherAction);
      assertSame(dependency, tested2.dependency);
      assertSame(dependency2, tested2.dependency2);
      assertNull(tested2.dependency3);
      assertFalse(tested2.doSomeOperation());

      new Verifications() {{
         anotherAction.run(); times = 0;
         dependency.doSomething(); times = 1;
         dependency2.doSomething();
      }};
   }

   @Test
   public void exerciseTestedSubclassObjectWithFieldsInjectedFromMockFieldsAndMockParameter(
      @Injectable Dependency dependency3)
   {
      verifyTestedObjectFromBaseTestClass(-1);

      assertEquals(-2, tested2.i);
      assertSame(dependency, tested2.dependency);
      assertSame(dependency2, tested2.dependency2);
      assertSame(dependency3, tested2.dependency3);
      assertSame(anotherAction, tested2.anotherAction);
      assertFalse(tested2.doSomeOperation());
   }

   @Test
   public void exerciseTestedSubclassObjectUsingConstructorAndFieldInjection(
      @Injectable("45") int value, @Injectable Dependency dependency1)
   {
      verifyTestedObjectFromBaseTestClass(45);

      assertEquals(45, tested2.i);
      assertSame(dependency1, tested2.dependency);
      assertSame(dependency2, tested2.dependency2);
      assertNull(tested2.dependency3);
      assertSame(anotherAction, tested2.anotherAction);
      assertFalse(tested2.doSomeOperation());
   }
}
