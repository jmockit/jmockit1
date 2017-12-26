/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package otherTests.testng;

import java.util.concurrent.atomic.*;

import static org.testng.Assert.*;
import org.testng.annotations.*;

// Just to make sure no NPEs or other exceptions occur from JMockit-TestNG integration.
public final class ParallelExecutionTest
{
   final AtomicInteger counter = new AtomicInteger();

   @Test(threadPoolSize = 4, invocationCount = 10)
   public void parallelExecution()
   {
      counter.incrementAndGet();
   }

   @AfterClass
   public void checkCounter()
   {
      assertEquals(counter.get(), 10);
   }
}
