/*
 * Copyright (c) 2006-2014 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.integration.testng;

import static org.testng.Assert.*;
import org.testng.annotations.*;

import mockit.*;
import mockit.integration.*;

public final class TestNGExpectationsTest
{
   @Tested TestedClass tested;
   @Injectable MockedClass dependency;
   @Injectable MockedClass mock2;

   @BeforeSuite
   void setUpTestSuite() // executed once before all tests; has the widest scope and occurs before everything else
   {
      // Expectations are not supported here, because the invocation count of each individual expectation (whether it
      // was recorded or created on demand during replay) must be reset between individual tests in order to avoid
      // unexpected side effects from one test to another.
      //
      // It would be possible to simply reset the invocation count of each expectation to zero, but that would require
      // keeping all expectations recorded in all scopes (the suite, the TestNG "test", the test class, and the test
      // method) in a corresponding hierarchy of nested states, while discarding those states which go "out of scope"
      // at the appropriate points.
      // Such an elaborate scheme was never the goal of the Expectations API; instead, each recorded expectation is
      // meant to be exist only for the duration of a single test method.
      // (Note, however, that it is perfectly valid to record shared expectations in one or more "@BeforeMethod"'s.
      // This is ok because such methods are re-executed for each test method after the first one.)
      assertInvalidPlaceToRecordExpectations();
   }

   private void assertInvalidPlaceToRecordExpectations()
   {
      try {
         new Expectations() {};
         fail("Did not fail validation as expected");
      }
      catch (IllegalStateException e) {
         assertTrue(e.getMessage().contains("record"));
      }
   }

   @BeforeTest // executed once before all tests in a "test" grouping; occurs before @BeforeClass methods
   void setUpTest()
   {
      // The same considerations as those for "@BeforeSuite" methods apply here.
      assertInvalidPlaceToRecordExpectations();
   }

   @BeforeClass // executed once before all tests in the test class, but after any @BeforeTest methods
   void setUpTestClass()
   {
      // The same considerations as those for "@BeforeSuite" and "@BeforeTest" methods apply here.
      assertInvalidPlaceToRecordExpectations();
   }

   @BeforeMethod
   void setUpTestMethod1()
   {
      new NonStrictExpectations() {{
         mock2.doSomethingElse(anyInt);
         result = true;
      }};
   }

   @BeforeMethod
   void setUpTestMethod2()
   {
      new NonStrictExpectations() {{ dependency.getValue(); result = "mocked"; }};
   }

   @AfterMethod
   void tearDownTestMethod1()
   {
      new Verifications() {{ dependency.doSomething(anyInt); }};
   }

   @AfterMethod
   public void tearDownTestMethod2()
   {
      new Verifications() {{ mock2.doSomethingElse(6); }};
   }

   @AfterClass // executed once after all tests in the test class; occurs before @AfterTest methods
   void tearDownClass()
   {
      // The same considerations as those for "@BeforeClass" methods apply here.
      assertInvalidPlaceToVerifyExpectations();
   }

   private void assertInvalidPlaceToVerifyExpectations()
   {
      try {
         new Verifications() {};
         fail("Did not fail validation as expected");
      }
      catch (IllegalStateException e) {
         assertTrue(e.getMessage().contains("verify"));
      }
   }

   @AfterTest
   void tearDownTest() // executed once after all tests in a "test" grouping; occurs after @AfterClass methods
   {
      // The same considerations as those for "@BeforeTest" methods apply here.
      assertInvalidPlaceToVerifyExpectations();
   }

   @AfterSuite
   void tearDownSuite() // executed once after all tests; has the widest scope and occurs after everything else
   {
      // The same considerations as those for "@BeforeSuite" methods apply here.
      assertInvalidPlaceToVerifyExpectations();
   }

   @Test
   public void testSomething()
   {
      new Expectations() {{
         dependency.doSomething(anyInt); result = true;
      }};

      assertTrue(dependency.doSomething(5));
      assertEquals(dependency.getValue(), "mocked");
      assertTrue(tested.doSomething(-5));
      assertTrue(mock2.doSomethingElse(6));

      new FullVerifications(dependency) {{
         dependency.doSomething(anyInt); times = 2;
         dependency.getValue();
      }};
   }

   @Test
   public void testSomethingElse()
   {
      assertEquals(dependency.getValue(), "mocked");
      assertFalse(tested.doSomething(41));
      assertTrue(mock2.doSomethingElse(6));

      new FullVerificationsInOrder(dependency) {{
         dependency.getValue();
         dependency.doSomething(anyInt);
      }};
   }
}
