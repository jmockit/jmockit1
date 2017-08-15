/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.integration.testng;

import org.testng.annotations.*;
import static org.testng.Assert.*;

import mockit.*;

public final class TestNGCascadingTest
{
   static class Foo { Bar getBar() { return null; } }
   static class Bar { String getValue() { return null; } }

   @Mocked Foo foo;

   @Test
   public void useExpectationResultRecordedOnCascadedInstance()
   {
      new Expectations() {{ foo.getBar().getValue(); result = "test"; }};

      String value = foo.getBar().getValue();

      assertNotNull(value);
   }

   @Test
   public void getUnrecordedResultFromCascadedInstance()
   {
      String value = foo.getBar().getValue();

      assertNull(value);
   }
}
