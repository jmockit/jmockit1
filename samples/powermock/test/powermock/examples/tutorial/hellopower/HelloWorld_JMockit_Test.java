/*
 * Copyright (c) 2006-2014 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package powermock.examples.tutorial.hellopower;

import org.junit.*;

import mockit.*;

import static org.junit.Assert.*;

/**
 * <a href="http://code.google.com/p/powermock/source/browse/trunk/examples/tutorial/src/solution/java/demo/org/powermock/examples/tutorial/hellopower/HelloWorldTest.java">PowerMock version</a>
 */
public final class HelloWorld_JMockit_Test
{
   @Test
   public void testGreetingUsingExpectationsAPI(@Mocked SimpleConfig mock)
   {
      new Expectations() {{
         SimpleConfig.getGreeting(); result = "Hello";
         SimpleConfig.getTarget(); result = "world";
      }};

      assertEquals("Hello world", new HelloWorld().greet());
   }

   @Test
   public void testGreetingUsingMockupsAPI()
   {
      new MockUp<SimpleConfig>() {
         @Mock String getGreeting() { return "Hello"; }
         @Mock String getTarget() { return "world"; }
      };

      assertEquals("Hello world", new HelloWorld().greet());
   }
}
