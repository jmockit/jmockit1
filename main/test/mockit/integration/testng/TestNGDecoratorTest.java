/*
 * Copyright (c) 2006-2014 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.integration.testng;

import javax.security.auth.login.*;

import static org.testng.Assert.*;
import org.testng.annotations.*;

import mockit.*;
import mockit.internal.*;

public final class TestNGDecoratorTest extends BaseTestNGDecoratorTest
{
   public static class RealClass2
   {
      public String getValue() { return "REAL2"; }
   }

   public static class MockClass2 extends MockUp<RealClass2>
   {
      @Mock
      public String getValue() { return "TEST2"; }
   }

   @Test
   public void setUpAndUseSomeMocks()
   {
      assertEquals(new RealClass1().getValue(), "TEST1");
      assertEquals(new RealClass2().getValue(), "REAL2");

      new MockClass2();

      assertEquals(new RealClass2().getValue(), "TEST2");
      assertEquals(new RealClass1().getValue(), "TEST1");
   }

   @Test
   public void setUpAndUseMocksAgain()
   {
      assertEquals(new RealClass1().getValue(), "TEST1");
      assertEquals(new RealClass2().getValue(), "REAL2");

      new MockClass2();

      assertEquals(new RealClass2().getValue(), "TEST2");
      assertEquals(new RealClass1().getValue(), "TEST1");
   }

   @AfterMethod
   public void afterTest()
   {
      assertEquals(new RealClass2().getValue(), "REAL2");
   }

   @SuppressWarnings("ClassMayBeInterface")
   public static class Temp {}
   private static final Temp temp = new Temp();

   @DataProvider(name = "data")
   public Object[][] createData1()
   {
      return new Object[][] { {temp} };
   }

   @Test(dataProvider = "data")
   public void checkNoMockingOfParametersWhenUsingDataProvider(Temp t)
   {
      assertSame(temp, t);
   }

   @Test
   public void checkMockingOfParameterWhenNotUsingDataProvider(@Mocked Temp mock)
   {
      assertNotSame(temp, mock);
   }

   @Test(expectedExceptions = UnexpectedInvocation.class)
   public void mockMethodWithViolatedInvocationCountConstraint() throws Exception
   {
      new MockUp<LoginContext>() {
         @Mock(minInvocations = 1)
         void $init(String name) { assertEquals(name, "test"); }

         @Mock(invocations = 1)
         void login() {}
      };

      LoginContext context = new LoginContext("test");
      context.login();
      context.login();
   }
}
