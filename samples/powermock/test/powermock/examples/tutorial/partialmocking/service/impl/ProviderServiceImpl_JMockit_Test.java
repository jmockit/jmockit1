/*
 * Copyright (c) 2006-2014 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package powermock.examples.tutorial.partialmocking.service.impl;

import java.util.*;

import powermock.examples.tutorial.partialmocking.dao.*;
import powermock.examples.tutorial.partialmocking.dao.domain.impl.*;
import powermock.examples.tutorial.partialmocking.domain.*;
import powermock.examples.tutorial.partialmocking.service.*;

import org.junit.*;
import static org.junit.Assert.*;

import mockit.*;

import static mockit.Deencapsulation.*;

/**
 * Demonstrates <em>partial mocking</em> of the class under test, where the methods to be mocked are determined from
 * those actually called in the record phase.
 * <p/>
 * The first four tests mock a private method defined in the class under test, while the last two tests directly
 * exercise this private method.
 * This is not recommended, though; instead, unit tests should be created only for the non-private methods in the class
 * under test.
 * <p/>
 * <a href="http://code.google.com/p/powermock/source/browse/trunk/examples/tutorial/src/solution/java/demo/org/powermock/examples/tutorial/partialmocking/service/impl/ProviderServiceImplTest.java">PowerMock version</a>
 */
public final class ProviderServiceImpl_JMockit_Test
{
   @Tested @Mocked ProviderServiceImpl tested;

   @Test
   public void testGetAllServiceProviders()
   {
      final Set<ServiceProducer> expectedServiceProducers = new HashSet<>();
      expectedServiceProducers.add(new ServiceProducer(1, "mock name"));

      new Expectations() {{
         invoke(tested, "getAllServiceProducers"); result = expectedServiceProducers;
      }};

      Set<ServiceProducer> actualServiceProviders = tested.getAllServiceProviders();

      assertSame(expectedServiceProducers, actualServiceProviders);
   }

   @Test
   public void testGetAllServiceProviders_noServiceProvidersFound()
   {
      Set<ServiceProducer> expectedServiceProducers = new HashSet<>();

      new Expectations() {{ invoke(tested, "getAllServiceProducers"); result = null; }};

      Set<ServiceProducer> actualServiceProviders = tested.getAllServiceProviders();

      assertNotSame(expectedServiceProducers, actualServiceProviders);
      assertEquals(expectedServiceProducers, actualServiceProviders);
   }

   @Test
   public void testGetServiceProvider_found()
   {
      int expectedServiceProducerId = 1;
      ServiceProducer expected = new ServiceProducer(expectedServiceProducerId, "mock name");

      final Set<ServiceProducer> serviceProducers = new HashSet<>();
      serviceProducers.add(expected);

      new Expectations() {{ invoke(tested, "getAllServiceProducers"); result = serviceProducers; }};

      ServiceProducer actual = tested.getServiceProvider(expectedServiceProducerId);

      assertSame(expected, actual);
   }

   @Test
   public void testGetServiceProvider_notFound()
   {
      new Expectations() {{
         invoke(tested, "getAllServiceProducers");
         // An empty collection is the default return value, so we don't have to record it here.
         // returns(new HashSet<ServiceProducer>());
      }};

      ServiceProducer actual = tested.getServiceProvider(1);

      assertNull(actual);
   }

   static class RealProviderServiceExpectations extends Expectations
   {
      private final ProviderService providerService;

      RealProviderServiceExpectations(ProviderDao providerDao)
      {
         providerService = new ProviderServiceImpl();
         setField(providerService, providerDao);
      }

      protected Set<ServiceProducer> getAllServiceProducers()
      {
         return invoke(providerService, "getAllServiceProducers");
      }
   }

   @Test
   public void getAllServiceProducers(@Mocked final ProviderDao providerDao)
   {
      String expectedName = "mock name";
      int expectedId = 1;

      final Set<ServiceArtifact> serviceArtifacts = new HashSet<>();
      serviceArtifacts.add(new ServiceArtifact(expectedId, expectedName));

      RealProviderServiceExpectations expectations = new RealProviderServiceExpectations(providerDao) {{
         providerDao.getAllServiceProducers();
         result = serviceArtifacts;
      }};

      Set<ServiceProducer> allProducers = expectations.getAllServiceProducers();

      assertEquals(1, allProducers.size());
      assertTrue(allProducers.contains(new ServiceProducer(expectedId, expectedName)));
   }

   @Test
   public void getAllServiceProducersOnEmptyProviderService(@Mocked final ProviderDao providerDao)
   {
      RealProviderServiceExpectations expectations = new RealProviderServiceExpectations(providerDao) {{
         providerDao.getAllServiceProducers(); result = new HashSet<ServiceArtifact>();
      }};

      Set<ServiceProducer> allProducers = expectations.getAllServiceProducers();

      assertTrue(allProducers.isEmpty());
   }
}
