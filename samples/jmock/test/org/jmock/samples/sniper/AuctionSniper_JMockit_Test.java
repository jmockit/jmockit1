/*
 * Copyright (c) 2006-2014 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package org.jmock.samples.sniper;

import org.junit.*;

import mockit.*;

public final class AuctionSniper_JMockit_Test
{
   final Money increment = new Money(2);
   final Money maximumBid = new Money(20);
   final Money beatableBid = new Money(10);
   final Money unbeatableBid = maximumBid.add(new Money(1));

   @Mocked Auction auction;
   @Mocked AuctionSniperListener listener;

   AuctionSniper sniper;

   @Before
   public void init()
   {
      sniper = new AuctionSniper(auction, increment, maximumBid, listener);
   }

   @Test
   public void triesToBeatTheLatestHighestBid() throws Exception
   {
      final Money expectedBid = beatableBid.add(increment);

      sniper.bidAccepted(beatableBid);

      new Verifications() {{ auction.bid(expectedBid); }};
   }

   @Test
   public void willNotBidPriceGreaterThanMaximum() throws Exception
   {
      sniper.bidAccepted(unbeatableBid);

      new Verifications() {{ auction.bid((Money) any); times = 0; }};
   }

   @Test
   public void willLimitBidToMaximum() throws Exception
   {
      sniper.bidAccepted(maximumBid.subtract(new Money(1)));

      new Verifications() {{ auction.bid(maximumBid); }};
   }

   @Test
   public void willAnnounceItHasFinishedIfPriceGoesAboveMaximum()
   {
      sniper.bidAccepted(unbeatableBid);

      new Verifications() {{ listener.sniperFinished(sniper); }};
   }

   @Test
   public void catchesExceptionsAndReportsThemToErrorListener() throws Exception
   {
      final AuctionException exception = new AuctionException("test");
      new Expectations() {{ auction.bid((Money) any); result = exception; }};

      sniper.bidAccepted(beatableBid);

      new Verifications() {{ listener.sniperBidFailed(sniper, exception); }};
   }
}