/*
 * Copyright (c) 2006-2011 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package integrationTests;

import org.junit.*;

public final class UnreachableStatementsTest extends CoverageTest
{
   UnreachableStatements tested;

   @Test(expected = AssertionError.class)
   public void nonBranchingMethodWithUnreachableLines()
   {
      tested.nonBranchingMethodWithUnreachableLines();
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