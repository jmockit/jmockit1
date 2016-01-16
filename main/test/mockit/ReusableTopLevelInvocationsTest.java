/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit;

import java.io.*;

import org.junit.*;
import static org.junit.Assert.assertTrue;

import mockit.internal.*;

final class FinalReusableExpectations extends Expectations
{
   FinalReusableExpectations(PrintWriter mock)
   {
      super(mock);
      mock.flush();
      minTimes = 2;
   }
}

class NonFinalReusableExpectations extends Expectations
{
   NonFinalReusableExpectations(PrintWriter mock)
   {
      super(mock);
      mock.flush();
      times = 2;
   }
}

public final class ReusableTopLevelInvocationsTest
{
   @Test
   public void useTopLevelAndFinalExpectationsSubclass() throws Exception
   {
      PrintWriter pw = new PrintWriter(System.out);

      new FinalReusableExpectations(pw);

      pw.flush();
      pw.flush();
   }

   @Test
   public void useTopLevelAndNonFinalExpectationsSubclass() throws Exception
   {
      final PrintWriter pw = new PrintWriter(System.out);

      new NonFinalReusableExpectations(pw) {{
         pw.checkError(); result = true;
      }};

      pw.flush();
      pw.flush();
      assertTrue(pw.checkError());
   }

   @Test(expected = MissingInvocation.class)
   public void useTopLevelAndFinalVerificationsSubclass(@Injectable PrintWriter mock) throws Exception
   {
      mock.flush();

      new FinalReusableVerifications(mock);
   }

   @Test
   public void useTopLevelAndFinalSubclassOfVerificationsSubclass(@Injectable PrintWriter mock) throws Exception
   {
      mock.flush();
      mock.flush();

      new FinalSubclassOfReusableVerifications(mock);
   }

   @Test(expected = MissingInvocation.class)
   public void useTopLevelAndNonFinalVerificationsSubclassWithNoAdditionalVerifications(@Injectable PrintWriter mock)
      throws Exception
   {
      mock.flush();

      new NonFinalReusableVerifications(mock) {};
   }

   @Test
   public void useTopLevelAndNonFinalVerificationsSubclassWithAdditionalVerifications(
      @Injectable final PrintWriter mock)
      throws Exception
   {
      mock.flush();
      mock.append('G');
      mock.flush();

      new NonFinalReusableVerifications(mock) {{
         mock.append(anyChar);
         times = 1;
      }};
   }
}

final class FinalReusableVerifications extends Verifications
{
   FinalReusableVerifications(PrintWriter mock)
   {
      mock.flush();
      times = 2;
   }
}

final class FinalSubclassOfReusableVerifications extends NonFinalReusableVerifications
{
   FinalSubclassOfReusableVerifications(PrintWriter mock) { super(mock); }
}

class NonFinalReusableVerifications extends Verifications
{
   NonFinalReusableVerifications(PrintWriter mock)
   {
      mock.flush();
      minTimes = 2;
      maxTimes = 2;
   }
}