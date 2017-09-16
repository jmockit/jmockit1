/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package java8testing;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import mockit.*;

final class JUnit5Test
{
   @Tested(availableDuringSetup = true) TestUtils utils;
   @Tested BusinessService cut;
   @Injectable Collaborator collaborator;
   boolean runningInnerTest;

   @BeforeEach
   void checkMockAndTestedFields()
   {
      if (!runningInnerTest) { // executed also before "innerTest": unclear whether it's a JUnit bug or not
         assertNotNull(utils);
      }

      assertNotNull(collaborator);
      assertNull(cut);
   }

   @AfterEach
   void checkMockAndTestedFieldsAgain()
   {
      if (!runningInnerTest) { // executed also after "innerTest": unclear whether it's a JUnit bug or not
         assertNotNull(utils);
      }

      assertNotNull(collaborator);
      assertNull(cut);
   }

   @Test
   void withParameterProvidedByJUnit(TestInfo testInfo)
   {
      assertNotNull(testInfo);
   }

   @Test
   void withMockParameters(@Mocked Runnable mock, @Injectable("test") String text)
   {
      assertNotNull(mock);
      assertEquals("test", text);
      assertNotNull(collaborator);
      assertSame(collaborator, cut.getCollaborator());
   }

   @Nested
   final class InnerTest
   {
      InnerTest() { runningInnerTest = true; }

      @BeforeEach
      void setUp()
      {
         assertNotNull(collaborator);
      }

      @Test
      void innerTest()
      {
         assertTrue(runningInnerTest);
      }
   }
}
