/*
 * Copyright (c) 2006-2014 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package integrationTests;

import org.junit.*;

public final class UnreachableStatementsTest extends CoverageTest
{
   UnreachableStatements tested;

   @Test
   public void nonBranchingMethodWithUnreachableLines()
   {
      try { tested.nonBranchingMethodWithUnreachableLines(); } catch (AssertionError ignore) {}

      assertLines(7, 10, 2);
      assertLine(7, 1, 1, 1);
      assertLine(8, 2, 2, 1);
      assertLine(9, 1, 0, 0);
      assertLine(10, 1, 0, 0);
   }

   @Test
   public void branchingMethodWithUnreachableLines_avoidUnreachableCode()
   {
      tested.branchingMethodWithUnreachableLines(0);
   }

   @Test(expected = AssertionError.class)
   public void branchingMethodWithUnreachableLines_hitUnreachableCode()
   {
      tested.branchingMethodWithUnreachableLines(1);
   }
}