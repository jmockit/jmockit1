package org.jmock.samples;

import java.util.*;

public class Publisher
{
   private final List<Subscriber> subscribers = new ArrayList<Subscriber>();

   public void add(Subscriber subscriber)
   {
      subscribers.add(subscriber);
   }

   public void publish(String message)
   {
      for (Subscriber subscriber : subscribers) {
         subscriber.receive(message);
      }
   }
}
