/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit;

import org.junit.*;
import org.junit.rules.*;
import static org.junit.Assert.assertEquals;

public final class TestedFieldWithFailedConstructorDITest
{
   @Rule public final ExpectedException thrown = ExpectedException.none();

   @Before
   public void configureExpectedException()
   {
      thrown.expect(IllegalArgumentException.class);
      thrown.expectMessage("No injectable value available for parameter \"value\" in constructor ");
      thrown.expectMessage("ClassWithOneParameter(Integer value)");
   }

   static class ClassWithOneParameter
   {
      Integer value;
      ClassWithOneParameter(Integer value) { this.value = value; }
   }

   @Tested ClassWithOneParameter tested;
   @Injectable Integer foo;

   @Test
   public void attemptToUseTestedObjectWhoseCreationFailedDueToInjectableWithoutAValue(@Injectable String s)
   {
      assertEquals("", s);
   }
}
