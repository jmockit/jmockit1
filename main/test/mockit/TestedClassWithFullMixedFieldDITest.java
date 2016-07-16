package mockit;

import javax.inject.*;

import org.junit.*;
import static org.junit.Assert.*;

public final class TestedClassWithFullMixedFieldDITest
{
   static class TestedClass
   {
      @Inject Dependency dependency;
      StringBuilder text;
   }

   static class Dependency { String value; }

   @Tested(fullyInitialized = true) TestedClass tested;

   @Test
   public void verifyThatFieldsFromJRETypesAreNotInitialized()
   {
      assertNull(tested.text);
      assertNull(tested.dependency.value);
   }
}
