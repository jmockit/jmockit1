/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit;

import org.junit.*;
import org.junit.rules.*;

public final class TestedFieldWithFailedFullDITest
{
   @Rule public final ExpectedException thrown = ExpectedException.none();

   @Before
   public void configureExpectedException()
   {
      thrown.expect(IllegalStateException.class);
      thrown.expectMessage("Missing @Tested or @Injectable");
      thrown.expectMessage("parameter \"value\"");
      thrown.expectMessage("ClassWithParameterizedConstructor(int value)");
      thrown.expectMessage("when initializing field ");
      thrown.expectMessage("dependency");
      thrown.expectMessage(ClassWithFieldOfClassHavingParameterizedConstructor.class.getSimpleName());
      thrown.expectMessage("tested");
   }

   static class ClassWithFieldOfClassHavingParameterizedConstructor { ClassWithParameterizedConstructor dependency; }

   static class ClassWithParameterizedConstructor
   {
      ClassWithParameterizedConstructor(@SuppressWarnings("unused") int value) {}
   }

   @Tested(fullyInitialized = true) ClassWithFieldOfClassHavingParameterizedConstructor tested;

   @Test
   public void attemptToUseTestedObjectWhoseCreationFailedDueToInjectableWithNullValue() {}
}
