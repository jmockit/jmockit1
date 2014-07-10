package org.jmock.samples.sniper;

import java.util.EventListener;

public interface AuctionSniperListener extends EventListener
{
   void sniperBidFailed(AuctionSniper sniper, AuctionException failure);

   void sniperFinished(AuctionSniper sniper);
}