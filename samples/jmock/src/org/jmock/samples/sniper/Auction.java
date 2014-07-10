package org.jmock.samples.sniper;

public interface Auction
{
   void bid(Money amount) throws AuctionException;
}
