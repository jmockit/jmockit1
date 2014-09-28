/*
 * Copyright (c) 2006-2014 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package org.jmock.samples.cache;

import org.junit.*;

import mockit.*;

import static org.junit.Assert.*;

public final class Cache_JMockit_Test
{
   private static final String KEY = "cachedItem";
   private static final Object VALUE = new Object();

   @Injectable Clock clock;
   @Injectable Loader loader;
   @Injectable CacheReloadPolicy reloadPolicy;

   /**
    * Note that with JMockit, the Cache class could obtain any or all of its dependencies internally instead of using
    * "constructor injection" (used automatically here).
    * Examples of such alternative mechanisms include the use of the design patterns Abstract Factory, Factory Method,
    * Singleton, and Service Locator. However, using such a pattern only makes sense when there is a good reason for
    * separating interface and implementation, such as when multiple implementations can realistically exist for a given
    * abstraction (separated interface), AND client code is expected to be able to select or switch between such
    * implementations at runtime. If that is not the case (and it often isn't), then it may be better to have no
    * separation between interface and implementation, and/or simply use the "new" operator to obtain dependencies
    * wherever they are needed. (Consider how an application typically uses List and ArrayList: declaring all
    * variables/fields/parameters of the interface type "List" -- the "program to an interface, not an implementation"
    * principle -- while referencing the implementation type in "new ArrayList()" expressions which "obtain the
    * dependency".)
    */
   @Tested Cache cache;

   @Test
   public void returnsCachedObjectWithinTimeout()
   {
      final long loadTime = 10;

      new Expectations() {{ // (this is "JMockit Expectations", not the jMock version)
         clock.time(); result = loadTime;
         loader.load(KEY); result = VALUE;
      }};

      Object actualValueFromFirstLookup = cache.lookup(KEY);

      new Expectations() {
         final long fetchTime = 200;

         {
            clock.time(); result = fetchTime;
            reloadPolicy.shouldReload(loadTime, fetchTime); result = false;
         }
      };

      Object actualValueFromSecondLookup = cache.lookup(KEY);

      assertSame("should be loaded object", VALUE, actualValueFromFirstLookup);
      assertSame("should be cached object", VALUE, actualValueFromSecondLookup);
   }

   @Test
   public void returnsCachedObjectWithinTimeout_usingTwoExpectationsBlocksForOneReplayPhase()
   {
      final long loadTime = 10;

      new StrictExpectations() {{
         clock.time(); result = loadTime;
         loader.load(KEY); result = VALUE;
      }};

      final long fetchTime = 200;

      new StrictExpectations() {{
         clock.time(); result = fetchTime;
         reloadPolicy.shouldReload(loadTime, fetchTime); result = false;
      }};

      // Replay phase:
      Object actualValueFromFirstLookup = cache.lookup(KEY);
      Object actualValueFromSecondLookup = cache.lookup(KEY);

      assertSame("should be loaded object", VALUE, actualValueFromFirstLookup);
      assertSame("should be cached object", VALUE, actualValueFromSecondLookup);
   }
}
