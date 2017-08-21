/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package otherTests.multicast;

import java.io.*;
import java.net.*;
import java.util.*;

import static java.util.Arrays.asList;
import org.junit.*;

import mockit.*;

public final class MessageTest
{
   // A general-purpose utility class that waits for background task completion.
   abstract static class TaskExecution implements Runnable
   {
      {
         List<Thread> tasksBefore = getActiveTasks();

         try {
            //noinspection OverriddenMethodCallDuringObjectConstruction
            run();
         }
         finally {
            waitForCompletionOfNewTasks(tasksBefore);
         }
      }

      private List<Thread> getActiveTasks()
      {
         Thread[] tasks = new Thread[2 * Thread.activeCount()];
         Thread.enumerate(tasks);
         return new ArrayList<Thread>(asList(tasks));
      }

      private void waitForCompletionOfNewTasks(List<Thread> tasksBefore)
      {
         List<Thread> tasksAfter = getActiveTasks();
         tasksAfter.removeAll(tasksBefore);

         for (Thread task : tasksAfter) {
            try { task.join(); } catch (InterruptedException ignore) {}
         }
      }
   }

   @Injectable final String testContents = "hello there";
   @Injectable final Client[] testClients = {new Client("client1"), new Client("client2")};
   @Injectable StatusListener listener;
   @Tested Message message;// = new Message(testClients, testContents, listener);

   @Test
   public void sendMessageToMultipleClients(@Mocked final Socket con) throws Exception
   {
      new Expectations() {{
         con.getOutputStream(); result = new ByteArrayOutputStream();
         con.getInputStream(); result = "reply1\n reply2\n";
      }};

      // Code under test:
      new TaskExecution() { @Override public void run() { message.dispatch(); } };

      // Verification that each client received the expected invocations:
      for (final Client client : testClients) {
         new VerificationsInOrder() {{
            listener.messageSent(client);
            listener.messageDisplayedByClient(client);
            listener.messageReadByClient(client);
         }};
      }
   }
}
