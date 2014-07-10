/*
 * Copyright (c) 2006-2011 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package integrationTests.otherControlStructures;

import org.junit.*;

import integrationTests.*;

public final class TryCatchFinallyStatementsTest extends CoverageTest
{
   final TryCatchFinallyStatements tested = new TryCatchFinallyStatements();

   @Test
   public void tryCatch()
   {
      tested.tryCatch();
   }

   @Test
   public void tryCatchWhichThrowsAndCatchesException()
   {
      tested.tryCatchWhichThrowsAndCatchesException();
   }
}
