/*
 * Copyright (c) 2006-2014 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package powermock.examples.staticmocking;

import org.junit.*;

import mockit.*;

import static org.junit.Assert.*;

/**
 * <a href="http://code.google.com/p/powermock/source/browse/trunk/examples/DocumentationExamples/src/test/java/powermock/examples/staticmocking/ServiceRegistratorTest.java">PowerMock version</a>
 */
public final class ServiceRegistrator_JMockit_Test
{
   @Test
   public void testRegisterService(@Mocked IdGenerator idGenerator)
   {
      final long expectedId = 42;
      ServiceRegistrator tested = new ServiceRegistrator();

      new Expectations() {{ IdGenerator.generateNewId(); result = expectedId; }};

      long actualId = tested.registerService(new Object());

      // Assert that the ID is correct.
      assertEquals(expectedId, actualId);
   }
}
