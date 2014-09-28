/*
 * Copyright (c) 2006-2014 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package integrationTests.multicast;

import java.io.*;
import java.net.*;
import java.util.*;

import static java.util.Arrays.asList;
import org.junit.*;

import mockit.*;

import static org.junit.Assert.*;

public final class MessageTest
{
   static final String testContents = "hello there";

   @Test
   public void sendMessageToSingleClient()
   {
      final Client theClient = new Client("client1");

      new MockUp<Socket>() {
         @Mock(invocations = 1)
         void $init(String host, int port)
         {
            assertEquals(theClient.getAddress(), host);
            assertTrue(port > 0);
         }

         @Mock(invocations = 1)
         public OutputStream getOutputStream()
         {
            return new ByteArrayOutputStream();
         }

         @Mock(invocations = 1)
         public InputStream getInputStream()
         {
            return new ByteArrayInputStream("reply1\nreply2\n".getBytes());
         }

         @Mock(minInvocations = 1)
         void close() {}
      };

      StatusListener listener = new MockUp<StatusListener>() {
         int eventIndex;

         @Mock(invocations = 1)
         void messageSent(Client toClient)
         {
            assertSame(theClient, toClient);
            assertEquals(0, eventIndex++);
         }

         @Mock(invocations = 1)
         void messageDisplayedByClient(Client client)
         {
            assertSame(theClient, client);
            assertEquals(1, eventIndex++);
         }

         @Mock(invocations = 1)
         void messageReadByClient(Client client)
         {
            assertSame(theClient, client);
            assertEquals(2, eventIndex++);
         }
      }.getMockInstance();

      exerciseCodeUnderTest(listener, theClient);
   }

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
   public void sendMessageToTwoClients()
   {
      new MockUp<Socket>() {
         @Mock(invocations = 2)
         void $init(String host, int port)
         {
            assertTrue(host.startsWith("client"));
            assertTrue(port > 0);
         }

         @Mock(invocations = 2)
         public OutputStream getOutputStream()
         {
            return new ByteArrayOutputStream();
         }

         @Mock(invocations = 2)
         public InputStream getInputStream()
         {
            return new ByteArrayInputStream("reply1\nreply2\n".getBytes());
         }

         @Mock(minInvocations = 2)
         void close() {}
      };

      StatusListener listener = new MockUp<StatusListener>() {
         @Mock(invocations = 2)
         void messageSent(Client toClient)
         {
            assertNotNull(toClient);
         }

         @Mock(invocations = 2)
         void messageDisplayedByClient(Client client)
         {
            assertNotNull(client);
         }

         @Mock(invocations = 2)
         void messageReadByClient(Client client)
         {
            assertNotNull(client);
         }
      }.getMockInstance();

      exerciseCodeUnderTest(listener, new Client("client1"), new Client("client2"));
   }

   @Test
   public void sendMessageToMultipleClients(@Mocked final Socket con, @Mocked final StatusListener listener)
      throws Exception
   {
      Client[] testClients = {new Client("client1"), new Client("client2"), new Client("client3")};

      new Expectations(testClients.length) {{
         new Socket(withPrefix("client"), anyInt); times = 1;
         con.getOutputStream(); result = new ByteArrayOutputStream();
         con.getInputStream(); result = "reply1\nreply2\n";
         con.close(); times = 1;
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

   @Test
   public void sendMessageToMultipleClients_minimal(@Mocked final Socket con, @Mocked final StatusListener listener)
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
