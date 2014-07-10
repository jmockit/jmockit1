/*
 * Copyright (c) 2006-2012 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.integration.junit4;

import java.util.*;

import org.junit.*;

import mockit.integration.*;

// These tests are expected to fail, so they are kept inactive to avoid busting the full test run.
@Ignore
public final class JUnit4ViolatedExpectationsTest
{
   // Tests that fail with a "missing invocation" error ///////////////////////////////////////////////////////////////

   @Test
   public void expectInvocationWhichDoesNotOccurInTestedCodeThatThrowsAnException_mockUp1()
   {
      new CollaboratorMockUp();
   }

   @Test
   public void expectInvocationWhichDoesNotOccurInTestedCodeThatThrowsAnException_strict1(Collaborator mock)
   {
      new CollaboratorStrictExpectations(mock);
   }

   @Test
   public void expectInvocationWhichDoesNotOccurInTestedCodeThatThrowsAnException_nonStrict1(Collaborator mock)
   {
      new CollaboratorNonStrictExpectations(mock);
   }

   // Tests that fail with the exception thrown by tested code ////////////////////////////////////////////////////////

   @Test
   public void expectInvocationWhichDoesNotOccurInTestedCodeThatThrowsAnException_mockUp2()
   {
      new CollaboratorMockUp();

      new Collaborator().doSomething();
   }

   @Test
   public void expectInvocationWhichDoesNotOccurInTestedCodeThatThrowsAnException_strict2(Collaborator mock)
   {
      new CollaboratorStrictExpectations(mock);

      mock.doSomething();
   }

   @Test
   public void expectInvocationWhichDoesNotOccurInTestedCodeThatThrowsAnException_nonStrict2(Collaborator mock)
   {
      new CollaboratorNonStrictExpectations(mock);

      mock.doSomething();
   }

   // Tests that fail with an "unexpected invocation" error ///////////////////////////////////////////////////////////

   @Test
   public void expectInvocationWhichDoesNotOccurInTestedCodeThatThrowsAnException_mockUp3()
   {
      new CollaboratorMockUp();

      new Collaborator();
      new Collaborator();
   }

   @Test
   public void expectInvocationWhichDoesNotOccurInTestedCodeThatThrowsAnException_strict3(Collaborator mock)
   {
      new CollaboratorStrictExpectations(mock);

      new Collaborator();
   }

   @Test
   public void expectInvocationWhichDoesNotOccurInTestedCodeThatThrowsAnException_nonStrict3(Collaborator mock)
   {
      new CollaboratorNonStrictExpectations(mock);

      new Collaborator();
      new Collaborator();
   }

   // Tests that fail with a "missing invocation" error after the exception thrown by tested code /////////////////////

   @Test(expected = IllegalFormatCodePointException.class)
   public void expectInvocationWhichDoesNotOccurInTestedCodeThatThrowsAnException_mockUp4()
   {
      new CollaboratorMockUp();

      new Collaborator().doSomething();
   }

   @Test(expected = IllegalFormatCodePointException.class)
   public void expectInvocationWhichDoesNotOccurInTestedCodeThatThrowsAnException_strict4(Collaborator mock)
   {
      new CollaboratorStrictExpectations(mock);

      mock.doSomething();
   }

   @Test(expected = IllegalFormatCodePointException.class)
   public void expectInvocationWhichDoesNotOccurInTestedCodeThatThrowsAnException_nonStrict4(Collaborator mock)
   {
      new CollaboratorNonStrictExpectations(mock);

      mock.doSomething();
   }

   // Tests that fail with a different exception than expected ////////////////////////////////////////////////////////

   @Test(expected = AssertionError.class)
   public void expectInvocationWhichDoesNotOccurInTestedCodeThatThrowsAnException_mockUp5()
   {
      new CollaboratorMockUp();

      new Collaborator().doSomething();
   }

   @Test(expected = AssertionError.class)
   public void expectInvocationWhichDoesNotOccurInTestedCodeThatThrowsAnException_strict5(Collaborator mock)
   {
      new CollaboratorStrictExpectations(mock);

      mock.doSomething();
   }

   @Test(expected = AssertionError.class)
   public void expectInvocationWhichDoesNotOccurInTestedCodeThatThrowsAnException_nonStrict5(Collaborator mock)
   {
      new CollaboratorNonStrictExpectations(mock);

      mock.doSomething();
   }

   // Tests that fail without the expected exception being thrown /////////////////////////////////////////////////////

   @Test(expected = AssertionError.class)
   public void expectInvocationWhichDoesNotOccurInTestedCodeThatThrowsAnException_mockUp6()
   {
      new CollaboratorMockUp();
   }

   @Test(expected = AssertionError.class)
   public void expectInvocationWhichDoesNotOccurInTestedCodeThatThrowsAnException_strict6(Collaborator mock)
   {
      new CollaboratorStrictExpectations(mock);
   }

   @Test(expected = AssertionError.class)
   public void expectInvocationWhichDoesNotOccurInTestedCodeThatThrowsAnException_nonStrict6(Collaborator mock)
   {
      new CollaboratorNonStrictExpectations(mock);
   }
}
