/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit;

import javax.inject.*;

import org.junit.*;
import static org.junit.Assert.*;

public final class StandardDI2Test
{
   static class TestedClass
   {
      TestedClass() { throw new RuntimeException("Must not occur"); }
      @Inject TestedClass(Runnable action) {}
   }

   @Tested TestedClass tested;

   @Test(expected = IllegalArgumentException.class)
   public void attemptToCreateTestedObjectThroughAnnotatedConstructorWithMissingInjectables()
   {
      fail();
   }
}
