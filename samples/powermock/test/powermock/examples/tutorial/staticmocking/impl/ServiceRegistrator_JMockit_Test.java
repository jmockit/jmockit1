/*
 * Copyright (c) 2006-2014 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package powermock.examples.tutorial.staticmocking.impl;

import java.util.*;

import org.junit.*;

import mockit.*;

import static mockit.Deencapsulation.*;
import static org.junit.Assert.*;
import powermock.examples.tutorial.staticmocking.osgi.*;

/**
 * Unit tests using the JMockit API for the
 * {@link powermock.examples.tutorial.staticmocking.impl.ServiceRegistrator} class.
 * <p/>
 * <a href="http://code.google.com/p/powermock/source/browse/trunk/examples/tutorial/src/solution/java/demo/org/powermock/examples/tutorial/staticmocking/impl/ServiceRegistratorTest.java">PowerMock version</a>
 */
public final class ServiceRegistrator_JMockit_Test
{
   @Tested ServiceRegistrator tested;
   @Mocked ServiceRegistration serviceRegistrationMock;
   @Mocked final IdGenerator unused = null;

   @Test
   public void registerService(@Injectable BundleContext bundleContextMock)
   {
      // Data for the test:
      String name = "a name";
      Object serviceImpl = new Object();
      final long expectedId = 42;

      new Expectations() {{ IdGenerator.generateNewId(); result = expectedId; }};

      // Code under test is exercised:
      long actualId = tested.registerService(name, serviceImpl);

      // State-based verifications (simplified):
      //noinspection unchecked
      Map<Long, ServiceRegistration> serviceRegistrations = getField(tested, Map.class);
      assertEquals(1, serviceRegistrations.size());
      assertSame(serviceRegistrationMock, serviceRegistrations.get(actualId));
   }

   @Test
   public void unregisterService()
   {
      //noinspection unchecked
      Map<Long, ServiceRegistration> serviceRegistrations = getField(tested, Map.class);
      long id = 1L;
      serviceRegistrations.put(id, serviceRegistrationMock);

      tested.unregisterService(id);

      new Verifications() {{ serviceRegistrationMock.unregister(); }};

      assertTrue(serviceRegistrations.isEmpty());
   }

   @Test(expected = IllegalStateException.class)
   public void unregisterServiceWithIdWhichDoesntExist()
   {
      tested.unregisterService(1L);

      // No invocation on any mock is expected.
      new FullVerifications() {};
   }
}
