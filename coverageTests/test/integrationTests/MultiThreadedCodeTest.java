package integrationTests;

import org.junit.*;

public final class MultiThreadedCodeTest extends CoverageTest
{
   MultiThreadedCode tested;

   @Test
   public void nonBlockingOperation() throws Exception {
      Thread worker = MultiThreadedCode.nonBlockingOperation();
      worker.join();

      assertLines(7, 18, 7);
      assertLine(7, 1, 1, 1);
      assertLine(12, 1, 1, 1);
      assertLine(14, 1, 1, 1);
      assertLine(17, 1, 1, 1);
      assertLine(18, 1, 1, 1);
   }
}