package integrationTests;

import org.junit.*;
import static org.junit.Assert.*;

public final class IfElseStatementsTest extends CoverageTest
{
   IfElseStatements tested;

   @Test
   public void simpleIf() {
      tested.simpleIf(true);
      tested.simpleIf(false);

      assertLines(8, 11, 3);
      assertLine(8, 1, 1, 2);
      assertLine(9, 1, 1, 1);
      assertLine(11, 1, 1, 2);

      assertBranchingPoints(8, 2, 2);
      assertBranchingPoints(9, 0, 0);
   }

   @Test
   public void ifAndElse() {
      tested.ifAndElse(true);
      tested.ifAndElse(false);
   }

   @Test
   public void singleLineIf() {
      tested.singleLineIf(true);
      tested.singleLineIf(false);

      // TODO: assertions
   }

   @Test
   public void singleLineIfAndElse() {
      tested.singleLineIfAndElse(true);
      tested.singleLineIfAndElse(false);

      assertLines(30, 31, 2);
      assertLine(30, 3, 3, 2, 1, 1);
      assertLine(31, 1, 1, 2);
   }

   @Test
   public void singleLineIfAndElseWhereOnlyTheElseIsExecuted() {
      tested.anotherSingleLineIfAndElse(false);

      assertLines(148, 148, 1);
      assertLine(148, 3, 2, 1, 0, 1);
   }

   @Test
   public void singleLineIfAndElseWhereElseIsExecutedMoreTimes() {
      tested.yetAnotherSingleLineIfAndElse(false);
      tested.yetAnotherSingleLineIfAndElse(true);
      tested.yetAnotherSingleLineIfAndElse(false);

      assertLines(153, 153, 1);
      assertLine(153, 3, 3, 3, 1, 2);
   }

   @Test
   public void ifWithBooleanAndOperator() {
      tested.ifWithBooleanAndOperator(true, false);
      tested.ifWithBooleanAndOperator(false, true);

      assertLines(158, 161, 2);
      assertLine(158, 2, 2, 2, 1);
      assertLine(159, 1, 0, 0);
      assertLine(161, 1, 1, 2);
   }

   @Test @Ignore
   public void anotherIfWithBooleanAndOperator() {
      tested.anotherIfWithBooleanAndOperator(true, true);
      tested.anotherIfWithBooleanAndOperator(true, false);

      assertLines(172, 175, 3);
      assertLine(172, 3, 2, 2, 2, 0);
      assertLine(173, 1, 1, 1);
      assertLine(175, 1, 1, 2);
   }

   @Test
   public void ifWithBooleanOrOperator() {
      tested.ifWithBooleanOrOperator(false, false);
      tested.ifWithBooleanOrOperator(true, true);

      assertLines(165, 168, 3);
      assertLine(165, 2, 2, 2, 1);
      assertLine(166, 1, 1, 1);
      assertLine(168, 1, 1, 2);
   }

   @Test
   public void methodWithFourDifferentPathsAndSimpleLines_exerciseTwoOppositePaths() {
      tested.methodWithFourDifferentPathsAndSimpleLines(true, 0);
      tested.methodWithFourDifferentPathsAndSimpleLines(false, 1);

      // TODO: assertions
   }

   @Test
   public void methodWithFourDifferentPathsAndSegmentedLines_exerciseTwoOppositePaths() {
      tested.methodWithFourDifferentPathsAndSegmentedLines(false, -1);
      tested.methodWithFourDifferentPathsAndSegmentedLines(true, 1);

      // TODO: assertions
   }

   @Test
   public void ifElseWithComplexBooleanCondition() {
      tested.ifElseWithComplexBooleanCondition(true, false);

      // TODO: assertions
   }

   @Test
   public void returnInput() {
      assertEquals(2, tested.returnInput(1, true, false, false));
      assertEquals(2, tested.returnInput(2, false, false, false));
      assertEquals(2, tested.returnInput(3, false, true, false));
      assertEquals(4, tested.returnInput(4, false, false, true));
      assertEquals(5, tested.returnInput(5, true, true, false));
      assertEquals(5, tested.returnInput(6, false, true, true));
      assertEquals(7, tested.returnInput(7, true, true, true));
      assertEquals(9, tested.returnInput(8, true, false, true));
   }

   @Test
   public void nestedIf() {
      assertEquals(1, tested.nestedIf(false, false));
      assertEquals(2, tested.nestedIf(true, true));

      // TODO: assertions
   }

   @Test
   public void ifElseWithNestedIf() {
      assertEquals(1, tested.ifElseWithNestedIf(true, false));
      assertEquals(2, tested.ifElseWithNestedIf(true, true));
      assertEquals(3, tested.ifElseWithNestedIf(false, false));

      // TODO: assertions
   }

   @Test
   public void nestedIfElse() {
      assertEquals(1, tested.nestedIfElse(false, false));
      assertEquals(2, tested.nestedIfElse(true, true));
      assertEquals(3, tested.nestedIfElse(true, false));
      assertEquals(4, tested.nestedIfElse(false, true));

      // TODO: assertions
   }

   @Test
   public void infeasiblePaths() {
      tested.infeasiblePaths(true);
      tested.infeasiblePaths(false);

      // TODO: assertions
   }

   @Test
   public void ifSpanningMultipleLines() {
      tested.ifSpanningMultipleLines(true, 0);
      tested.ifSpanningMultipleLines(false, -1);

      assertLine(179, 2, 2, 2, 1);
      assertLine(183, 1, 1, 1);
      assertLine(185, 1, 1, 2);
   }
}