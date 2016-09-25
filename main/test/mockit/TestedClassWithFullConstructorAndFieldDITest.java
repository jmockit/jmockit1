package mockit;

import org.junit.*;
import static org.junit.Assert.*;

public final class TestedClassWithFullConstructorAndFieldDITest
{
   static class TestedClass
   {
      String value;
      DependencyWithFieldDIOnly dependency1;
      DependencyWithConstructorDIOnly dependency2;
   }

   static class DependencyWithFieldDIOnly
   {
      String value;
   }

   static class DependencyWithConstructorDIOnly
   {
      final String value;
      DependencyWithConstructorDIOnly(String value) { this.value = value; }
   }

   @Tested(fullyInitialized = true) TestedClass tested;
   @Injectable String first = "text";

   @Test
   public void verifyEachTargetFieldGetsInjectedWithFirstUnusedInjectableWhetherThroughFieldOrConstructorInjection()
   {
      assertEquals("text", tested.value);
      assertEquals("text", tested.dependency1.value);
      assertEquals("text", tested.dependency2.value);
   }
}
