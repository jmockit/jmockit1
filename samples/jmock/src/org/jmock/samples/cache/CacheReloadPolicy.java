package org.jmock.samples.cache;

// Created from usage in CacheTest.
//
// Note that none of the methods in this class can be final, because jMock's ClassImposterizer is
// not able to imposterize any final methods.
public abstract class CacheReloadPolicy
{
   public abstract boolean shouldReload(long loadTime, long fetchTime);
}
