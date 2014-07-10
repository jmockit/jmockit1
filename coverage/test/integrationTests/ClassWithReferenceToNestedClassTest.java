/*
 * Copyright (c) 2006-2014 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package integrationTests;

import org.junit.*;
import static org.junit.Assert.*;

public final class ClassWithReferenceToNestedClassTest extends CoverageTest
{
   final ClassWithReferenceToNestedClass tested = null;

   @Test
   public void exerciseOnePathOfTwo()
   {
      ClassWithReferenceToNestedClass.doSomething();

      assertEquals(2, fileData.lineCoverageInfo.getExecutableLineCount());
      assertEquals(50, fileData.lineCoverageInfo.getCoveragePercentage());
      assertEquals(2, fileData.lineCoverageInfo.getTotalItems());
      assertEquals(1, fileData.lineCoverageInfo.getCoveredItems());

      assertEquals(2, fileData.pathCoverageInfo.firstLineToMethodData.size());
      assertEquals(50, fileData.pathCoverageInfo.getCoveragePercentage());
      assertEquals(2, fileData.pathCoverageInfo.getTotalItems());
      assertEquals(1, fileData.pathCoverageInfo.getCoveredItems());
   }
}