/*
 * Copyright (c) 2006-2014 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package powermock.examples;

import powermock.examples.dependencymanagement.*;
import powermock.examples.domain.*;

import org.junit.*;
import static org.junit.Assert.*;

import mockit.*;

/**
 * <a href="http://code.google.com/p/powermock/source/browse/trunk/examples/AbstractFactory/src/test/java/powermock/examples/MyServiceUserTest.java">PowerMock version</a>
 */
public final class MyServiceUser_JMockit_Test
{
   @Tested MyServiceUser tested;
   @Mocked DependencyManager dependencyManagerMock;

   @Test
   public void getNumberOfPersons()
   {
      final Person[] persons = {
         new Person("Rogério", "Liesenfeld", "MockStreet"),
         new Person("John", "Doe", "MockStreet2")
      };

      new Expectations() {{
         DependencyManager.getInstance().getMyService().getAllPersons(); result = persons;
      }};

      int numberOfPersons = tested.getNumberOfPersons();

      assertEquals(2, numberOfPersons);
   }
}
