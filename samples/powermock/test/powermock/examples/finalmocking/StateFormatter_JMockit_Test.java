/*
 * Copyright (c) 2006-2014 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package powermock.examples.finalmocking;

import org.junit.*;

import mockit.*;

import static org.junit.Assert.*;

/**
 * <a href="http://code.google.com/p/powermock/source/browse/trunk/examples/DocumentationExamples/src/test/java/powermock/examples/finalmocking/StateFormatterTest.java">PowerMock version</a>
 */
public final class StateFormatter_JMockit_Test
{
   @Tested StateFormatter tested;
   @Injectable StateHolder stateHolderMock;

   @Test
   public void getFormattedState_actualStateExists()
   {
      final String expectedState = "state";
      new Expectations() {{ stateHolderMock.getState(); result = expectedState; }};

      String actualState = tested.getFormattedState();

      assertEquals(expectedState, actualState);
   }

   @Test
   public void getFormattedState_noStateExists()
   {
      String actualState = tested.getFormattedState();

      assertEquals("State information is missing", actualState);
   }
}
