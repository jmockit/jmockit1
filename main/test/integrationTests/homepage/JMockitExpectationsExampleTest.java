/*
 * Copyright (c) 2006-2014 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package integrationTests.homepage;

import org.junit.*;

import mockit.*;

import static org.junit.Assert.*;

public final class JMockitExpectationsExampleTest
{
   // This is a mock field. A mocked instance is automatically created and assigned to it, for each test.
   @Mocked DependencyXyz mock;

   @Test
   public void testDoOperationAbc()
   {
      ServiceAbc sut = new ServiceAbc();

      new Expectations() {{
         // Records an expectation for an specific instance method of the mocked type,
         // which is expected to be executed on any instance of that type:
         mock.doSomething("test"); result = 123;
      }};

      // In ServiceAbc#doOperationAbc(String s): "new DependencyXyz().doSomething(s);"
      Object result = sut.doOperationAbc("test");

      assertNotNull(result);

      // That all expectations recorded were actually executed in the replay phase is automatically
      // verified at this point, through transparent integration with the JUnit/TestNG test runner.
   }
}
