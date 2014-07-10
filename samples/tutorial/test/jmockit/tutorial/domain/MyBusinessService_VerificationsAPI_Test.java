/*
 * Copyright (c) 2006-2013 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package jmockit.tutorial.domain;

import org.apache.commons.mail.*;

import org.junit.*;

import mockit.*;

import jmockit.tutorial.persistence.*;

public final class MyBusinessService_VerificationsAPI_Test
{
   @Tested MyBusinessService service; // instantiated automatically
   @Mocked(stubOutClassInitialization = true) Database onlyStatics;
   @Capturing Email email; // concrete subclass mocked on demand, when loaded

   final EntityX data = new EntityX(5, "abc", "someone@somewhere.com");

   @Test
   public void doBusinessOperationXyzPersistsData() throws Exception
   {
      // No expectations recorded in this case.
      
      service.doBusinessOperationXyz(data);

      new Verifications() {{ Database.persist(data); }};
   }

   @Test
   public void doBusinessOperationXyzFindsItemsAndSendsNotificationEmail() throws Exception
   {
      // Invocations that produce a result are recorded, but only those we care about.
      new NonStrictExpectations() {{
         Database.find(withSubstring("select"), any);
         result = new EntityX(1, "AX5", "someone@somewhere.com");
      }};

      service.doBusinessOperationXyz(data);

      new VerificationsInOrder() {{
         email.addTo(data.getCustomerEmail());
         email.setMsg(withNotEqual(""));
         email.send();
      }};
   }
}
