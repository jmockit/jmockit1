/*
 * Copyright (c) 2006-2015 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package org.atinject.tck.modern;

import javax.inject.*;

import org.junit.*;
import static org.junit.Assert.*;

import mockit.*;

import org.atinject.tck.modern.accessories.*;

public final class FullyInitializedDependenciesTest
{
   static { V8Engine.class.getName(); }

   @Tested(fullyInitialized = true) Convertible car;

   @Test
   public void fieldsInjected()
   {
      assertNotNull(car.cupholder);
      assertNotNull(car.spareTire);
   }

   @Test @Ignore("Engine is abstract, but has a concrete V8Engine class to be instantiated")
   public void providerReturnedValues()
   {
      assertNotNull(car.engineProvider.get());
   }

   @Test
   public void fieldInjectionWithValues()
   {
      assertFalse(car.fieldPlainSeat instanceof DriversSeat);
      assertFalse(car.fieldPlainTire instanceof SpareTire);
   }

   @Test
   public void fieldInjectionWithProviders()
   {
      assertNotNull(car.fieldPlainSeatProvider.get());
      assertNotNull(car.fieldPlainTireProvider.get());
   }

   @Test
   public void fieldInjectedProviderYieldsSingleton()
   {
      Provider<Seat> p = car.fieldPlainSeatProvider;
      assertSame(p.get(), p.get());
   }

   @Test @Ignore("Need to create new instance for every non-singleton dependency")
   public void singletonAnnotationNotInheritedFromSuperType()
   {
      assertNotSame(car.driversSeatA, car.driversSeatB);
   }

   @Test
   public void fieldInjectedProviderYieldsDistinctValues()
   {
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
