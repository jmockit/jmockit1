/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package otherTests.testng;

import java.applet.*;

import org.testng.annotations.*;
import static org.testng.Assert.*;

import mockit.*;

public final class TestNGFakingTest
{
   @BeforeSuite
   void setUpTestSuite()
   {
      assertNull(System.getenv("BeforeSuite"));
   }

   @BeforeTest
   void setUpTest()
   {
      assertNull(System.getenv("BeforeTest"));
   }

   @BeforeClass
   void setUpTestClass()
   {
      new MockUp<System>() {
         @Mock String getenv(String name) { return name; }
      };
   }

   @BeforeMethod
   void setUpTestMethod()
   {
      assertEquals(System.getenv("BeforeMethod"), "BeforeMethod");

      new MockUp<Applet>() {
         @Mock String getAppletInfo() { return "MOCK"; }
      };
   }

   @Test
   public void testSomething()
   {
      assertEquals(System.getenv("testMethod"), "testMethod");
      assertEquals(new Applet().getAppletInfo(), "MOCK");
   }

   @AfterMethod
   void tearDownTestMethod()
   {
      assertEquals(System.getenv("AfterMethod"), "AfterMethod");
      assertEquals(new Applet().getAppletInfo(), "MOCK");
   }

   @AfterClass
   void tearDownClass()
   {
      assertEquals(System.getenv("AfterClass"), "AfterClass");
      assertNull(new Applet().getAppletInfo());
   }

   @AfterTest
   void tearDownTest()
   {
      assertNull(System.getenv("AfterTest"), "Fake still in effect");
      assertNull(new Applet().getAppletInfo());
   }

   @AfterSuite
   void tearDownSuite()
   {
      assertNull(System.getenv("AfterSuite"));
      assertNull(new Applet().getAppletInfo());
   }
}
