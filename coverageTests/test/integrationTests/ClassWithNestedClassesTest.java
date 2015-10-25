/*
 * Copyright (c) 2006-2015 Rogério Liesenfeld
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

      assertEquals(8, fileData.lineCoverageInfo.getExecutableLineCount());
      assertEquals(50, fileData.lineCoverageInfo.getCoveragePercentage());
      assertEquals(8, fileData.lineCoverageInfo.getTotalItems());
      assertEquals(4, fileData.lineCoverageInfo.getCoveredItems());

      assertEquals(5, fileData.pathCoverageInfo.firstLineToMethodData.size());
      assertEquals(60, fileData.pathCoverageInfo.getCoveragePercentage());
      assertEquals(5, fileData.pathCoverageInfo.getTotalItems());
      assertEquals(3, fileData.pathCoverageInfo.getCoveredItems());
   }
}
