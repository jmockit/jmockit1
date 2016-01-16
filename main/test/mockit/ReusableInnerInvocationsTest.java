/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit;

import java.io.*;

import org.junit.*;

public final class ReusableInnerInvocationsTest
{
   @Injectable PrintWriter mock;

   static class NestedVerifications extends Verifications
   {
      NestedVerifications(PrintWriter mockPW)
      {
         mockPW.append(anyChar); times = 1;
      }
   }

   @Test
   public void reusingNestedVerifications()
   {
      mock.append('c');
      mock.checkError();

      new NestedVerifications(mock) {{
         mock.checkError(); times = 1;
      }};
   }

   class InnerVerifications extends Verifications
   {
      InnerVerifications()
      {
         mock.append(anyChar); times = 1;
      }
   }

   @Test
   public void reusingInnerVerifications()
   {
      mock.print(2);
      mock.append('T');
      mock.println(true);

      new InnerVerifications() {{
         mock.print((int) anyInt); times = 1;
         mock.println((boolean) anyBoolean);
      }};
   }

   @Test
   public void reusingLocalVerifications()
   {
      class LocalVerifications extends Verifications {{
         mock.append(anyChar); times = 1;
      }}

      mock.println(true);
      mock.print(1);
      mock.append('e');

      new LocalVerifications() {{
         mock.print((int) anyInt);
         mock.println((boolean) anyBoolean);
      }};
   }

   class BaseVerifications extends Verifications {}

   class DerivedVerifications extends BaseVerifications
   {
      protected DerivedVerifications()
      {
         mock.print(anyString);
      }
   }

   @Test
   public void reusingHierarchyOfInnerVerificationClasses()
   {
      mock.print("test");

      new DerivedVerifications() {};
   }
}