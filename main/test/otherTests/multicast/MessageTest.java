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
   static final String testContents = "hello there";

   void exerciseCodeUnderTest(StatusListener listener, Client... to)
   {
      final Message message = new Message(to, testContents, listener);

      new TaskExecution() {
         @Override public void run() { message.dispatch(); }
      };
   }

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

   @Test
   public void sendMessageToMultipleClients(@Mocked final Socket con, @Mocked final StatusListener listener)
      throws Exception
   {
      Client[] testClients = {new Client("client1"), new Client("client2")};

      new Expectations() {{
         con.getOutputStream(); result = new ByteArrayOutputStream();
         con.getInputStream(); result = "reply1\n reply2\n";
      }};

      exerciseCodeUnderTest(listener, testClients);

      for (final Client client : testClients) {
         new VerificationsInOrder() {{
            listener.messageSent(client);
            listener.messageDisplayedByClient(client);
            listener.messageReadByClient(client);
         }};
      }
   }
}
