package org.jmock.samples;

import org.jmock.*;
import org.jmock.auto.*;
import org.jmock.integration.junit4.*;
import org.junit.*;

public class PublisherTest
{
   @Rule public final JUnitRuleMockery context = new JUnitRuleMockery();

   @Mock Subscriber subscriber;

   @Test
   public void oneSubscriberReceivesAMessage()
   {
      // set up
      Publisher publisher = new Publisher();
      publisher.add(subscriber);

      final String message = "message";

      // expectations
      context.checking(new Expectations() {{
         oneOf(subscriber).receive(message);
      }});

      // execute
      publisher.publish(message);
   }
}
