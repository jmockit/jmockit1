/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
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

   static class DependencyWithFieldDIOnly { String value; }

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

   @SuppressWarnings("unused")
   static class ClassWithMultipleConstructors
   {
      final int value;
      ClassWithMultipleConstructors() { value = 1; }
      ClassWithMultipleConstructors(int value) { throw new RuntimeException("Not to be called"); }
   }

   @Tested(fullyInitialized = true) ClassWithMultipleConstructors tested2;

   @Test
   public void verifyInitializationOfClassWithMultipleConstructors()
   {
      assertEquals(1, tested2.value);
   }

   static class ClassWithFieldToInject { ClassWithMultipleConstructors dependency; }

   @Tested(fullyInitialized = true) ClassWithFieldToInject tested3;

   @Test
   public void verifyInitializationOfClassWithFieldOfAnotherClassHavingMultipleConstructors()
   {
      assertNotNull(tested3.dependency);
      assertEquals(1, tested3.dependency.value);
   }

   static final class Dependency {}

   @SuppressWarnings("unused")
   static final class AnotherClassWithMultipleConstructors
   {
      final Dependency dep;
      AnotherClassWithMultipleConstructors() { dep = new Dependency(); }
      AnotherClassWithMultipleConstructors(Dependency dep) { this.dep = dep; }
   }

   @Tested Dependency dep;
   @Tested(fullyInitialized = true) AnotherClassWithMultipleConstructors tested4;

   @Test
   public void verifyInitializationOfClassWithMultipleConstructorsHavingTestedFieldForParameter()
   {
      assertSame(dep, tested4.dep);
   }
}
