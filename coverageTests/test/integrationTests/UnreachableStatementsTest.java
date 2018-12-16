package integrationTests;

import org.junit.*;
import org.junit.runners.*;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public final class UnreachableStatementsTest extends CoverageTest
{
   UnreachableStatements tested;

   @Test
   public void staticClassInitializerShouldHaveNoBranches() {
      assertLine(3, 1, 1, 5); // one execution for each test (the constructor), plus one for the static initializer
   }

   @Test
   public void nonBranchingMethodWithUnreachableLines() {
      try { tested.nonBranchingMethodWithUnreachableLines(); } catch (AssertionError ignore) {}

      assertLines(7, 10, 2);
      assertLine(7, 1, 1, 1);
      assertLine(8, 1, 1, 1);
      assertLine(9, 1, 0, 0);
      assertLine(10, 1, 0, 0);
   }

   @Test
   public void branchingMethodWithUnreachableLines_avoidAssertion() {
      tested.branchingMethodWithUnreachableLines(0);

      assertLines(14, 20, 3);
      assertLine(14, 1, 1, 1);
      assertLine(15, 1, 0, 0);
      assertLine(16, 1, 0, 0);
      assertLine(19, 1, 1, 1);
      assertLine(20, 1, 1, 1);
   }

   @Test
   public void branchingMethodWithUnreachableLines_hitAndFailAssertion() {
      try { tested.branchingMethodWithUnreachableLines(1); } catch (AssertionError ignore) {}

      // Accounts for executions from previous test.
      assertLines(14, 20, 4);
      assertLine(14, 1, 1, 2);
      assertLine(15, 1, 1, 1);
      assertLine(16, 1, 0, 0);
  }
}