/*
 * Copyright (c) 2006-2014 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.integration.testng;

import org.testng.annotations.*;
import static org.testng.Assert.*;

import mockit.*;

public final class OverlappedMockingWithMockFieldTest
{
   static final class DerivedClass extends BaseClass
   {
      boolean doSomethingElse() { doSomething1(); return true; }
   }

   @BeforeClass @AfterClass
   public void verifyNoMockingBeforeAndAfterAllTests()
   {
      BaseClass bc = new BaseClass();
      try { bc.doSomething1(); fail(); } catch (RuntimeException ignore) {}
      try { bc.doSomething1(); fail(); } catch (RuntimeException ignore) {}
      try { BaseClass.doStatic1(); fail(); } catch (RuntimeException ignore) {}
      try { BaseClass.doStatic2(); fail(); } catch (RuntimeException ignore) {}
   }

   @Mocked("doSomething1") BaseClass base;

   @Test
   public void overlappedStaticPartialMocking(@Mocked({"doSomething2", "doSomethingElse"}) final DerivedClass derived)
   {
      new Expectations() {{
         derived.doSomethingElse(); result = true;
      }};

      try { base.doSomething1(); fail(); } catch (RuntimeException ignore) {}
      callDoSomething2OnBaseObject(false);

      try { derived.doSomething1(); fail(); } catch (RuntimeException ignore) {}
      derived.doSomething2();
      assertTrue(derived.doSomethingElse());
   }

   private void callDoSomething2OnBaseObject(boolean expectRealMethodToBeExecuted)
   {
      if (expectRealMethodToBeExecuted) {
         try { base.doSomething2(); fail(); } catch (RuntimeException ignore) {}
      }
      else {
         base.doSomething2();
      }
   }

   @Test(dependsOnMethods = "overlappedStaticPartialMocking")
   public void regularMockingOfBaseClassAfterRegularMockingOfDerivedClassInPreviousTest()
   {
      assertRegularMockingOfBaseClass();
   }

   private void assertRegularMockingOfBaseClass()
   {
      base.doSomething1();
      callDoSomething2OnBaseObject(true);

      DerivedClass derived = new DerivedClass();
      assertTrue(derived.doSomethingElse());
      derived.doSomething1();
      try { derived.doSomething2(); fail(); } catch (RuntimeException ignore) {}
   }

   @Test(dependsOnMethods = "regularMockingOfBaseClassAfterRegularMockingOfDerivedClassInPreviousTest")
   public void overlappedDynamicPartialMockingOfAllInstances()
   {
      final DerivedClass derived = new DerivedClass();

      new Expectations(DerivedClass.class) {{
         base.doSomething2();
         derived.doSomethingElse(); result = true;
      }};

      try { base.doSomething1(); fail(); } catch (RuntimeException ignore) {}
      base.doSomething2();

      try { derived.doSomething1(); fail(); } catch (RuntimeException ignore) {}
      derived.doSomething2();
      assertTrue(derived.doSomethingElse());

      new Verifications() {{
         base.doSomething1(); times = 2;
         base.doSomething2(); times = 2;
         derived.doSomething1(); times = 2;
         derived.doSomething2(); times = 2;
         derived.doSomethingElse(); times = 1;
      }};
   }

   @Test(dependsOnMethods = "overlappedDynamicPartialMockingOfAllInstances")
   public void regularMockingOfBaseClassAfterDynamicMockingOfDerivedClassInPreviousTest()
   {
      assertRegularMockingOfBaseClass();
   }

   @Test(dependsOnMethods = "regularMockingOfBaseClassAfterDynamicMockingOfDerivedClassInPreviousTest")
   public void overlappedDynamicPartialMockingOfSingleInstance()
   {
      final DerivedClass derived = new DerivedClass();

      new Expectations(derived) {{
         derived.doSomething2();
         derived.doSomethingElse(); result = true;
      }};

      try { base.doSomething1(); fail(); } catch (RuntimeException ignore) {}
      callDoSomething2OnBaseObject(true);

      try { derived.doSomething1(); fail(); } catch (RuntimeException ignore) {}
      derived.doSomething2();
      assertTrue(derived.doSomethingElse());

      new Verifications() {{
         base.doSomething1(); times = 1;
         base.doSomething2(); times = 1;
         derived.doSomething1(); times = 1;
         derived.doSomething2(); times = 1;
         derived.doSomethingElse(); times = 1;
      }};
   }

   @Test(dependsOnMethods = "overlappedDynamicPartialMockingOfSingleInstance")
   public void regularMockingOfBaseClassAfterDynamicMockingOfDerivedClassInstanceInPreviousTest()
   {
      assertRegularMockingOfBaseClass();
   }
}
