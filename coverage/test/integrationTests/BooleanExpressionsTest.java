/*
 * Copyright (c) 2006-2015 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package integrationTests;

import org.junit.*;
import org.junit.runners.*;
import static org.junit.Assert.*;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public final class BooleanExpressionsTest extends CoverageTest
{
   BooleanExpressions tested;

   @Test
   public void evalBuggyCombination()
   {
      // Only this combination will fail if the third condition in eval1 is changed to "z < 0",
      // which demonstrates that a more sophisticated metric than branch coverage is needed.
      assertTrue(tested.eval1(true, false, 1));

//      assertLine(7, 3, 3, 1, 1, 1);

      findMethodData(7);
      assertPaths(4, 1, 1);
   }

   @Test // includes executions from the previous test
   public void evalOnlySomeCombinations()
   {
      assertTrue(tested.eval1(true, true, 0));
      assertFalse(tested.eval1(true, false, 0));

//      assertLine(7, 3, 3, 3, 3, 2);

      findMethodData(7);
      assertPaths(4, 3, 3);
   }

   @Test
   public void evalAllCombinations()
   {
      assertTrue(tested.eval2(true, true, 0));
      assertTrue(tested.eval2(true, false, 1));
      assertFalse(tested.eval2(true, false, 0));
      assertFalse(tested.eval2(false, true, 0));

//      assertLine(12, 3, 3, 4, 3, 2);

      findMethodData(12);
      assertPaths(4, 4, 4);
   }

   @Test
   public void evalAllPaths()
   {
      assertFalse(tested.eval3(false, true, false));
      assertTrue(tested.eval3(true, true, false));
      assertTrue(tested.eval3(true, false, true));
      assertFalse(tested.eval3(true, false, false));

//      assertLine(17, 3, 3, 4, 3, 2);

      findMethodData(17);
      assertPaths(4, 4, 4);
   }

   @Test
   public void evalOnlyFirstAndSecondBranches()
   {
      assertFalse(tested.eval4(false, true, false));
      assertFalse(tested.eval4(false, false, false));
      assertFalse(tested.eval4(false, true, true));
      assertFalse(tested.eval4(false, false, true));
      assertTrue(tested.eval4(true, false, false));
      assertTrue(tested.eval4(true, false, true));

//      assertLine(22, 3, 2, 6, 2, 0);

      findMethodData(22);
      assertPaths(4, 2, 6);
   }

   @Test
   public void eval5()
   {
      assertFalse(tested.eval5(false, true, true));
      assertTrue(tested.eval5(false, false, false));

//      assertLine(30, 1, 1, 1);

      findMethodData(27);
      assertPaths(4, 2, 2);
      assertRegularPath(4, 0);
      assertRegularPath(8, 1);
      assertRegularPath(10, 0);
      assertShadowedPath(14, 0);
      assertRegularPath(13, 1);
   }

   @Test
   public void methodWithComplexExpressionWhichCallsAnotherInSameClass()
   {
      BooleanExpressions.isSameTypeIgnoringAutoBoxing(int.class, Integer.class);

      findMethodData(35);
      assertPaths(8, 1, 1);

      findMethodData(43);
      assertPaths(63, 1, 1);
   }

   @Test
   public void trivialMethodWhichReturnsBooleanInput()
   {
      assertTrue(tested.simplyReturnsInput(true));
      assertFalse(tested.simplyReturnsInput(false));

      assertLine(53, 1, 1, 2);

      findMethodData(53);
      assertPaths(1, 1, 2);
      assertPath(2, 2);
   }

   @Test
   public void methodWhichReturnsNegatedBoolean()
   {
      assertTrue(tested.returnsNegatedInput(false));

//      assertLine(58, 1, 1, 1);

      findMethodData(58);
      assertPaths(1, 1, 1);
      assertShadowedPath(6, 0);
      assertRegularPath(5, 1);
   }

   @Test
   public void methodWithIfElseAndTrivialTernaryOperator()
   {
      assertTrue(tested.returnsTrivialResultFromInputAfterIfElse(false, 1));
      assertFalse(tested.returnsTrivialResultFromInputAfterIfElse(true, 0));

      findMethodData(65);
      assertPaths(2, 2, 2);
      assertShadowedPath(11, 0);
      assertRegularPath(10, 1);
      assertShadowedPath(10, 1);
      assertRegularPath(9, 1);
   }

   @Test
   public void methodWithTrivialTernaryOperatorAndTrivialIfElse()
   {
      assertTrue(tested.returnsResultPreviouslyComputedFromInput(false, 1));
      assertFalse(tested.returnsResultPreviouslyComputedFromInput(false, 0));
      assertTrue(tested.returnsResultPreviouslyComputedFromInput(true, 1));
      assertTrue(tested.returnsResultPreviouslyComputedFromInput(true, -1));

      assertLines(77, 86, 5);
      assertLine(77, 3, 3, 4, 2, 2);
      assertLine(80, 2, 2, 4, 3);
      assertLine(82, 1, 1, 1);
      assertLine(83, 1, 1, 1);
      assertLine(86, 1, 1, 4);

      findMethodData(77);
      assertPaths(4, 3, 4);
      assertPath(12, 1);
      assertPath(11, 1);
      assertPath(11, 0);
      assertPath(10, 2);
   }
}
