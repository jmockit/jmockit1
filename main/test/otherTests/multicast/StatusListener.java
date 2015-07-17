package otherTests.multicast;

public interface StatusListener
{
   void messageSent(Client toClient);
   void messageDisplayedByClient(Client client);
   void messageReadByClient(Client client);
}
