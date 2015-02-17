/*
 * Copyright (c) 2006-2015 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package integrationTests.loops;

import java.util.*;
import static java.util.Arrays.*;

import org.junit.*;
import static org.junit.Assert.*;

import integrationTests.*;

public final class ForStatementsTest extends CoverageTest
{
   ForStatements tested;

   @Test
   public void forInSeparateLines()
   {
      tested.forInSeparateLines();
      tested.forInSeparateLines();

      assertLines(9, 12, 3);
      assertLine(9, 1, 1, 6, 4); // TODO: should have 3 segments
      assertLine(10, 1, 1, 4);
      assertLine(12, 1, 1, 2);

      findMethodData(9);
      assertPaths(2, 1, 2);
      assertMethodLines(9, 12);
      assertPath(4, 0);
      assertPath(5, 2);
   }

   @Test
   public void forInSingleLine()
   {
      tested.forInSingleLine(1);
      tested.forInSingleLine(2);

      assertLines(15, 16, 2);
      assertLine(15, 2, 2, 3, 1); // TODO: should have 5 segments
      assertLine(16, 1, 1, 2);

      findMethodData(15);
      assertPaths(2, 2, 2);
      assertMethodLines(15, 16);
      assertPath(4, 1);
      assertPath(5, 1);
   }

   @Test
   public void forEachArrayElement()
   {
      int sum = tested.forEachArrayElement(1, 2, 3);
      assertEquals(6, sum);

      assertLines(20, 26, 4);
      assertLine(20, 1, 1, 1);
      assertLine(22, 2, 2, 4, 3);
      assertLine(23, 1, 1, 3);
      assertLine(26, 1, 1, 1);
   }

   @Test
   public void forEachCollectionElement()
   {
      String result = tested.forEachCollectionElement(asList("a", "b", "c"));
      assertEquals("abc", result);

      assertLines(31, 37, 5);
      assertLine(31, 1, 1, 1);
      assertLine(33, 2, 2, 1, 3);
      assertLine(34, 1, 1, 3);
      assertLine(35, 1, 1, 3);
      assertLine(37, 1, 1, 1);
   }

   @Test
   public void forUsingIterator()
   {
      List<? extends Number> numbers = new ArrayList<Number>(asList(1, 0L, 2.0));
      tested.forUsingIterator(numbers);

      assertLines(42, 49, 6);
      assertLine(42, 1, 1, 1, 3); // TODO: should have 2 segments
      assertLine(43, 1, 1, 3);
      assertLine(45, 1, 1, 3);
      assertLine(46, 1, 1, 1);
      assertLine(48, 1, 1, 3);
      assertLine(49, 1, 1, 1);
   }
}