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

   @BeforeEach
   void checkMockAndTestedFields()
   {
      assertNotNull(utils);
      assertNotNull(collaborator);
      assertNull(cut);
   }

   @AfterEach
   void checkMockAndTestedFieldsAgain()
   {
      assertNotNull(utils);
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
      @BeforeEach
      void setUp()
      {
         assertNotNull(utils);
         assertNotNull(collaborator);
         assertNull(cut);
      }

      @Test
      void innerTest()
      {
         assertNotNull(collaborator);
         assertSame(collaborator, cut.getCollaborator());
      }

      @Test
      void innerTestWithMockParameter(@Injectable("123") int number)
      {
         assertEquals(123, number);
      }
   }
}
