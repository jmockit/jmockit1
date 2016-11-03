/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit;

import org.junit.*;
import org.junit.rules.*;

public final class TestedFieldWithFailedConstructorDITest
{
   @Rule public final ExpectedException thrown = ExpectedException.none();

   @Before
   public void configureExpectedException()
   {
      thrown.expect(IllegalArgumentException.class);
      thrown.expectMessage("No injectable value available for parameter \"value\" in constructor ");
      thrown.expectMessage("ClassWithStringParameter(String value)");
   }

   static class ClassWithStringParameter
   {
      String value;
      ClassWithStringParameter(String value) { this.value = value; }
   }

   @Tested ClassWithStringParameter tested;
   @Injectable String foo;

   @Test
   public void attemptToUseTestedObjectWhoseCreationFailedDueToInjectableWithoutAValue() {}
}
