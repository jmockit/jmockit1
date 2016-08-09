/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.integration.testng;

import java.applet.*;

import javax.naming.*;

import static org.testng.Assert.*;
import org.testng.annotations.*;

import mockit.*;

public final class TestNGDecoratorTest extends BaseTestNGDecoratorTest
{
   public static class MockClass2 extends MockUp<Reference>
   {
      @Mock
      public String getClassName() { return "TEST2"; }
   }

   @Test
   public void setUpAndUseSomeMocks()
   {
      assertEquals(new Applet().getAppletInfo(), "TEST1");
      assertEquals(new Reference("REAL2").getClassName(), "REAL2");

      new MockClass2();

      assertEquals(new Reference("").getClassName(), "TEST2");
      assertEquals(new Applet().getAppletInfo(), "TEST1");
   }

   @Test
   public void setUpAndUseMocksAgain()
   {
      assertEquals(new Applet().getAppletInfo(), "TEST1");
      assertEquals(new Reference("REAL2").getClassName(), "REAL2");

      new MockClass2();

      assertEquals(new Reference("").getClassName(), "TEST2");
      assertEquals(new Applet().getAppletInfo(), "TEST1");
   }

   @AfterMethod
   public void afterTest()
   {
      assertEquals(new Reference("REAL2").getClassName(), "REAL2");
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
}
