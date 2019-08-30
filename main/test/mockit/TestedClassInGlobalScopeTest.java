package mockit;

import org.junit.*;
import static org.junit.Assert.*;
import static org.junit.runners.MethodSorters.*;

@FixMethodOrder(NAME_ASCENDING)
public final class TestedClassInGlobalScopeTest
{
   static class TestedClass { Integer someValue; }

   @Tested(fullyInitialized = true, global = true) TestedClass testedGlobal;
   @Tested(fullyInitialized = true) TestedClass testedLocal;

   @Test
   public void useTestedObjectInFirstStepOfTestedScenario() {
      assertNull(testedGlobal.someValue);
      assertNotSame(testedGlobal, testedLocal);
      testedGlobal.someValue = 123;
   }

   @Test
   public void useTestedObjectInSecondStepOfTestedScenario() {
      assertNotNull(testedGlobal.someValue);
      assertNull(testedLocal.someValue);
   }
}