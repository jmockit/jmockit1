package mockit;

import org.junit.*;
import static org.junit.Assert.*;

class BaseTestClass {
   static final class Dependency {}
   @Tested final Dependency dependency = new Dependency();
}

public final class TestedClassInjectedFromBaseTest extends BaseTestClass
{
   static final class TestedClass { Dependency dependency; }

   @Tested(fullyInitialized = true) TestedClass tested;

   @Test
   public void verifyTestedObjectInjectedWithTestedDependencyProvidedByBaseTestClass() {
      assertSame(dependency, tested.dependency);
   }
}
