/*
 * Copyright (c) 2006-2013 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package org.atinject.tck.auto;

import javax.inject.*;

import org.atinject.tck.auto.accessories.*;

import org.junit.*;
import static org.junit.Assert.*;

import mockit.*;

import static mockit.Deencapsulation.*;

/**
 * Most tests in the TCK do not apply to JMockit.
 * They fall into one of the following categories:
 * <ol>
 * <li>They expect the mock objects themselves to be injected with second-level dependencies, rather than just
 * injecting them into the first-level tested object. For a mocking tool, it only makes sense to support injection
 * into top-level tested objects.</li>
 * <li>They require support for "method injection", which is intentionally not supported in JMockit.</li>
 * <li>They require that a particular implementation class be instantiated for a qualified injection point, as
 * indicated through the "javax.inject.Qualifier" annotation. When the injected type is mocked, however, there is no
 * implementation code to speak of, so qualifiers are simply ignored.
 * It would be possible for JMockit to use the qualifiers applied on a mock field/parameter to find a matching injection
 * point of the same type, though. However, it's already possible to disambiguate by type+name (including constructor
 * parameter names as well as mock parameter names), so support for qualifiers would add little value to justify the
 * cost.</li>
 * </ol>
 * This test class replicates the applicable TCK tests, as it was not possible to use the test cases nested in the
 * {@code Convertible} class directly.
 * (Also, there are several deficiencies in the original tests, which are fixed here.)
 */
@SuppressWarnings("ClassWithTooManyFields")
public final class JavaxInjectTCKTest
{
   @Tested Convertible car;

   // For constructor injection:
   @Injectable Seat plainSeat;
   @Injectable Tire plainTire;
   @Injectable Seat driversSeat;
   @Injectable Tire spareTire;

   // For constructor injection of "Provider<T>" parameters:
   @Injectable Seat plainSeatProvider; // Seat is a @Singleton
   @Injectable Provider<Tire> plainTireProvider; // Tire is not a @Singleton
   @Injectable Provider<Seat> driversSeatProvider; // DriversSeat is not a @Singleton
   @Injectable Provider<Tire> spareTireProvider; // SpareTire is not a @Singleton

   // For field injection:
   @Injectable SpareTire anotherSpareTire;
   @Injectable Cupholder cupholder;
   @Injectable Seat fieldDriversSeat;
   @Injectable Tire fieldSpareTire;
   @Injectable Seat driversSeatA;
   @Injectable Seat driversSeatB;

   // For field injection of "Provider<T>" fields:
   @Injectable Engine engineToBeProvided;
   @Injectable Seat fieldPlainSeatProvider;
   @Injectable Provider<Tire> fieldPlainTireProvider;
   @Injectable Provider<Seat> fieldDriversSeatProvider;
   @Injectable Provider<Tire> fieldSpareTireProvider;

   // For static field injection:
   @Injectable Seat staticFieldPlainSeat;
   @Injectable Tire staticFieldPlainTire;
   @Injectable Seat staticFieldDriversSeat;
   @Injectable Tire staticFieldSpareTire;
   @Injectable Seat staticFieldPlainSeatProvider;
   @Injectable Tire staticFieldPlainTireProvider;
   @Injectable Seat staticFieldDriversSeatProvider;
   @Injectable Tire staticFieldSpareTireProvider;

   // For non-Singletons:
   @Injectable Tire tire1;
   @Injectable Tire tire2;
   @Injectable DriversSeat driversSeat1;
   @Injectable DriversSeat driversSeat2;
   @Injectable SpareTire spareTire1;
   @Injectable SpareTire spareTire2;

   @Before
   public void configureProvidersForNonSingletons()
   {
      new NonStrictExpectations() {{
         plainTireProvider.get(); result = tire1; result = tire2;
         driversSeatProvider.get(); result = driversSeat1; result = driversSeat2;
         spareTireProvider.get(); result = spareTire1; result = spareTire2;
         fieldPlainTireProvider.get(); result = tire1; result = tire2;
         fieldDriversSeatProvider.get(); result = driversSeat1; result = driversSeat2;
         fieldSpareTireProvider.get(); result = spareTire1; result = spareTire2;
      }};
   }

   @Test
   public void fieldsInjected()
   {
      assertNotNull(car.cupholder);
      assertNotNull(car.spareTire);
   }

   @Test
   public void providerReturnedValues()
   {
      assertNotNull(car.engineProvider.get());
   }

   @Test
   public void constructorInjectionWithValues()
   {
      assertFalse(getField(car, "constructorPlainSeat") instanceof DriversSeat);
      assertFalse(getField(car, "constructorPlainTire") instanceof SpareTire);
   }

   @Test
   public void fieldInjectionWithValues()
   {
      assertFalse(car.fieldPlainSeat instanceof DriversSeat);
      assertFalse(car.fieldPlainTire instanceof SpareTire);
   }

   @Test
   public void constructorInjectionWithProviders()
   {
      Provider<Seat> p1 = getField(car, "constructorPlainSeatProvider");
      assertNotNull(p1.get());

      Provider<Tire> p2 = getField(car, "constructorPlainTireProvider");
      assertNotNull(p2.get());
   }

   @Test
   public void fieldInjectionWithProviders()
   {
      assertNotNull(car.fieldPlainSeatProvider.get());
      assertNotNull(car.fieldPlainTireProvider.get());
   }

   @Test
   public void constructorInjectedProviderYieldsSingleton()
   {
      Provider<Seat> p = getField(car, "constructorPlainSeatProvider");
      assertSame(p.get(), p.get());
   }

   @Test
   public void fieldInjectedProviderYieldsSingleton()
   {
      Provider<Seat> p = car.fieldPlainSeatProvider;
      assertSame(p.get(), p.get());
   }

   @Test
   public void singletonAnnotationNotInheritedFromSupertype()
   {
      assertNotSame(car.driversSeatA, car.driversSeatB);
   }

   @Test
   public void constructorInjectedProviderYieldsDistinctValues()
   {
      Provider<Seat> p1 = getField(car, "constructorDriversSeatProvider");
      assertNotSame(p1.get(), p1.get());

      Provider<Tire> p2 = getField(car, "constructorPlainTireProvider");
      assertNotSame(p2.get(), p2.get());

      Provider<Tire> p3 = getField(car, "constructorSpareTireProvider");
      assertNotSame(p3.get(), p3.get());
   }

   @Test
   public void fieldInjectedProviderYieldsDistinctValues()
   {
      assertNotSame(car.fieldDriversSeatProvider.get(), car.fieldDriversSeatProvider.get());
      assertNotSame(car.fieldPlainTireProvider.get(), car.fieldPlainTireProvider.get());
      assertNotSame(car.fieldSpareTireProvider.get(), car.fieldSpareTireProvider.get());
   }

   @Test
   public void staticFieldInjectionWithValues()
   {
      assertNotNull(Convertible.staticFieldPlainSeat);
      assertNotNull(Convertible.staticFieldPlainTire);
      assertNotNull(Convertible.staticFieldDriversSeat);
      assertNotNull(Convertible.staticFieldSpareTire);
   }

   @Test
   public void staticFieldInjectionWithProviders()
   {
      assertNotNull(Convertible.staticFieldPlainSeatProvider.get());
      assertNotNull(Convertible.staticFieldPlainTireProvider.get());
      assertNotNull(Convertible.staticFieldDriversSeatProvider.get());
      assertNotNull(Convertible.staticFieldSpareTireProvider.get());
   }
}
