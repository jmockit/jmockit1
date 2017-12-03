/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.integration.junit4;

import java.util.*;

import org.junit.*;

import mockit.*;
import mockit.integration.*;

// These tests are expected to fail, so they are kept inactive.
@Ignore
public final class JUnit4ViolatedExpectationsTest
{
   @Test // fails with a "missing invocation" error
   public void expectInvocationWhichDoesNotOccurInTestedCodeThatThrowsAnException_1(@Mocked final Collaborator mock)
   {
      new Expectations() {{
         mock.doSomething();
      }};
   }

   @Test // fails with the exception thrown by tested code
   public void expectInvocationWhichDoesNotOccurInTestedCodeThatThrowsAnException_2(@Mocked final Collaborator mock)
   {
      new Expectations() {{ mock.doSomething(); result = new IllegalFormatCodePointException('x'); }};

      mock.doSomething();
   }

   @Test // fails with an "unexpected invocation" error
   public void expectInvocationWhichDoesNotOccurInTestedCodeThatThrowsAnException_3(@Mocked Collaborator mock)
   {
      new Expectations() {{
         new Collaborator();
         maxTimes = 1;
      }};

      new Collaborator();
      new Collaborator();
   }

   // fails with a "missing invocation" error after the exception thrown by tested code
   @Test(expected = IllegalFormatCodePointException.class)
   public void expectInvocationWhichDoesNotOccurInTestedCodeThatThrowsAnException_4(@Mocked final Collaborator mock)
   {
      new Expectations() {{
         mock.doSomething(); result = new IllegalFormatCodePointException('x');
         minTimes = 2;
      }};

      mock.doSomething();
   }

   @Test(expected = AssertionError.class) // fails with a different exception than expected
   public void expectInvocationWhichDoesNotOccurInTestedCodeThatThrowsAnException_5(@Mocked final Collaborator mock)
   {
      new Expectations() {{
         mock.doSomething(); result = new IllegalFormatCodePointException('x');
      }};

      mock.doSomething();
   }

   @Test(expected = AssertionError.class) // fails without the expected exception being thrown
   public void expectInvocationWhichDoesNotOccurInTestedCodeThatThrowsAnException_6(@Mocked final Collaborator mock)
   {
      new Expectations() {{
         mock.doSomething(); result = new IllegalFormatCodePointException('x');
      }};
   }
}
