/*
 * Copyright (c) 2006-2015 Rogério Liesenfeld
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
 * This test class replicates the applicable {@code @Inject} TCK tests, as it was not possible to use the test cases
 * nested in the {@link Convertible} class directly.
 * <p/>
 * These tests correspond to those in the TCK which don't require support for features intentionally left out in
 * JMockit. They are described below.
 * <ol>
 * <li>
 *    <em>Method</em> injection.
 *    Historically, method injection originated with the use of public setters in the Spring framework, which were
 *    meant to be used in test code. In Java EE, such setters are not commonly used. Also, modern testing tools,
 *    including all mocking libraries, have their own annotation-based mechanisms for injection, dispensing with the
 *    need for users to explicitly call setter methods in tests.
 * </li>
 * <li>
 *    <em>Constructor</em> injection into lower-level dependencies.
 *    JMockit only supports <em>field</em> injection into such dependencies, through the
 *    {@link Tested#fullyInitialized} annotation attribute.
 *    Again, historically the use of constructor injection originated in the Guice framework, for direct use in test
 *    code. Just like setters, in Java EE such constructors are not commonly used. Also, use of modern testing tools
 *    avoids the need for hand-coded injection in tests.
 * </li>
 * <li>
 *    Selection of a particular implementation class to be instantiated for a <em>qualified</em> injection point, as
 *    indicated through the {@code javax.inject.Qualifier} meta-annotation.
 *    It would be possible for JMockit to use the qualifiers applied on a mock field or mock parameter to find a
 *    matching injection point of the same type.
 *    However, it's already possible to disambiguate by type and name (including constructor parameter names as well as
 *    mock parameter names), so support for qualifiers would be of little value.
 *    This said, the special {@code @Named} qualifier <em>is</em> currently supported.
 * </li>
 * </ol>
 */
public final class MockedDependenciesTest
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
   @Injectable Seat fieldPlainSeat;
   @Injectable Seat fieldDriversSeat;
   @Injectable Tire fieldPlainTire;
   @Injectable Tire spare; // matches the name given by the @Named annotation, not the field name
   @Injectable Seat driversSeatA;
   @Injectable Seat driversSeatB;

   // For field injection of "Provider<T>" fields:
   @Injectable Engine engineToBeProvided;
   @Injectable Seat fieldPlainSeatProvider;
   @Injectable Provider<Tire> fieldPlainTireProvider;
   @Injectable Provider<Seat> fieldDriversSeatProvider;
   @Injectable Provider<Tire> fieldSpareTireProvider; // TODO: add @Named("spare"), since "spare" field already exists

   // For static field injection:
   @Injectable Seat staticFieldPlainSeat;
   @Injectable Tire staticFieldPlainTire;
   @Injectable Seat staticFieldDriversSeat;
   @Injectable Tire staticFieldSpareTire;
   @Injectable Seat staticFieldPlainSeatProvider;
   @Injectable Tire staticFieldPlainTireProvider;
   @Injectable Seat staticFieldDriversSeatProvider;
   @Injectable Tire staticFieldSpareTireProvider; // TODO: add @Named("spare")

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
      // TODO: uncomment once support for @Named injectables is added
      // assertNotSame(car.fieldSpareTireProvider.get(), car.fieldSpareTireProvider.get());
   }

   @Test
   public void staticFieldInjectionWithValues()
   {
      assertNotNull(Convertible.staticFieldPlainSeat);
      assertNotNull(Convertible.staticFieldPlainTire);
      assertNotNull(Convertible.staticFieldDriversSeat);
      // TODO: uncomment once support for @Named injectables is added
      // assertNotNull(Convertible.staticFieldSpareTire);
   }

   @Test
   public void staticFieldInjectionWithProviders()
   {
      assertNotNull(Convertible.staticFieldPlainSeatProvider.get());
      assertNotNull(Convertible.staticFieldPlainTireProvider.get());
      assertNotNull(Convertible.staticFieldDriversSeatProvider.get());
      // TODO: uncomment once support for @Named injectables is added
      // assertNotNull(Convertible.staticFieldSpareTireProvider.get());
   }
}
