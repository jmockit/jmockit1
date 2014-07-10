/*
 * Copyright (c) 2006-2011 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package integrationTests.multicast;

public interface StatusListener
{
   void messageSent(Client toClient);
   void messageDisplayedByClient(Client client);
   void messageReadByClient(Client client);
}
