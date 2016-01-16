/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.integration.junit4;

import static org.junit.Assert.*;
import org.junit.*;

public final class UseDependencyTest
{
   @Test
   public void useMockedDependency()
   {
      if (AnotherDependency.mockedAtSuiteLevel) {
         assertFalse(AnotherDependency.alwaysTrue());
      }
      else {
         assertTrue(AnotherDependency.alwaysTrue());
      }
   }

   private static final boolean STATIC_FIELD = Dependency.alwaysTrue();
   private final boolean instanceField = Dependency.alwaysTrue();

   @Test
   public void useFieldSetThroughDirectInstanceInitializationRatherThanBeforeMethod()
   {
      assertTrue("Dependency still mocked", instanceField);
   }

   @Test
   public void useFieldSetThroughDirectClassInitializationRatherThanBeforeClassMethod()
   {
      assertTrue("Dependency still mocked", STATIC_FIELD);
   }
}
