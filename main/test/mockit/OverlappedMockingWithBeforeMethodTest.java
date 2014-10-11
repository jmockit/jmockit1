/*
 * Copyright (c) 2006-2014 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit;

import static org.junit.Assert.*;
import org.junit.*;
import org.junit.runners.*;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
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
      new Expectations() {{
         derived.doSomethingElse(); result = true;
      }};

      try { BaseClass.doStatic1(); fail(); } catch (RuntimeException ignore) {}
      BaseClass.doStatic2();

      assertTrue(derived.doSomethingElse());
   }

   @Test
   public void regularMockingOfBaseClassAfterRegularMockingOfDerivedClassInPreviousTest()
   {
      BaseClass.doStatic1();
      try { BaseClass.doStatic2(); fail(); } catch (RuntimeException ignore) {}

      DerivedClass derived = new DerivedClass();
      assertTrue(derived.doSomethingElse());
   }
}
