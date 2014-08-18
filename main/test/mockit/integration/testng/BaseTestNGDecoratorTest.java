/*
 * Copyright (c) 2006-2014 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.integration.testng;

import org.testng.*;
import org.testng.annotations.*;
import static org.testng.Assert.*;

import mockit.*;

public class BaseTestNGDecoratorTest implements IHookable
{
   // Makes sure TestNG integration works with test classes which implement IHookable.
   @Override
   public void run(IHookCallBack callBack, ITestResult testResult)
   {
      callBack.runTestMethod(testResult);
   }

   public static class RealClass1
   {
      public String getValue() { return "REAL1"; }
   }

   public static class MockClass1 extends MockUp<RealClass1>
   {
      @Mock
      public String getValue() { return "TEST1"; }
   }

   @BeforeMethod
   public final void beforeBase()
   {
      assertEquals(new RealClass1().getValue(), "REAL1");
      new MockClass1();
      assertEquals(new RealClass1().getValue(), "TEST1");
   }

   @AfterMethod
   public final void afterBase()
   {
      assertEquals(new RealClass1().getValue(), "TEST1");
   }
}
