package org.jmock.samples.sniper;

import org.junit.*;

import org.jmock.*;
import org.jmock.integration.junit4.*;

/**
 * This and other classes in this package (except, naturally, AuctionSniper_JMockit_Test) are based
 * on the original source code available
 * <a href="http://svn.jmock.codehaus.org/browse/jmock/trunk/jmock2/example/org/jmock/example/sniper">here</a>.
 * Small modifications were made to simplify the code, without compromising the original design.
 */
public final class AuctionSniperTest
{
   @Rule public final JUnitRuleMockery context = new JUnitRuleMockery();

   final Money increment = new Money(2);
   final Money maximumBid = new Money(20);
   final Money beatableBid = new Money(10);
   final Money unbeatableBid = maximumBid.add(new Money(1));

   final Auction auction = context.mock(Auction.class);
   final AuctionSniperListener listener = context.mock(AuctionSniperListener.class, "listener");

   final AuctionSniper sniper = new AuctionSniper(auction, increment, maximumBid, listener);

   @Test
   public void triesToBeatTheLatestHighestBid() throws Exception
   {
      final Money expectedBid = beatableBid.add(increment);

      context.checking(new Expectations() {{
         oneOf(auction).bid(expectedBid);
      }});

      sniper.bidAccepted(beatableBid);
   }

   @Test
   public void willNotBidPriceGreaterThanMaximum() throws Exception
   {
      context.checking(new Expectations() {{
         ignoring(listener);
         never(auction).bid(with(any(Money.class)));
      }});

      sniper.bidAccepted(unbeatableBid);
   }

   @Test
   public void willLimitBidToMaximum() throws Exception
   {
      context.checking(new Expectations() {{
         exactly(1).of(auction).bid(maximumBid);
      }});

      sniper.bidAccepted(maximumBid.subtract(new Money(1)));
   }

   @Test
   public void willAnnounceItHasFinishedIfPriceGoesAboveMaximum()
   {
      context.checking(new Expectations() {{
         exactly(1).of(listener).sniperFinished(sniper);
      }});

      sniper.bidAccepted(unbeatableBid);
   }

   @Test
   public void catchesExceptionsAndReportsThemToErrorListener() throws Exception
   {
      final AuctionException exception = new AuctionException("test");

      context.checking(new Expectations() {{
         allowing(auction).bid(with(any(Money.class))); will(throwException(exception));
         exactly(1).of(listener).sniperBidFailed(sniper, exception);
      }});

      sniper.bidAccepted(beatableBid);
   }
}