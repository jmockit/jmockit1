package otherTests;

import org.junit.*;
import static org.junit.Assert.*;

import mockit.*;

public final class JUnit4Test
{
   @Mocked ClassWithObjectOverrides mock;

   @Test
   public void useMockedInstance() {
      assertFalse(mock.toString().isEmpty());
   }
}
