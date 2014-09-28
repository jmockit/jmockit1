/*
 * Copyright (c) 2006-2014 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package integrationTests.homepage;

import org.junit.*;

import mockit.*;

public final class JMockitVerificationsExampleTest
{
   @Test // notice the "mock parameters", whose values will be created automatically
   public void testDoAnotherOperation(@Mocked final AnotherDependency anotherMock, @Mocked final DependencyXyz mock)
   {
      new Expectations() {{
         mock.doSomething("test"); result = 123;
      }};

      // In ServiceAbc#doAnotherOperationAbc(String s): "new DependencyXyz().doSomething(s);"
      // and "new AnotherDependency().complexOperation(1, obj);".
      new ServiceAbc().doAnotherOperation("test");

      new Verifications() {{
         anotherMock.complexOperation(anyInt, null);
      }};
   }
}
