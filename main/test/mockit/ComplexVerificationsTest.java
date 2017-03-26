/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit;

import static org.junit.Assert.*;
import org.junit.*;

public class ComplexVerificationsTest
{
   public static class A
   {
      @SuppressWarnings("UnusedParameters")
      public void process(Object[] inputData) {}
      public int result() { return 1; }
   }

   public static class B { public int foo() { return -2; } }
   public static class C { public int bar() { return 3; } }

   final Object[] input = new Object[3];

   int testedMethod()
   {
      // Requirement 1: instantiations occur (a) first, (b) once per class.
      A a = new A();
      B b = new B();
      C c = new C();

      // Requirement 2: a.process is (a) called first, (b) only once.
      a.process(input);

      // Requirement 3: b.foo and c.bar are called (a) between the calls to A, and (b) input.length times each.
      //noinspection UnusedDeclaration
      for (Object in : input) {
         // Requirement 4: b.foo and c.bar are called in any order relative to each other.
         b.foo();
         c.bar();
      }

      // Requirement 5: a.result is (a) called last, (b) only once.
      return a.result();

      // Requirement 6: no other invocations occur on (a) A, (b) B, or (c) C.
   }

   @Test
   public void usingStrictAndNotStrictMockedTypes(@Mocked A anyA, @Mocked B anyB, @Mocked C anyC)
   {
      new StrictExpectations() {{
         // Meets requirements 1 and 2.
         A a = new A();
         a.process(input);

         // Meets requirement 5.
         a.result(); result = 42;
      }};

      new Expectations() {{
         // Meets requirements 1 and 2.
         B b = new B(); times = 1;
         C c = new C(); times = 1;

         // Meets requirement 3b and 4, but NOT 3a.
         b.foo(); times = input.length;
         c.bar(); times = input.length;
      }};

      assertEquals(42, testedMethod());

      // Meets requirement 6.
      new FullVerifications() {};
   }

   @Test
   public void fewerRequirementsUsingNotStrictExpectationsOnly(@Mocked A anyA, @Mocked B anyB, @Mocked C anyC)
   {
      // Requirements to meet: only 1b, 3b, 4, 6b and 6c.

      new Expectations() {{
         // Meets requirement 1b.
         A a = new A(); times = 1;
         B b = new B(); times = 1;
         C c = new C(); times = 1;
         a.result(); result = 42;

         // Meets requirements 3b and 4.
         b.foo(); times = input.length;
         c.bar(); times = input.length;
      }};

      assertEquals(42, testedMethod());

      // Meets requirements 6b and 6c.
      new FullVerifications(anyB, anyC) {};
   }
}
