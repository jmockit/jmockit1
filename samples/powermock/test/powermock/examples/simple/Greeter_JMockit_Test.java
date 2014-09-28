/*
 * Copyright (c) 2006-2014 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package powermock.examples.simple;

import org.junit.*;

import mockit.*;

import static mockit.Deencapsulation.*;
import static org.junit.Assert.*;

/**
 * <a href="http://code.google.com/p/powermock/source/browse/trunk/examples/simple/src/test/java/demo/org/powermock/examples/simple/GreeterTest.java">PowerMock version</a>
 */
public final class Greeter_JMockit_Test
{
   @Test
   public void getMessage(@Mocked(stubOutClassInitialization = true) SimpleConfig unused)
   {
      new Expectations() {{
         SimpleConfig.getGreeting(); result = "Hi";
         SimpleConfig.getTarget(); result = "All";
      }};

      // Testing private methods directly should be avoided in real tests:
      Object message = invoke(Greeter.class, "getMessage");

      assertEquals("Hi All", message);
      assertFalse(SimpleConfig.wasInitialized);
   }

   @Test
   public void testRun(@Mocked final Logger logger)
   {
      new Expectations() {{
         logger.log("Hello"); times = 10;
      }};

      invoke(new Greeter(), "run", 10, "Hello");
   }

   @Test(expected = IllegalArgumentException.class)
   public void runWhenLoggerThrowsUnexpectedRuntimeException(@Mocked Logger mock)
   {
      new Expectations() {{
         new Logger(); result = new IllegalArgumentException("Unexpected exception");
      }};

      invoke(new Greeter(), "run", 10, "Hello");
   }
}
