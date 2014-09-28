/*
 * Copyright (c) 2006-2014 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package powermock.examples.tutorial.domainmocking.impl;

import org.junit.*;

import mockit.*;

import static org.junit.Assert.*;
import powermock.examples.tutorial.domainmocking.*;
import powermock.examples.tutorial.domainmocking.domain.*;

/**
 * <a href="http://code.google.com/p/powermock/source/browse/trunk/examples/tutorial/src/solution/java/demo/org/powermock/examples/tutorial/domainmocking/impl/SampleServiceImplTest.java">PowerMock version</a>
 */
public final class SampleServiceImpl_JMockit_Test
{
   @Tested SampleServiceImpl tested;
   @Injectable PersonService personService;
   @Injectable EventService eventService;

   @Test
   public void createPerson(@Mocked final BusinessMessages businessMessages)
   {
      String firstName = "firstName";
      String lastName = "lastName";
      final Person person = new Person(firstName, lastName);

      assertTrue(tested.createPerson(firstName, lastName));

      new VerificationsInOrder() {{
         personService.create(person, withInstanceLike(businessMessages));
         businessMessages.hasErrors();
      }};
   }

   @Test
   public void createPersonWithBusinessError(@Mocked final BusinessMessages businessMessages)
   {
      String firstName = "firstName";
      String lastName = "lastName";
      final Person person = new Person(firstName, lastName);

      new Expectations() {{
         businessMessages.hasErrors(); result = true;
      }};

      assertFalse(tested.createPerson(firstName, lastName));

      new VerificationsInOrder() {{
         personService.create(person, withInstanceLike(businessMessages));
         eventService.sendErrorEvent(person, withInstanceLike(businessMessages));
      }};
   }

   // Notice that this test does not in fact need any mocking, but just for demonstration...
   @Test(expected = SampleServiceException.class)
   public void createPersonWithIllegalName(@Mocked Person person)
   {
      final String firstName = "firstName";
      final String lastName = "lastName";

      new Expectations() {{
         new Person(firstName, lastName); result = new IllegalArgumentException("test");
      }};

      tested.createPerson(firstName, lastName);
   }
}
