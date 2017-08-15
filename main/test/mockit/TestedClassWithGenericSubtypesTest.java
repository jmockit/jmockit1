/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit;

import org.junit.*;
import static org.junit.Assert.*;

public final class TestedClassWithGenericSubtypesTest
{
   static class GenericClass<T> { T value; }
   static class Subclass1 extends GenericClass<String> {}
   static class Subclass2 extends GenericClass<Double> {}
   static class SUT1
   {
      GenericClass<String> dependency1;
      GenericClass<Double> dependency2;
   }

   @Test
   public void injectSubclassInstancesIntoFieldsOfBaseGenericClass(
      @Tested SUT1 sut, @Injectable Subclass1 s1, @Injectable Subclass2 s2)
   {
      assertSame(s1, sut.dependency1);
      assertSame(s2, sut.dependency2);
   }

   @SuppressWarnings("unused") public interface GenericInterface<T> {}
   static class Impl1 implements GenericInterface<String> {}
   static class Impl2 implements GenericInterface<Double> {}
   static class SUT2
   {
      final GenericInterface<String> dependency1;
      final GenericInterface<Double> dependency2;

      SUT2(GenericInterface<String> dep1, GenericInterface<Double> dep2)
      {
         dependency1 = dep1;
         dependency2 = dep2;
      }
   }

   @Test
   public void injectImplementationInstancesIntoFieldsOfBaseGenericInterface(
      @Tested SUT2 sut, @Injectable Impl1 i1, @Injectable Impl2 i2)
   {
      assertSame(i1, sut.dependency1);
      assertSame(i2, sut.dependency2);
   }

   static final class Dependency {}
   static final class Service1 extends GenericClass<Dependency> {}
   static final class Service2 { Service1 service1; }

   @Test
   public void injectInstanceIntoTypeVariableOfSecondLevelClass(@Tested(fullyInitialized = true) Service2 service2)
   {
      Service1 service1 = service2.service1;
      assertNotNull(service1);
      assertTrue(service1.value instanceof Dependency);
   }
}
