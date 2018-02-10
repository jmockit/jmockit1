package mockit;

import javax.inject.*;

import org.junit.*;
import static org.junit.Assert.*;

public final class StandardDI2Test
{
   static class TestedClass {
      TestedClass() { throw new RuntimeException("Must not occur"); }
      @Inject TestedClass(Runnable action) {}
   }

   @Tested TestedClass tested;

   @Test(expected = IllegalArgumentException.class)
   public void attemptToCreateTestedObjectThroughAnnotatedConstructorWithMissingInjectables() {
      fail();
   }
}
