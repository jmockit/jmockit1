/*
 * Copyright (c) 2006-2014 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package jmockit.tutorial.domain;

import org.apache.commons.mail.*;

import org.junit.*;

import mockit.*;

import jmockit.tutorial.persistence.*;

public final class MyBusinessService_ExpectationsAPI_Test
{
   @Mocked(stubOutClassInitialization = true) final Database unused = null;
   @Mocked SimpleEmail anyEmail;

   @Test
   public void doBusinessOperationXyz() throws Exception
   {
      final EntityX data = new EntityX(5, "abc", "abc@xpta.net");
      final EntityX existingItem = new EntityX(1, "AX5", "someone@somewhere.com");

      new Expectations() {{
         Database.find(withSubstring("select"), any);
         result = existingItem; // automatically wrapped in a list of one item
      }};

      new MyBusinessService(data).doBusinessOperationXyz();

      new Verifications() {{ Database.persist(data); }};
      new Verifications() {{ anyEmail.send(); times = 1; }};
   }

   @Test(expected = EmailException.class)
   public void doBusinessOperationXyzWithInvalidEmailAddress() throws Exception
   {
      new Expectations() {{
         anyEmail.addTo((String) withNotNull()); result = new EmailException();
      }};

      EntityX data = new EntityX(5, "abc", "someone@somewhere.com");
      new MyBusinessService(data).doBusinessOperationXyz();
   }
}
