/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package coverageExperiments;

import static org.junit.Assert.*;
import org.junit.*;

import mockit.*;

public final class PartialMockingTest
{
   @Tested TestedClassPartiallyMocked sut;

   @Test
   public void useDynamicPartialMocking()
   {
      new Expectations(sut) {{
         sut.doSomethingElse(); result = 5;
      }};

      int r = sut.doSomething("testing");

      assertEquals(12, r);
   }
}
