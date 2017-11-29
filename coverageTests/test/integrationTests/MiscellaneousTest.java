package integrationTests;

import org.junit.*;

public final class MiscellaneousTest
{
   @Test
   public void methodWithIINCWideInstruction()
   {
      int i = 0;
      i += 1000; // compiled to opcode iinc_w
      assert i == 1000;
   }
}
