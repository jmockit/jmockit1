/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package otherTests.multicast;

import java.io.*;
import java.net.*;

public final class Message
{
   private static final int CLIENT_PORT = 8000;

   private final Client[] to;
   private final String contents;
   private final StatusListener listener;

   public Message(Client[] to, String contents, StatusListener listener)
   {
      this.to = to;
      this.contents = contents;
      this.listener = listener;
   }

   /**
    * Sends the message contents to all clients, notifying the status listener about the
    * corresponding events as they occur.
    * <p/>
    * Network communication with clients occurs asynchronously, without ever blocking the caller.
    * Status notifications are executed on the Swing EDT (Event Dispatching Thread), so that the
    * UI can be safely updated.
    */
   public void dispatch()
   {
      for (Client client : to) {
         MessageDispatcher dispatcher = new MessageDispatcher(client);
         new Thread(dispatcher).start();
      }
   }

   private final class MessageDispatcher implements Runnable
   {
      private final Client client;

      MessageDispatcher(Client client) { this.client = client; }

      @Override
      public void run()
      {
         try {
            communicateWithClient();
         }
         catch (IOException e) {
            throw new RuntimeException(e);
         }
      }

      private void communicateWithClient() throws IOException
      {
         Socket connection = new Socket(client.getAddress(), CLIENT_PORT);

         try {
            sendMessage(connection.getOutputStream());
            readRequiredReceipts(connection.getInputStream());
         }
         finally {
            connection.close();
         }
      }

      private void sendMessage(OutputStream output)
      {
         new PrintWriter(output, true).println(contents);
         listener.messageSent(client);
      }

      private void readRequiredReceipts(InputStream input) throws IOException
      {
         BufferedReader in = new BufferedReader(new InputStreamReader(input));

         // Wait for display receipt:
         in.readLine();
         listener.messageDisplayedByClient(client);

         // Wait for read receipt:
         in.readLine();
         listener.messageReadByClient(client);
      }
   }
}