/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package tutorial.domain;

import org.junit.*;
import org.junit.rules.*;
import static org.junit.Assert.*;

import mockit.*;

import org.apache.commons.mail.*;
import static tutorial.persistence.Database.*;

public final class MyBusinessServiceTest
{
   @Rule public final ExpectedException thrown = ExpectedException.none();

   @Tested final EntityX data = new EntityX(1, "abc", "someone@somewhere.com");
   @Tested(fullyInitialized = true) MyBusinessService businessService;
   @Mocked SimpleEmail anyEmail;

   @Test
   public void doBusinessOperationXyz() throws Exception
   {
      EntityX existingItem = new EntityX(1, "AX5", "abc@xpta.net");
      persist(existingItem);

      businessService.doBusinessOperationXyz();

      assertNotEquals(0, data.getId()); // implies "data" was persisted
      new Verifications() {{ anyEmail.send(); times = 1; }};
   }

   @Test
   public void doBusinessOperationXyzWithInvalidEmailAddress() throws Exception
   {
      final String email = "invalid address";
      data.setCustomerEmail(email);
      new Expectations() {{ anyEmail.addTo(email); result = new EmailException(); }};
      thrown.expect(EmailException.class);

      businessService.doBusinessOperationXyz();
   }
}
