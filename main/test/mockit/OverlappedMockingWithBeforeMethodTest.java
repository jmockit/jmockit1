/*
 * Copyright (c) 2006-2013 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit;

import static org.junit.Assert.*;
import org.junit.*;

public final class OverlappedMockingWithBeforeMethodTest
{
   static final class DerivedClass extends BaseClass
   {
      boolean doSomethingElse() { doStatic1(); return true; }
   }

   @Mocked("doStatic1") final BaseClass mock = null;

   @Test
   public void overlappedStaticPartialMocking(@Mocked({"doStatic2", "doSomethingElse"}) final DerivedClass derived)
   {
      new NonStrictExpectations() {{
         derived.doSomethingElse(); result = true;
      }};

      try { BaseClass.doStatic1(); fail(); } catch (RuntimeException ignore) {}
      BaseClass.doStatic2();

      assertTrue(derived.doSomethingElse());
   }

   @Test
   public void regularMockingOfBaseClassAfterRegularMockingOfDerivedClassInPreviousTest()
   {
      assertRegularMockingOfBaseClass();
   }

   private void assertRegularMockingOfBaseClass()
   {
      BaseClass.doStatic1();
      try { BaseClass.doStatic2(); fail(); } catch (RuntimeException ignore) {}

      DerivedClass derived = new DerivedClass();
      assertTrue(derived.doSomethingElse());
   }

   @Test
   public void overlappedDynamicPartialMockingOfAllInstances()
   {
      final DerivedClass derived = new DerivedClass();

      new NonStrictExpectations(DerivedClass.class) {{
         BaseClass.doStatic2();
         derived.doSomethingElse(); result = true;
      }};

      try { BaseClass.doStatic1(); fail(); } catch (RuntimeException ignore) {}
      BaseClass.doStatic2();
      assertTrue(derived.doSomethingElse());

      new Verifications() {{
         BaseClass.doStatic1(); times = 1;
         BaseClass.doStatic2(); times = 1;
         derived.doSomethingElse(); times = 1;
      }};
   }

   @Test
   public void regularMockingOfBaseClassAfterDynamicMockingOfDerivedClassInPreviousTest()
   {
      assertRegularMockingOfBaseClass();
   }

   @Test
   public void overlappedDynamicPartialMockingOfSingleInstance()
   {
      final DerivedClass derived = new DerivedClass();

      new NonStrictExpectations(derived) {{
         derived.doSomethingElse(); result = true;
      }};

      try { BaseClass.doStatic1(); fail(); } catch (RuntimeException ignore) {}
      try { BaseClass.doStatic2(); fail(); } catch (RuntimeException ignore) {}
      assertTrue(derived.doSomethingElse());

      new Verifications() {{
         BaseClass.doStatic1(); times = 1;
         BaseClass.doStatic2(); times = 1;
         derived.doSomethingElse(); times = 1;
      }};
   }

   @Test
   public void regularMockingOfBaseClassAfterDynamicMockingOfDerivedClassInstanceInPreviousTest()
   {
      assertRegularMockingOfBaseClass();
   }
}
