package org.jmock.samples.sniper;

public final class AuctionSniper
{
   private final Auction lotToBidFor;
   private final Money bidIncrement;
   private final Money maximumBid;
   private final AuctionSniperListener listener;

   public AuctionSniper(
      Auction lotToBidFor, Money bidIncrement, Money maximumBid, AuctionSniperListener listener)
   {
      this.lotToBidFor = lotToBidFor;
      this.bidIncrement = bidIncrement;
      this.maximumBid = maximumBid;
      this.listener = listener;
   }

   public void bidAccepted(Money amount)
   {
      if (amount.compareTo(maximumBid) <= 0) {
         placeBid(amount);
      }
      else {
         listener.sniperFinished(this);
      }
   }

   private void placeBid(Money amount)
   {
      Money bidAmount = Money.min(maximumBid, amount.add(bidIncrement));

      try {
         lotToBidFor.bid(bidAmount);
      }
      catch (AuctionException e) {
         listener.sniperBidFailed(this, e);
      }
   }
}