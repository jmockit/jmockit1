/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.integration.junit4;

import org.junit.runners.*;
import org.junit.runners.model.*;

import mockit.internal.startup.*;

/**
 * A test runner for <em>JUnit 4.5+</em>, with special modifications to integrate with JMockit.
 * Normally, use of this class is optional.
 * Instead, simply make sure that <tt>jmockit-1-x.jar</tt> precedes <tt>junit-4-x.jar</tt> in the runtime classpath.
 *
 * @see <a href="http://jmockit.org/tutorial/Introduction.html#runningTests" target="tutorial">Tutorial</a>
 */
public final class JMockit extends BlockJUnit4ClassRunner
{
   static { Startup.initializeIfPossible(); }

   /**
    * Constructs a new instance of the test runner.
    *
    * @throws InitializationError if the test class is malformed
    */
   public JMockit(Class<?> testClass) throws InitializationError
   {
      super(testClass);
   }
}
