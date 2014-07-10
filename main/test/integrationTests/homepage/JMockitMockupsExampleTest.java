/*
 * Copyright (c) 2006-2012 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package integrationTests.homepage;

import org.junit.*;

import mockit.*;

import static org.junit.Assert.*;

public final class JMockitMockupsExampleTest
{
   @Test
   public void testDoOperationAbc()
   {
      // A "mock-up" class, defined and applied at the same time:
      new MockUp<DependencyXyz>() {
         @Mock(invocations = 1)
         int doSomething(String value)
         {
            assertEquals("test", value);
            return 123;
         }
      };

      // In ServiceAbc#doOperationAbc(String s): "new DependencyXyz().doSomething(s);"
      Object result = new ServiceAbc().doOperationAbc("test");

      assertNotNull(result);
   }
}
