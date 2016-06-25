/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package java8testing;

import org.junit.jupiter.api.*;
import org.junit.platform.runner.JUnitPlatform;
import org.junit.runner.*;
import static org.junit.jupiter.api.Assertions.*;

import mockit.*;

@RunWith(JUnitPlatform.class)
public final class JUnit5Test
{
   @Tested(availableDuringSetup = true) TestUtils utils;

   @Tested BusinessService cut;
   @Injectable Collaborator collaborator;

   @BeforeEach
   void checkMockFields()
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
}
