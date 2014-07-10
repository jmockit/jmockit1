package org.jmock.samples.cache;

// Created from usage in CacheTest.
//
// Note that this class cannot be final, because jMock's ClassImposterizer is not able to
// imposterize final classes.
public class Clock
{
   public long time()
   {
      return 0;
   }
}
