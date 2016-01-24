/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package integrationTests;

import org.junit.*;
import static org.junit.Assert.*;

public final class ClassWithNestedClassesTest extends CoverageTest
{
   final ClassWithNestedClasses tested = null;

   @Test
   public void exerciseNestedClasses()
   {
      ClassWithNestedClasses.doSomething();

      assertEquals(10, fileData.lineCoverageInfo.getExecutableLineCount());
      assertEquals(60, fileData.lineCoverageInfo.getCoveragePercentage());
      assertEquals(10, fileData.lineCoverageInfo.getTotalItems());
      assertEquals( 6, fileData.lineCoverageInfo.getCoveredItems());

      findMethodData(27);
      assertMethodLines(27, 33);
      assertPaths(2, 1, 1);
      assertPath(4, 0);
      assertPath(5, 1);

      assertEquals( 5, fileData.pathCoverageInfo.firstLineToMethodData.size());
      assertEquals(50, fileData.pathCoverageInfo.getCoveragePercentage());
      assertEquals( 6, fileData.pathCoverageInfo.getTotalItems());
      assertEquals( 3, fileData.pathCoverageInfo.getCoveredItems());
   }
}
