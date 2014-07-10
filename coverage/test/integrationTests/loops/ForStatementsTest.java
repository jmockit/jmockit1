/*
 * Copyright (c) 2006-2013 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package integrationTests.loops;

import org.junit.*;

import integrationTests.*;

public final class ForStatementsTest extends CoverageTest
{
   ForStatements tested = new ForStatements();

   @Test
   public void forInSeparateLines()
   {
      tested.forInSeparateLines();
      tested.forInSeparateLines();

      findMethodData(9);
      assertPaths(2, 1, 2);
      assertPath(4, 0);
      assertPath(5, 2);
   }
}