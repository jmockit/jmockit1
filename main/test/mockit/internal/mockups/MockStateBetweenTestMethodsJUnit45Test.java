/*
 * Copyright (c) 2006-2014 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.mockups;

import java.io.*;
import java.sql.*;

import org.junit.*;
import static org.junit.Assert.*;

import mockit.*;
import mockit.internal.state.*;

public final class MockStateBetweenTestMethodsJUnit45Test
{
   static final class RealClass
   {
      int doSomething() { throw new RuntimeException("Unexpected execution"); }
   }

   static final class TheMockClass extends MockUp<RealClass>
   {
      static MockUp<?> theMockUp;
      int value;

      TheMockClass() { theMockUp = this; }

      @Mock(invocations = 1)
      int doSomething() { return ++value; }

      static void assertMockState(int expectedInvocationCount)
      {
         MockState mockState = TestRun.getMockStates().getMockState(theMockUp, 0);

         assertTrue(mockState.isWithExpectations());
         assertEquals(expectedInvocationCount, mockState.getTimesInvoked());
      }
   }

   public static class InterfaceMockUp extends MockUp<Driver>
   {
      private final String acceptedURL;
      public InterfaceMockUp(String acceptedURL) { this.acceptedURL = acceptedURL; }
      @Mock public boolean acceptsURL(String url) { return acceptedURL.contains(url); }
   }

   static Driver mockDriver1;

   @BeforeClass
   public static void applyTestClassScopedMockUps()
   {
      new TheMockClass();
      mockDriver1 = new InterfaceMockUp("url1").getMockInstance();
   }

   static Driver mockDriver2;

   @Before
   public void applyTestScopedMockUps()
   {
      mockDriver2 = new InterfaceMockUp("url2").getMockInstance();
   }

   @Test
   public void firstTest()
   {
      TheMockClass.assertMockState(0);
      assertEquals(1, new RealClass().doSomething());
      TheMockClass.assertMockState(1);
   }

   @Test
   public void secondTest() throws Exception
   {
      TheMockClass.assertMockState(0);
      assertEquals(2, new RealClass().doSomething());
      TheMockClass.assertMockState(1);

      new FileIOMockUp();
      new FileIO().writeToFile("test.txt");
   }

   public static final class FileIOMockUp extends MockUp<FileIO>
   {
      @Mock
      public static void writeToFile(String fileName)
      {
         assertNotNull(fileName);
      }
   }

   @After
   public void verifyThatTestMethodMockUpsAreNoLongerInEffectWhileOthersStillAre() throws Exception
   {
      assertThatClassMockedInTestMethodWasRestored();

      assertTrue(mockDriver1.acceptsURL("url1"));
      assertTrue(mockDriver2.acceptsURL("url2"));
   }

   static void assertThatClassMockedInTestMethodWasRestored()
   {
      try {
         new FileIO().writeToFile(null);
         fail();
      }
      catch (NullPointerException ignore) {}
      catch (IOException ignore) {}
   }

   @AfterClass
   public static void verifyThatOnlyTestClassScopedMockUpsAreStillInEffect() throws Exception
   {
      assertTrue(mockDriver1.acceptsURL("url1"));
      assertFalse(mockDriver2.acceptsURL("url2"));
   }
}