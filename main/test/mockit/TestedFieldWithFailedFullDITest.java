package mockit;

import org.junit.*;
import org.junit.rules.*;

public final class TestedFieldWithFailedFullDITest
{
   @Rule public final ExpectedException thrown = ExpectedException.none();

   @Before
   public void configureExpectedException() {
      thrown.expect(IllegalStateException.class);
      thrown.expectMessage("Missing @Tested or @Injectable");
      thrown.expectMessage("for parameter \"value\" in constructor ClassWithParameterizedConstructor(int value)");
      thrown.expectMessage("when initializing field ");
      thrown.expectMessage("dependency");
      thrown.expectMessage("of @Tested object \"" + ClassWithFieldOfClassHavingParameterizedConstructor.class.getSimpleName() + " tested");
   }

   static class ClassWithFieldOfClassHavingParameterizedConstructor { ClassWithParameterizedConstructor dependency; }
   static class ClassWithParameterizedConstructor { ClassWithParameterizedConstructor(@SuppressWarnings("unused") int value) {} }

   @Tested(fullyInitialized = true) ClassWithFieldOfClassHavingParameterizedConstructor tested;

   @Test
   public void attemptToUseTestedObjectWhoseCreationFailedDueToInjectableWithNullValue() {}

   @Test
   public void attemptToUseTestedObjectWhoseCreationFailedDueToInjectableWithNullValue2() {}
}