/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.integration.junit4;

import org.junit.*;
import org.junit.runner.*;
import org.junit.runners.*;

import mockit.*;

@Ignore
@RunWith(Suite.class)
@Suite.SuiteClasses({MockDependencyTest.class, UseDependencyTest.class})
public final class TestSuiteWithBeforeClass
{
   @BeforeClass
   public static void setUpSuiteWideFakes()
   {
      new MockUp<AnotherDependency>() {
         @Mock boolean alwaysTrue() { return false; }
      };

      AnotherDependency.mockedAtSuiteLevel = true;
   }
}
