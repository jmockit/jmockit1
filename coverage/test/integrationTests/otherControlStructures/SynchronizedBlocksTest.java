/*
 * Copyright (c) 2006-2011 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package integrationTests.otherControlStructures;

import org.junit.*;

public final class SynchronizedBlocksTest
{
   private final SynchronizedBlocks tested = new SynchronizedBlocks();

   @Test
   public void doInSynchronizedBlock()
   {
      tested.doInSynchronizedBlock();
   }

   @Test
   public void doInSynchronizedBlockWithTrue()
   {
      tested.doInSynchronizedBlockWithParameter(true);
   }

   @Test(expected = RuntimeException.class)
   public void doInSynchronizedBlockWithFalse()
   {
      tested.doInSynchronizedBlockWithParameter(false);
   }
}
