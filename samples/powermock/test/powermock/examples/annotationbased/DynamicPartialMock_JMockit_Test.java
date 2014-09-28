/*
 * Copyright (c) 2006-2014 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package powermock.examples.annotationbased;

import org.junit.*;

import mockit.*;

import static org.junit.Assert.*;
import powermock.examples.annotationbased.dao.*;

public final class DynamicPartialMock_JMockit_Test
{
   @Test
   public void useDynamicPartialMock()
   {
      final SomeDao someDao = new SomeDao();
      SomeService someService = new SomeService(someDao);

      // Only invocations recorded inside this block will stay mocked for the replay.
      new Expectations(someDao) {{ someDao.getSomeData(); result = "test"; }};

      assertEquals("test", someService.getData());
      assertNotNull(someService.getMoreData());
   }
}
