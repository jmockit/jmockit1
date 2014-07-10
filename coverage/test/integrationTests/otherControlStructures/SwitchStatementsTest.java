/*
 * Copyright (c) 2006-2013 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package integrationTests.otherControlStructures;

import org.junit.*;

import integrationTests.*;

public final class SwitchStatementsTest extends CoverageTest
{
   final SwitchStatements tested = new SwitchStatements();

   @Test
   public void switchStatementWithSparseCasesAndDefault()
   {
      tested.switchStatementWithSparseCasesAndDefault('A');
      tested.switchStatementWithSparseCasesAndDefault('\0');

      assertLines(7, 22, 5);
      assertLine(7, 1, 1, 2);
      assertLine(9, 1, 1, 1);
      assertLine(10, 1, 1, 1);
      assertLine(18, 1, 1, 1);
      assertLine(20, 1, 0, 0);
      assertLine(22, 1, 1, 1);

      findMethodData(7);
      assertMethodLines(7, 22);
      assertPaths(4, 2, 2);
      assertPath(4, 1);
      assertPath(5, 0);
      assertPath(6, 1);
      assertPath(6, 0);
   }

   @Test
   public void switchStatementWithSparseCasesAndDefaultOnDefaultCase()
   {
      try {
         tested.anotherSwitchStatementWithSparseCasesAndDefault('x');
      }
      catch (IllegalArgumentException e) {
         // OK
      }

      findMethodData(26);
      assertPaths(2, 1, 1);
   }

   @Test
   public void switchStatementWithCompactCasesAndDefault()
   {
      tested.switchStatementWithCompactCasesAndDefault(2);
      tested.switchStatementWithCompactCasesAndDefault(4);

      findMethodData(37);
      assertPaths(4, 2, 2);
      assertPath(4, 1);
      assertPath(5, 0);
      assertPath(6, 0);
      assertPath(6, 1);
   }

   @Test
   public void switchStatementWithCompactCasesAndDefaultOnDefaultCase()
   {
      try {
         tested.anotherSwitchStatementWithCompactCasesAndDefault(1);
         tested.anotherSwitchStatementWithCompactCasesAndDefault(5);
      }
      catch (IllegalArgumentException e) {
         // OK
      }

      findMethodData(56);
      assertPaths(2, 2, 2);
   }

   @Test
   public void switchStatementWithSparseCasesAndNoDefault()
   {
      tested.switchStatementWithSparseCasesAndNoDefault('f');
      tested.switchStatementWithSparseCasesAndNoDefault('b');

      findMethodData(64);
      assertPaths(3, 2, 2);
      assertPath(6, 0);
      assertPath(6, 1);
      assertPath(4, 1);
   }

   @Test
   public void switchStatementWithCompactCasesAndNoDefault()
   {
      tested.switchStatementWithCompactCasesAndNoDefault(0);
      tested.switchStatementWithCompactCasesAndNoDefault(4);
      tested.switchStatementWithCompactCasesAndNoDefault(5);

      findMethodData(76);
      assertPaths(4, 2, 3);
      assertPath(5, 0);
      assertPath(5, 0);
      assertPath(7, 1);
      assertPath(5, 2);
   }

   @Test
   public void switchStatementWithExitInAllCases()
   {
      tested.switchStatementWithExitInAllCases(1);
      tested.switchStatementWithExitInAllCases(2);

      findMethodData(89);
      assertPaths(3, 2, 2);
      assertPath(5, 1);
      assertPath(5, 1);
      assertPath(5, 0);
   }
}