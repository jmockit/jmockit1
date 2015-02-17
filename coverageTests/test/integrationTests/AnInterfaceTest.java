/*
 * Copyright (c) 2006-2014 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package integrationTests;

import org.junit.*;
import static org.junit.Assert.*;

public final class AnInterfaceTest extends CoverageTest
{
   AnInterface tested;

   @Before
   public void setUp()
   {
      tested = new AnInterface() {
         @Override public void doSomething(String s, boolean b) {}
         @Override public int returnValue() { return 0; }
      };
   }

   @Test
   public void useAnInterface()
   {
      tested.doSomething("test", true);

      assertEquals(0, fileData.lineCoverageInfo.getExecutableLineCount());
      assertEquals(-1, fileData.lineCoverageInfo.getCoveragePercentage());
      assertEquals(0, fileData.lineCoverageInfo.getTotalItems());
      assertEquals(0, fileData.lineCoverageInfo.getCoveredItems());

      assertTrue(fileData.pathCoverageInfo.firstLineToMethodData.isEmpty());
      assertEquals(-1, fileData.pathCoverageInfo.getCoveragePercentage());
      assertEquals(0, fileData.pathCoverageInfo.getTotalItems());
      assertEquals(0, fileData.pathCoverageInfo.getCoveredItems());
   }
}