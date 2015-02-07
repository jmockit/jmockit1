/*
 * Copyright (c) 2006-2015 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.integration.testng;

import org.testng.*;

import mockit.integration.testng.internal.*;
import mockit.internal.startup.*;

/**
 * A test listener implementation for TestNG 6.2+ that will properly initialize JMockit before any tests are executed.
 *
 * @see <a href="http://jmockit.org/tutorial/Introduction.html#runningTests">Tutorial</a>
 */
public final class Initializer implements ITestNGListener
{
   public Initializer()
   {
      if (Startup.initializeIfPossible()) {
         //noinspection deprecation
         TestNGRunnerDecorator.registerWithTestNG(TestNG.getDefault());
      }
   }
}
