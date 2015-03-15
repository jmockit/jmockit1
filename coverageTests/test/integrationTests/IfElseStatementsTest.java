/*
 * Copyright (c) 2006-2015 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package integrationTests;

import org.junit.*;
import static org.junit.Assert.*;

public final class IfElseStatementsTest extends CoverageTest
{
   IfElseStatements tested;

   @Test
   public void simpleIf()
   {
      tested.simpleIf(true);
      tested.simpleIf(false);

      assertLines(8, 11, 3);
      assertLine(8, 1, 1, 2);
      assertLine(9, 1, 1, 1);
      assertLine(11, 1, 1, 2);

      assertBranchingPoints(8, 2, 2);
      assertBranchingPoints(9, 0, 0);

      findMethodData(8);
      assertPaths(2, 2, 2);
      assertMethodLines(8, 11);
      assertPath(4, 1);
      assertPath(5, 1);
   }

   @Test
   public void ifAndElse()
   {
      tested.ifAndElse(true);
      tested.ifAndElse(false);

      findMethodData(15);
      assertPaths(2, 2, 2);
      assertMethodLines(15, 21);
      assertPath(6, 1);
      assertPath(5, 1);
   }

   @Test
   public void singleLineIf()
   {
      tested.singleLineIf(true);
      tested.singleLineIf(false);

      findMethodData(25);
      assertPaths(2, 2, 2);
      assertMethodLines(25, 26);
      assertPath(4, 1);
      assertPath(5, 1);
   }

   @Test
   public void singleLineIfAndElse()
   {
      tested.singleLineIfAndElse(true);
      tested.singleLineIfAndElse(false);

      assertLines(30, 31, 2);
      assertLine(30, 3, 3, 2, 1, 1);
      assertLine(31, 1, 1, 2);

      findMethodData(30);
      assertPaths(2, 2, 2);
      assertMethodLines(30, 31);
      assertPath(6, 1);
      assertPath(5, 1);
   }

   @Test
   public void singleLineIfAndElseWhereOnlyTheElseIsExecuted()
   {
      tested.anotherSingleLineIfAndElse(false);

      assertLines(148, 148, 1);
      assertLine(148, 3, 2, 1, 0, 1);
   }

   @Test
   public void singleLineIfAndElseWhereElseIsExecutedMoreTimes()
   {
      tested.yetAnotherSingleLineIfAndElse(false);
      tested.yetAnotherSingleLineIfAndElse(true);
      tested.yetAnotherSingleLineIfAndElse(false);

      assertLines(153, 153, 1);
      assertLine(153, 3, 3, 3, 1, 2);
   }

   @Test
   public void ifWithBooleanAndOperator()
   {
      tested.ifWithBooleanAndOperator(true, false);
      tested.ifWithBooleanAndOperator(false, true);

      assertLines(158, 161, 2);
      assertLine(158, 2, 2, 2, 1);
      assertLine(159, 1, 0, 0);
      assertLine(161, 1, 1, 2);
   }

   @Test @Ignore
   public void anotherIfWithBooleanAndOperator()
   {
      tested.anotherIfWithBooleanAndOperator(true, true);
      tested.anotherIfWithBooleanAndOperator(true, false);

      assertLines(172, 175, 3);
      assertLine(172, 3, 2, 2, 2, 0);
      assertLine(173, 1, 1, 1);
      assertLine(175, 1, 1, 2);
   }

   @Test
   public void ifWithBooleanOrOperator()
   {
      tested.ifWithBooleanOrOperator(false, false);
      tested.ifWithBooleanOrOperator(true, true);

      assertLines(165, 168, 3);
      assertLine(165, 2, 2, 2, 1);
      assertLine(166, 1, 1, 1);
      assertLine(168, 1, 1, 2);
   }

   @Test
   public void methodWithFourDifferentPathsAndSimpleLines_exerciseTwoOppositePaths()
   {
      tested.methodWithFourDifferentPathsAndSimpleLines(true, 0);
      tested.methodWithFourDifferentPathsAndSimpleLines(false, 1);

      findMethodData(35);
      assertPaths(4, 2, 2);
      assertMethodLines(35, 45);
      assertPath(9, 0);
      assertPath(10, 1);
      assertPath(8, 1);
      assertPath(9, 0);
   }

   @Test
   public void methodWithFourDifferentPathsAndSegmentedLines_exerciseTwoOppositePaths()
   {
      tested.methodWithFourDifferentPathsAndSegmentedLines(false, -1);
      tested.methodWithFourDifferentPathsAndSegmentedLines(true, 1);

      findMethodData(49);
      assertPaths(4, 2, 2);
      assertMethodLines(49, 53);
      assertPath(11, 1);
      assertPath(10, 0);
      assertPath(10, 0);
      assertPath(9, 1);
   }

   @Test
   public void ifElseWithComplexBooleanCondition()
   {
      tested.ifElseWithComplexBooleanCondition(true, false);

      findMethodData(58);
      assertPaths(3, 1, 1);
      assertMethodLines(58, 61);
      assertPath(5, 1);
      assertPath(7, 0);
      assertPath(7, 0);
   }

   @Test
   public void returnInput()
   {
      assertEquals(2, tested.returnInput(1, true, false, false));
      assertEquals(2, tested.returnInput(2, false, false, false));
      assertEquals(2, tested.returnInput(3, false, true, false));
      assertEquals(4, tested.returnInput(4, false, false, true));
      assertEquals(5, tested.returnInput(5, true, true, false));
      assertEquals(5, tested.returnInput(6, false, true, true));
      assertEquals(7, tested.returnInput(7, true, true, true));
      assertEquals(9, tested.returnInput(8, true, false, true));

      findMethodData(68);
      assertPaths(8, 8, 8);
      assertMethodLines(68, 81);
      assertPath(11, 1);
      assertPath(12, 1);
      assertPath(12, 1);
      assertPath(13, 1);
      assertPath(12, 1);
      assertPath(13, 1);
      assertPath(13, 1);
      assertPath(14, 1);
   }

   @Test
   public void nestedIf()
   {
      assertEquals(1, tested.nestedIf(false, false));
      assertEquals(2, tested.nestedIf(true, true));

      findMethodData(86);
      assertPaths(3, 2, 2);
      assertPath(5, 1);
      assertPath(7, 0);
      assertPath(8, 1);
   }

   @Test
   public void ifElseWithNestedIf()
   {
      assertEquals(1, tested.ifElseWithNestedIf(true, false));
      assertEquals(2, tested.ifElseWithNestedIf(true, true));
      assertEquals(3, tested.ifElseWithNestedIf(false, false));

      findMethodData(99);
      assertPaths(3, 3, 3);
      assertPath(5, 1);
      assertPath(7, 1);
      assertPath(8, 1);
   }

   @Test
   public void nestedIfElse()
   {
      assertEquals(1, tested.nestedIfElse(false, false));
      assertEquals(2, tested.nestedIfElse(true, true));
      assertEquals(3, tested.nestedIfElse(true, false));
      assertEquals(4, tested.nestedIfElse(false, true));

      findMethodData(115);
      assertPaths(4, 4, 4);
      assertPath(8, 1);
      assertPath(9, 1);
      assertPath(9, 1);
      assertPath(8, 1);
   }

   @Test
   public void infeasiblePaths()
   {
      tested.infeasiblePaths(true);
      tested.infeasiblePaths(false);

      findMethodData(137);
      assertPaths(4, 2, 2);
      assertPath(7, 1);
      assertPath(8, 0);
      assertPath(8, 0);
      assertPath(9, 1);
   }

   @Test
   public void ifSpanningMultipleLines()
   {
      tested.ifSpanningMultipleLines(true, 0);
      tested.ifSpanningMultipleLines(false, -1);

      assertLine(179, 2, 2, 2, 1);
      assertLine(183, 1, 1, 1);
      assertLine(185, 1, 1, 2);
   }
}