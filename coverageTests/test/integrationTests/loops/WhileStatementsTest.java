package integrationTests.loops;

import org.junit.*;

import integrationTests.*;

public final class WhileStatementsTest extends CoverageTest
{
   WhileStatements tested;

   @Test
   public void whileBlockInSeparateLines() {
      tested.whileBlockInSeparateLines();

      assertLines(7, 12, 4);
      assertLine(7, 1, 1, 1);
      assertLine(9, 1, 1, 6);
      assertLine(10, 1, 1, 5);
      assertLine(12, 1, 1, 1);
   }

   @Test
   public void whileBlockInSingleLine() {
      tested.whileBlockInSingleLine(0);
      tested.whileBlockInSingleLine(1);
      tested.whileBlockInSingleLine(2);

      assertLines(15, 16, 2);
      assertLine(15, 2, 2, 6);
      assertLine(16, 1, 1, 3);
   }

   @Test
   public void whileWithIfElse() {
      tested.whileWithIfElse(0);
      tested.whileWithIfElse(1);
      tested.whileWithIfElse(2);

      assertLines(120, 129, 5);
   }

   @Test
   public void whileWithContinue() {
      tested.whileWithContinue(0);
      tested.whileWithContinue(1);
      tested.whileWithContinue(2);

      assertLines(20, 29, 6);
      assertLine(20, 1, 1, 6);
      assertLine(21, 1, 1, 3);
      assertLine(22, 1, 1, 2);
      assertLine(23, 1, 1, 2);
      assertLine(26, 1, 1, 1);
      assertLine(29, 1, 1, 3);
   }

   @Test
   public void whileWithBreak() {
      tested.whileWithBreak(0);
      tested.whileWithBreak(1);
      tested.whileWithBreak(2);

      assertLines(34, 42, 5);
      assertLine(34, 1, 1, 4);
      assertLine(35, 1, 1, 3);
      assertLine(36, 1, 1, 2);
      assertLine(39, 1, 1, 1);
      assertLine(42, 1, 1, 3);
   }

   @Test
   public void nestedWhile() {
      tested.nestedWhile(0, 2);
      tested.nestedWhile(1, 1);

      assertLines(47, 54, 4);
      assertLine(47, 1, 1, 3);
      assertLine(48, 1, 1, 1);
      assertLine(49, 1, 0, 0);
      assertLine(52, 1, 1, 1);
      assertLine(54, 1, 1, 2);
   }

   @Test
   public void doWhileInSeparateLines() {
      tested.doWhileInSeparateLines();

      assertLines(58, 63, 4);
      assertLine(58, 1, 1, 1);
      assertLine(61, 1, 1, 3);
      assertLine(62, 1, 1, 3);
      assertLine(63, 1, 1, 1);
   }

   @Test
   public void bothKindsOfWhileCombined() {
      tested.bothKindsOfWhileCombined(0, 0);
      tested.bothKindsOfWhileCombined(0, 2);
      tested.bothKindsOfWhileCombined(1, 1);

      assertLines(69, 76, 5);
      assertLine(69, 1, 1, 5);
      assertLine(71, 1, 1, 5);
      assertLine(73, 1, 1, 4);
      assertLine(75, 1, 1, 4);
      assertLine(76, 1, 1, 3);
   }

   @Test
   public void whileTrueEndingWithAnIf() {
      tested.whileTrueEndingWithAnIf(0);

      // TODO: assertions
   }

   @Test
   public void whileTrueStartingWithAnIf() {
      tested.whileTrueStartingWithAnIf(0);

      // TODO: assertions
   }

   @Test(expected = IllegalStateException.class)
   public void whileTrueWithoutExitCondition() {
      tested.whileTrueWithoutExitCondition();
   }

   @Test(expected = IllegalStateException.class)
   public void whileTrueContainingTryFinally() {
      tested.whileTrueContainingTryFinally();
   }
}
