/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.integration.testng;

import static org.testng.Assert.*;
import org.testng.annotations.*;

public final class TestNGParametersTest
{
   String p1;
   String p2;

   @Parameters({"p1", "p2"})
   public TestNGParametersTest(@Optional("Abc") String p1, @Optional("XPTO") String p2)
   {
      this.p1 = p1;
      this.p2 = p2;
   }

   @BeforeClass @Parameters("p1")
   void setUpClass(@Optional("Abc") String param)
   {
      assertEquals(param, "Abc");
      assertEquals(param, p1);
   }

   @BeforeTest @Parameters("param1")
   void setUpTest(@Optional String param)
   {
      assertNull(param);
   }

   @BeforeMethod @Parameters("param2")
   void setUp(@Optional("XYZ5") String param)
   {
      assertEquals(param, "XYZ5");
   }

   @Test @Parameters({"first", "second"})
   public void testSomething(@Optional("abc") String a, @Optional("123") String b)
   {
      assertEquals(a, "abc");
      assertEquals(b, "123");
      assertEquals(p1, "Abc");
      assertEquals(p2, "XPTO");
   }

   @Test
   public void testWithoutParameters()
   {
      assertEquals(p1, "Abc");
      assertEquals(p2, "XPTO");
   }

   @AfterMethod @Parameters("param3")
   void tearDown(@Optional String param)
   {
      assertNull(param);
   }

   @AfterTest @Parameters("param1")
   void tearDownTest(@Optional("value") String param)
   {
      assertEquals(param, "value");
   }

   @AfterClass @Parameters("p2")
   void tearDownClass(@Optional("XPTO") String param)
   {
      assertEquals(param, "XPTO");
      assertEquals(param, p2);
   }

   @DataProvider
   public Object[][] data()
   {
      Object[][] data = {{1, "A"}, {2, "B"}, {3, "C"}};
      return data;
   }

   @Test(dataProvider = "data")
   public void usingDataProvider(int i, String s)
   {
      assertTrue(i > 0);
      assertFalse(s.isEmpty());
   }
}
