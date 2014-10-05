/*
 * Copyright (c) 2006-2014 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package jmockit.tutorial.domain;

import java.util.*;

import org.apache.commons.mail.*;

import org.junit.*;
import static org.junit.Assert.*;

import mockit.*;

import jmockit.tutorial.persistence.*;

public final class MyBusinessService_MockupsAPI_Test
{
   public static final class MockDatabase extends MockUp<Database>
   {
      @Mock
      public void $clinit() { /* do nothing */ }

      @Mock(invocations = 1)
      public List<EntityX> find(String ql, Object... args)
      {
         assertNotNull(ql);
         assertTrue(args.length > 0);
         return Arrays.asList(new EntityX(1, "AX5", "someone@somewhere.com"));
      }

      @Mock(maxInvocations = 1)
      public void persist(Object o) { assertNotNull(o); }
   }

   @BeforeClass
   public static void mockUpPersistenceFacade()
   {
      // Applies the mock class by invoking its constructor:
      new MockDatabase();
   }

   final EntityX data = new EntityX(5, "abc", "5453-1");

   @Test
   public void doBusinessOperationXyz() throws Exception
   {
      // Defines and applies a mock class in one operation:
      new MockUp<Email>() {
         @Mock(invocations = 1)
         Email addTo(Invocation inv, String email)
         {
            assertEquals(data.getCustomerEmail(), email);
            return inv.getInvokedInstance();
         }

         @Mock(invocations = 1)
         String send() { return ""; }
      };

      new MyBusinessService(data).doBusinessOperationXyz();
   }

   @Test(expected = EmailException.class)
   public void doBusinessOperationXyzWithInvalidEmailAddress() throws Exception
   {
      new MockUp<Email>() {
         @Mock
         Email addTo(String email) throws EmailException
         {
            assertNotNull(email);
            throw new EmailException();
         }
      };

      new MyBusinessService(data).doBusinessOperationXyz();
   }
}
