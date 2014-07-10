package org.jmock.samples.cache;

import java.util.*;

// Created from usage in CacheTest and to pass the test.
public final class Cache
{
   private final Map<String, Object> cachedItems = new HashMap<String, Object>();
   private final Map<String, Long> loadTimes = new HashMap<String, Long>();
   private final Clock clock;
   private final Loader loader;
   private final CacheReloadPolicy reloadPolicy;

   public Cache(Clock clock, CacheReloadPolicy reloadPolicy, Loader loader)
   {
      this.clock = clock;
      this.loader = loader;
      this.reloadPolicy = reloadPolicy;
   }

   public Object lookup(String key)
   {
      Long loadTime = loadTimes.get(key);
      long currTime = clock.time();

      if (loadTime != null && !reloadPolicy.shouldReload(loadTime, currTime)) {
         return cachedItems.get(key);
      }

      loadTimes.put(key, currTime);
      Object value = loader.load(key);
      cachedItems.put(key, value);
      return value;
   }
}
