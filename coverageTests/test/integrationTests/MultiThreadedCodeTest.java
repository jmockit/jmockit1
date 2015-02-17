/*
 * Copyright (c) 2006-2013 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package integrationTests;

import org.junit.*;

public final class MultiThreadedCodeTest extends CoverageTest
{
   MultiThreadedCode tested;

   @Test
   public void nonBlockingOperation() throws Exception
   {
      Thread worker = tested.nonBlockingOperation();
      worker.join();

      assertLines(7, 17, 7);
      assertLine(7, 1, 1, 1);
      assertLine(11, 1, 1, 1);
      assertLine(13, 1, 1, 1);
      assertLine(16, 1, 1, 1);
      assertLine(17, 1, 1, 1);

      findMethodData(7);
      assertPaths(1, 1, 1);
      assertMethodLines(7, 17);
      assertPath(2, 1);
   }
}