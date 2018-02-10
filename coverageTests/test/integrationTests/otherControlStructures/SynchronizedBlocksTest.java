package integrationTests.otherControlStructures;

import org.junit.*;

public final class SynchronizedBlocksTest
{
   private final SynchronizedBlocks tested = new SynchronizedBlocks();

   @Test
   public void doInSynchronizedBlock() {
      tested.doInSynchronizedBlock();
   }

   @Test
   public void doInSynchronizedBlockWithTrue() {
      tested.doInSynchronizedBlockWithParameter(true);
   }

   @Test(expected = RuntimeException.class)
   public void doInSynchronizedBlockWithFalse() {
      tested.doInSynchronizedBlockWithParameter(false);
   }
}
