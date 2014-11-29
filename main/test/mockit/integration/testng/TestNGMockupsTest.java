/*
 * Copyright (c) 2006-2014 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.integration.testng;

import org.testng.annotations.*;
import static org.testng.Assert.*;

import mockit.*;
import mockit.integration.*;

public final class TestNGMockupsTest
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

      new MockUp<MockedClass>() {
         @Mock String getValue() { return "MOCK"; }
      };
   }

   @Test
   public void testSomething()
   {
      assertEquals(System.getenv("testMethod"), "testMethod");
      assertEquals(new MockedClass().getValue(), "MOCK");
   }

   @AfterMethod
   void tearDownTestMethod()
   {
      assertEquals(System.getenv("AfterMethod"), "AfterMethod");
      assertEquals(new MockedClass().getValue(), "MOCK");
   }

   @AfterClass
   void tearDownClass()
   {
      assertEquals(System.getenv("AfterClass"), "AfterClass");
      assertEquals(new MockedClass().getValue(), "REAL");
   }

   @AfterTest
   void tearDownTest()
   {
      assertNull(System.getenv("AfterTest"), "MockUp still in effect");
      assertEquals(new MockedClass().getValue(), "REAL");
   }

   @AfterSuite
   void tearDownSuite()
   {
      assertNull(System.getenv("AfterSuite"));
      assertEquals(new MockedClass().getValue(), "REAL");
   }
}
