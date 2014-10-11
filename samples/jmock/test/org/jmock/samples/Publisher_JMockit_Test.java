/*
 * Copyright (c) 2006-2014 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package org.jmock.samples;

import org.junit.*;

import mockit.*;

public final class Publisher_JMockit_Test
{
   @Test
   public void oneSubscriberReceivesAMessage(@Mocked final Subscriber subscriber)
   {
      // set up
      Publisher publisher = new Publisher();
      publisher.add(subscriber);

      final String message = "message";

      // execute
      publisher.publish(message);

      // expectations
      new Verifications() {{
         subscriber.receive(message);
      }};
   }
}
