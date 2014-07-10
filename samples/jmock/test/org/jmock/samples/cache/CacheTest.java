package org.jmock.samples.cache;

import org.jmock.*;
import org.jmock.auto.*;
import org.jmock.integration.junit4.*;
import org.jmock.lib.legacy.*;
import static org.junit.Assert.*;
import org.junit.*;

/**
 * The base for this test can be found in the
 * <a href="http://www.jmock.org/expectations.html">jMock Cookbook</a>.
 */
public final class CacheTest
{
   private static final String KEY = "cachedItem";
   private static final Object VALUE = new Object();

   @Rule public final JUnitRuleMockery context = new JUnitRuleMockery() {{
      setImposteriser(ClassImposteriser.INSTANCE);
   }};

   @Mock Clock clock;
   @Mock Loader loader;
   @Mock CacheReloadPolicy reloadPolicy;

   @Test // (original jMock example test, with additional comments)
   public void returnsCachedObjectWithinTimeout()
   {
      // Note that jMock forces the Cache class to have a way to be passed the clock and loader
      // objects (i.e., constructor or setter dependency injection), effectively preventing other
      // design choices, such as using a Service Locator or simply the "new" operator.
      //
      // For objects which may have multiple implementations such as the loader, using dependency
      // injection is perfectly fine. The clock object, however, looks more like an artifact created
      // only for the purpose of unit testing under jMock's constraints.
      Cache cache = new Cache(clock, reloadPolicy, loader);

      final long loadTime = 10;

      context.checking(new Expectations() {{ // (this is "jMock Expectations", not JMockit's version)
         oneOf(clock).time(); will(returnValue(loadTime));
         oneOf(loader).load(KEY); will(returnValue(VALUE));
      }});

      Object actualValueFromFirstLookup = cache.lookup(KEY);

      context.checking(new Expectations() {
         final long fetchTime = 200;

         {
            oneOf(clock).time(); will(returnValue(fetchTime));
            allowing(reloadPolicy).shouldReload(loadTime, fetchTime); will(returnValue(false));
         }
      });

      Object actualValueFromSecondLookup = cache.lookup(KEY);

      assertSame("should be loaded object", VALUE, actualValueFromFirstLookup);
      assertSame("should be cached object", VALUE, actualValueFromSecondLookup);
   }

   @Test
   public void returnsCachedObjectWithinTimeout_usingTwoExpectationsBlocksForOneReplayPhase()
   {
      Cache cache = new Cache(clock, reloadPolicy, loader);
      final long loadTime = 10;

      context.checking(new Expectations() {{
         oneOf(clock).time(); will(returnValue(loadTime));
         oneOf(loader).load(KEY); will(returnValue(VALUE));
      }});

      context.checking(new Expectations() {
         final long fetchTime = 200;

         {
            oneOf(clock).time(); will(returnValue(fetchTime));
            allowing(reloadPolicy).shouldReload(loadTime, fetchTime); will(returnValue(false));
         }
      });

      // Replay phase:
      Object actualValueFromFirstLookup = cache.lookup(KEY);
      Object actualValueFromSecondLookup = cache.lookup(KEY);

      assertSame("should be loaded object", VALUE, actualValueFromFirstLookup);
      assertSame("should be cached object", VALUE, actualValueFromSecondLookup);
   }

   @Test
   public void returnsCachedObjectWithinTimeout_usingSingleExpectationsBlock()
   {
      Cache cache = new Cache(clock, reloadPolicy, loader);
      final long loadTime = 10;

      context.checking(new Expectations() {
         // For first cache lookup:
         {
            oneOf(clock).time(); will(returnValue(loadTime));
            oneOf(loader).load(KEY); will(returnValue(VALUE));
         }

         // For second cache lookup:
         final long fetchTime = 200;

         {
            oneOf(clock).time(); will(returnValue(fetchTime));
            allowing(reloadPolicy).shouldReload(loadTime, fetchTime); will(returnValue(false));
         }
      });

      Object actualValueFromFirstLookup = cache.lookup(KEY);
      Object actualValueFromSecondLookup = cache.lookup(KEY);

      assertSame("should be loaded object", VALUE, actualValueFromFirstLookup);
      assertSame("should be cached object", VALUE, actualValueFromSecondLookup);
   }
}
