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
   static class Dependency2 {}

   @Test
   public void verifyThatFieldsFromJRETypesAreNotInitialized(@Tested(fullyInitialized = true) TestedClass tested)
   {
      assertNull(tested.text);
      assertNull(tested.dependency.value);
   }

   static class TestedClass2
   {
      @Inject Dependency dependency1;
      Dependency2 dependency2;
   }

   @Test
   public void verifyThatFieldsOfUserTypesAreInitializedEvenOnlySomeAreAnnotated(
      @Tested(fullyInitialized = true) TestedClass2 tested)
   {
      assertNotNull(tested.dependency1);
      assertNotNull(tested.dependency2);
   }
}
