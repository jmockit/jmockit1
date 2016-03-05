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
      ClassWithNestedClasses.methodContainingAnonymousClass(1);

      assertEquals(12, fileData.lineCoverageInfo.getExecutableLineCount());
      assertEquals(64, fileData.lineCoverageInfo.getCoveragePercentage());
      assertEquals(14, fileData.lineCoverageInfo.getTotalItems());
      assertEquals( 9, fileData.lineCoverageInfo.getCoveredItems());

      findMethodData(27);
      assertMethodLines(27, 33);
      assertPaths(2, 1, 1);
      assertPath(4, 0);
      assertPath(5, 1);

      assertEquals( 6, fileData.pathCoverageInfo.firstLineToMethodData.size());
      assertEquals(57, fileData.pathCoverageInfo.getCoveragePercentage());
      assertEquals( 7, fileData.pathCoverageInfo.getTotalItems());
      assertEquals( 4, fileData.pathCoverageInfo.getCoveredItems());
   }
}
