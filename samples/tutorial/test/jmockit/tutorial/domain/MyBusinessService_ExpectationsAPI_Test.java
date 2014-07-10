/*
 * Copyright (c) 2006-2013 Rogério Liesenfeld
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
   @Mocked SimpleEmail email;

   @Test
   public void doBusinessOperationXyz() throws Exception
   {
      final EntityX data = new EntityX(5, "abc", "abc@xpta.net");

      // Recorded strictly, so matching invocations must be replayed in the same order:
      new Expectations() {{
         Database.find(withSubstring("select"), any);
         result = new EntityX(1, "AX5", "someone@somewhere.com");

         Database.persist(data);
      }};

      // Recorded non-strictly, so matching invocations can be replayed in any order:
      new NonStrictExpectations() {{
         email.send(); times = 1; // a non-strict expectation requires a constraint if expected
      }};

      new MyBusinessService().doBusinessOperationXyz(data);
   }

   @Test(expected = EmailException.class)
   public void doBusinessOperationXyzWithInvalidEmailAddress() throws Exception
   {
      new NonStrictExpectations() {{
         email.addTo((String) withNotNull()); result = new EmailException();

         // If the e-mail address is invalid, sending the message should not be attempted:
         email.send(); times = 0;
      }};

      EntityX data = new EntityX(5, "abc", "someone@somewhere.com");
      new MyBusinessService().doBusinessOperationXyz(data);
   }
}
