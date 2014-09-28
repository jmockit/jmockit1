/*
 * Copyright (c) 2006-2014 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package unitils.wiki;

import org.junit.*;

import mockit.*;

import static org.junit.Assert.*;

public final class PartialMock_JMockit_Test
{
   // Note that JMockit does not require the use of dependency injection.
   @Tested MyService myService;

   @Test
   public void outputText()
   {
      // Nothing is mocked for this test.
      assertEquals("the text", myService.outputText()); // executes the original behavior
   }

   @Test
   public void outputOtherText()
   {
      // Creates an instance normally, to use when recording expectations on instance methods.
      final TextService textService = new TextService();

      new Expectations(TextService.class) {{ // "dynamic" partial mocking, for all instances of the class
         // Will match calls to the method on any instance:
         textService.getText(); result = "some other text"; // overrides the original behavior
      }};

      assertEquals("some other text", myService.outputText()); // executes this new behavior
   }
}