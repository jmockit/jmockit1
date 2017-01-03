/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit;

import org.junit.*;
import org.junit.rules.*;

import mockit.internal.*;

import org.hamcrest.*;

@SuppressWarnings("UnusedDeclaration")
public final class VerificationsWithPartialOrderingTest
{
   @Rule public final ExpectedException thrown = ExpectedException.none();

   public static class Dependency
   {
      public void setSomething(int value) {}
      public void setSomethingElse(String value) {}
      public void editABunchMoreStuff() {}
      public void notifyBeforeSave() {}
      public void prepare() {}
      public void save() {}
   }

   @Mocked Dependency mock;

   void exerciseCodeUnderTest()
   {
      mock.prepare();
      mock.setSomething(123);
      mock.setSomethingElse("anotherValue");
      mock.editABunchMoreStuff();
      mock.notifyBeforeSave();
      mock.save();
   }

   @Test
   public void verifyFirstCallOnly()
   {
      exerciseCodeUnderTest();

      new VerificationsInOrder() {{
         mock.prepare();
         unverifiedInvocations();
      }};
   }

   @Test
   public void verifyFirstCallWhichWasRecordedWithAConstraint()
   {
      new Expectations() {{
         mock.prepare(); times = 1;
      }};

      exerciseCodeUnderTest();

      new VerificationsInOrder() {{
         mock.prepare();
         mock.setSomething(anyInt);
         unverifiedInvocations();
      }};
   }

   @Test
   public void verifyFirstCall_outOfOrder()
   {
      thrown.expect(UnexpectedInvocation.class);

      mock.setSomething(123);
      mock.prepare();

      new VerificationsInOrder() {{
         mock.prepare();
         unverifiedInvocations();
      }};
   }

   @Test
   public void verifyLastCallOnly()
   {
      exerciseCodeUnderTest();

      new VerificationsInOrder() {{
         unverifiedInvocations();
         mock.save();
      }};
   }

   @Test
   public void verifyLastTwoCalls()
   {
      exerciseCodeUnderTest();

      new VerificationsInOrder() {{
         unverifiedInvocations();
         mock.notifyBeforeSave();
         mock.save();
      }};
   }

   @Test
   public void verifyLastCallWhenOutOfOrder()
   {
      thrown.expect(UnexpectedInvocation.class);

      mock.setSomething(123);
      mock.save();
      mock.editABunchMoreStuff();

      new VerificationsInOrder() {{
         unverifiedInvocations();
         mock.save();
      }};
   }

   @Test
   public void verifyLastTwoCallsWhenOutOfOrder()
   {
      thrown.expect(MissingInvocation.class);

      mock.setSomething(123);
      mock.save();
      mock.notifyBeforeSave();

      new VerificationsInOrder() {{
         unverifiedInvocations();
         mock.notifyBeforeSave();
         mock.save();
      }};
   }

   @Test
   public void verifyFirstAndLastCallsWhenOutOfOrder()
   {
      thrown.expect(UnexpectedInvocation.class);

      mock.prepare();
      mock.setSomething(123);
      mock.setSomethingElse("anotherValue");
      mock.notifyBeforeSave();
      mock.editABunchMoreStuff();
      mock.save();

      new VerificationsInOrder() {{
         mock.prepare();
         unverifiedInvocations();
         mock.notifyBeforeSave();
         mock.save();
      }};
   }

   @Test
   public void verifyFirstCallThenOthersInAnyOrder()
   {
      exerciseCodeUnderTest();

      new VerificationsInOrder() {{
         mock.prepare();
         unverifiedInvocations();
      }};

      new Verifications() {{
         mock.setSomethingElse("anotherValue");
         mock.setSomething(123);
      }};
   }

   @Test
   public void verifySomeCallsInAnyOrderThenFirstCall_outOfOrder()
   {
      thrown.expect(UnexpectedInvocation.class);

      mock.setSomething(123);
      mock.editABunchMoreStuff();
      mock.prepare();

      new Verifications() {{
         mock.setSomething(123);
      }};

      new VerificationsInOrder() {{
         mock.prepare(); times = 1;
         unverifiedInvocations();
      }};
   }

   @Test
   public void verifySomeCallsInAnyOrderThenLastCall()
   {
      exerciseCodeUnderTest();

      new Verifications() {{
         mock.setSomethingElse("anotherValue");
         mock.setSomething(123);
      }};

      new VerificationsInOrder() {{
         unverifiedInvocations();
         mock.save(); times = 1;
      }};
   }

   @Test
   public void attemptToUseOrderedVerificationsAsAlreadyVerified()
   {
      thrown.expect(IllegalArgumentException.class);

      mock.prepare();
      mock.editABunchMoreStuff();
      mock.save();

      final Verifications verified = new VerificationsInOrder() {{
         mock.prepare();
         mock.editABunchMoreStuff();
      }};

      new VerificationsInOrder() {{
         verifiedInvocations(verified);
      }};
   }

   @Test
   public void verifyThatSomeCallsInAnyRelativeOrderOccurBeforeAllOthers()
   {
      // These can occur in any order, but before any others:
      mock.prepare();
      mock.setSomethingElse("anotherValue");
      mock.setSomethingElse(null);

      // Not verified, but must occur after all others:
      mock.setSomething(123);
      mock.save();

      final Verifications initialGroupOfInvocations = new Verifications() {{
         mock.prepare();
         mock.setSomethingElse(anyString);
      }};

      new VerificationsInOrder() {{
         verifiedInvocations(initialGroupOfInvocations);
         unverifiedInvocations();
      }};
   }

   @Test
   public void verifyThatSomeCallsInAnyRelativeOrderOccurBeforeAllOthers_outOfOrder()
   {
      thrown.expect(UnexpectedInvocation.class);

      // Not verified, but should occur *after* the verified invocations:
      mock.save();

      // Verified:
      mock.prepare();
      mock.editABunchMoreStuff();

      // Not verified:
      mock.setSomething(123);

      final Verifications initialGroupOfInvocations = new Verifications() {{
         mock.prepare();
         mock.editABunchMoreStuff();
      }};

      new VerificationsInOrder() {{
         verifiedInvocations(initialGroupOfInvocations);
         unverifiedInvocations();
      }};
   }

   @Test
   public void verifyThatSomeCallsInAnyRelativeOrderOccurAfterAllOthers()
   {
      // Not verified:
      mock.prepare();
      mock.setSomething(123);

      // Verified in any order:
      mock.setSomethingElse("anotherValue");
      mock.save();
      mock.setSomethingElse(null);

      final Verifications finalGroupOfInvocations = new Verifications() {{
         mock.setSomethingElse(anyString);
         mock.save();
      }};

      new VerificationsInOrder() {{
         unverifiedInvocations();
         verifiedInvocations(finalGroupOfInvocations);
      }};
   }

   @Test
   public void verifyThatSomeCallsInAnyRelativeOrderOccurAfterAllOthers_outOfOrder()
   {
      thrown.expect(UnexpectedInvocation.class);

      // Not verified:
      mock.prepare();

      // Verified in any order:
      mock.setSomethingElse("anotherValue");
      mock.setSomethingElse(null);
      mock.save();

      // Not verified, but should occur *before* the verified ones:
      mock.setSomething(123);

      final Verifications finalGroupOfInvocations = new Verifications() {{
         mock.setSomethingElse(anyString);
         mock.save();
      }};

      new VerificationsInOrder() {{
         unverifiedInvocations();
         verifiedInvocations(finalGroupOfInvocations);
      }};
   }

   @Test
   public void verifyThatUnorderedGroupOfCallsOccursAfterOneOtherCall_outOfOrder()
   {
      thrown.expect(UnexpectedInvocation.class);
      thrown.expectCause(new CustomTypeSafeMatcher<Throwable>("starting with \"Unexpected invocation \"") {
         @Override
         protected boolean matchesSafely(Throwable cause)
         {
            return cause.toString().startsWith("Unexpected invocation ");
         }
      });

      mock.setSomething(123);
      mock.editABunchMoreStuff();
      mock.prepare();

      final Verifications v = new Verifications() {{
         mock.editABunchMoreStuff();
         mock.setSomething(anyInt);
      }};

      new VerificationsInOrder() {{
         mock.prepare();
         verifiedInvocations(v);
      }};
   }

   @Test
   public void verifyThatAnOrderedGroupOfCallsOccursBetweenTwoOtherGroupsOfCalls(@Mocked final Runnable aCallback)
   {
      // First group, happening before everything else in any order:
      mock.prepare();
      aCallback.run();

      // Verified intermediate invocations, occurring in this order:
      mock.setSomething(123);
      mock.setSomethingElse("anotherValue");
      mock.setSomethingElse(null);
      mock.editABunchMoreStuff();

      // Second group, happening after everything else in any order:
      mock.notifyBeforeSave();
      mock.save();

      final Verifications before = new Verifications() {{
         aCallback.run();
         mock.prepare();
      }};

      final Verifications after = new Verifications() {{
         mock.save();
         mock.notifyBeforeSave();
      }};

      new VerificationsInOrder() {{
         verifiedInvocations(before);
         mock.setSomething(anyInt);
         mock.setSomethingElse(anyString); times = 2;
         mock.editABunchMoreStuff();
         verifiedInvocations(after);
      }};
   }

   @Test
   public void verifyThatAnOrderedGroupOfCallsOccursBetweenTwoOtherGroupsOfCalls_outOfOrder(
      @Mocked final Runnable aCallback)
   {
      thrown.expect(UnexpectedInvocation.class);

      // First verified group, happening before everything else in any order:
      mock.prepare();
      aCallback.run();

      // Verified intermediate invocations, occurring in this order:
      mock.setSomething(123);
      mock.setSomethingElse("anotherValue");
      mock.setSomethingElse(null);
      mock.editABunchMoreStuff();

      // Unexpected (and unverified):
      mock.setSomethingElse("");

      // Second verified group, happening after everything else in any order:
      mock.notifyBeforeSave();
      mock.save();

      final Verifications before = new Verifications() {{
         aCallback.run();
         mock.prepare();
      }};

      final Verifications after = new Verifications() {{
         mock.save();
         mock.notifyBeforeSave();
      }};

      new VerificationsInOrder() {{
         verifiedInvocations(before);
         mock.setSomething(anyInt);
         mock.setSomethingElse(anyString); times = 2;
         mock.editABunchMoreStuff();
         verifiedInvocations(after);
      }};
   }

   @Test
   public void verifyThatAnUnverifiedGroupOfCallsOccursBetweenTwoOtherGroupsOfCalls(@Mocked final Runnable aCallback)
   {
      // Initial invocation:
      mock.prepare();

      // First group, happening before everything else in any order:
      mock.setSomething(123);
      mock.setSomething(-56);

      // Intermediate invocations, occurring in any order:
      mock.setSomethingElse("anotherValue");
      mock.editABunchMoreStuff();
      mock.setSomethingElse(null);

      // Second group, happening after everything else in any order:
      aCallback.run();
      mock.save();

      final Verifications before = new Verifications() {{
         mock.setSomething(anyInt);
      }};

      final Verifications after = new Verifications() {{
         mock.save();
         aCallback.run();
      }};

      new VerificationsInOrder() {{
         mock.prepare();
         verifiedInvocations(before);
         unverifiedInvocations();
         verifiedInvocations(after);
      }};
   }

   @Test
   public void verifyThatAnUnverifiedGroupOfCallsOccursBetweenTwoOtherGroupsOfCalls_outOfOrder(
      @Mocked final Runnable aCallback)
   {
      thrown.expect(UnexpectedInvocation.class);

      // First group, happening before everything else in any order:
      mock.setSomething(123);
      mock.setSomething(-56);

      // Unverified intermediate invocations, occurring in any order:
      mock.setSomethingElse("anotherValue");

      // Second group, happening after everything else in any order:
      aCallback.run();
      mock.editABunchMoreStuff(); // out of place
      mock.save();

      final Verifications before = new Verifications() {{
         mock.setSomething(anyInt);
         times = 2;
      }};

      final Verifications after = new Verifications() {{
         aCallback.run();
         mock.save();
      }};

      new VerificationsInOrder() {{
         verifiedInvocations(before);
         unverifiedInvocations();
         verifiedInvocations(after);
      }};
   }

   @Test
   public void verifyThatOneGroupOfInvocationsOccursBeforeAnother()
   {
      mock.prepare();
      mock.save();

      final Verifications first = new Verifications() {{ mock.prepare(); }};
      final Verifications last = new Verifications() {{ mock.save(); }};

      new VerificationsInOrder() {{
         verifiedInvocations(first);
         unverifiedInvocations();
         verifiedInvocations(last);
      }};
   }

   @Test
   public void verifyThatOneGroupOfInvocationsOccursBeforeAnother_outOfOrder()
   {
      thrown.expect(UnexpectedInvocation.class);

      mock.save();
      mock.prepare();

      final Verifications first = new Verifications() {{ mock.prepare(); }};
      final Verifications last = new Verifications() {{ mock.save(); }};

      new VerificationsInOrder() {{
         verifiedInvocations(first);
         verifiedInvocations(last);
      }};
   }

   @Test
   public void verifyThatOneGroupOccursBeforeAnotherAllowingUnverifiedInvocationsInBetween_outOfOrder()
   {
      thrown.expect(UnexpectedInvocation.class);
      thrown.expectMessage("Invocation occurred unexpectedly before another ");

      mock.save();
      mock.prepare();

      final Verifications first = new Verifications() {{ mock.prepare(); }};
      final Verifications last = new Verifications() {{ mock.save(); }};

      new VerificationsInOrder() {{
         verifiedInvocations(first);
         unverifiedInvocations();
         verifiedInvocations(last);
      }};
   }

   @Test
   public void verifyFirstAndLastCalls()
   {
      exerciseCodeUnderTest();

      new VerificationsInOrder() {{
         mock.prepare();
         unverifiedInvocations();
         mock.notifyBeforeSave();
         mock.save();
      }};
   }

   @Test
   public void verifyFirstAndLastCallsWithFirstOutOfOrder()
   {
      thrown.expect(UnexpectedInvocation.class);

      mock.editABunchMoreStuff();
      mock.prepare();
      mock.save();

      new VerificationsInOrder() {{
         mock.prepare();
         unverifiedInvocations();
         mock.save();
      }};
   }

   @Test
   public void verifyFirstAndLastInvocationsWithSomeInvocationsInBetweenImplicitlyVerified()
   {
      thrown.expect(UnexpectedInvocation.class);

      new Expectations() {{
         mock.setSomething(anyInt);
      }};

      exerciseCodeUnderTest();

      new VerificationsInOrder() {{
         mock.prepare();
         // For the test to pass, a call to unverifiedInvocations() should be
         // inserted here, even if verification occurred implicitly.
         mock.setSomethingElse(anyString);
         unverifiedInvocations();
         mock.save();
      }};
   }

   @Test
   public void verifyFirstAndLastCallsWithLastOutOfOrder()
   {
      thrown.expect(UnexpectedInvocation.class);

      mock.prepare();
      mock.editABunchMoreStuff();
      mock.save();
      mock.notifyBeforeSave();

      new VerificationsInOrder() {{
         mock.prepare();
         unverifiedInvocations();
         mock.save();
      }};
   }

   @Test
   public void verifyFirstAndLastCallsWithOthersInBetweenInAnyOrderWithYetAnotherCallAnywhere()
   {
      mock.prepare(); // first call
      mock.editABunchMoreStuff();
      mock.notifyBeforeSave();
      mock.save(); // last call
      mock.setSomething(3); // could occur anywhere

      new Verifications() {{ mock.setSomething(anyInt); }};

      new VerificationsInOrder() {{
         mock.prepare();
         unverifiedInvocations();
         mock.save();
      }};
   }

   @Test
   public void verifyTwoOrderedCallsAfterVerifyingDifferentCallsToSameMethodInAnyOrder()
   {
      mock.setSomething(1);
      mock.prepare();
      mock.setSomething(2);
      mock.save();
      mock.setSomething(3);

      new Verifications() {{ mock.setSomething(anyInt); times = 3; }};

      new VerificationsInOrder() {{
         mock.prepare();
         mock.save();
      }};
   }

   @Test
   public void verifyFirstAndLastCallsWithOthersInBetweenInAnyOrder()
   {
      exerciseCodeUnderTest();

      new VerificationsInOrder() {{
         mock.prepare();
         unverifiedInvocations();
         mock.notifyBeforeSave();
         mock.save();
      }};

      new Verifications() {{
         mock.setSomething(123);
         mock.setSomethingElse("anotherValue");
      }};
   }

   @Test
   public void verifyFirstAndLastCallsWithOthersInBetweenInAnyOrderWhenOutOfOrder()
   {
      thrown.expect(UnexpectedInvocation.class);

      mock.prepare();
      mock.setSomething(123);
      mock.setSomethingElse("anotherValue");
      mock.notifyBeforeSave();
      mock.editABunchMoreStuff();
      mock.save();

      new Verifications() {{
         mock.setSomethingElse("anotherValue");
         mock.setSomething(anyInt);
      }};

      new VerificationsInOrder() {{
         mock.prepare();
         unverifiedInvocations();
         mock.notifyBeforeSave();
         mock.save();
      }};
   }

   @Test
   public void verifyConsecutiveInvocations()
   {
      exerciseCodeUnderTest();

      new VerificationsInOrder() {{
         mock.prepare();
         unverifiedInvocations();
         mock.setSomething(123);
         mock.setSomethingElse("anotherValue");
         unverifiedInvocations();
         mock.save();
      }};
   }

   @Test
   public void verifyConsecutiveInvocationsWhenNotConsecutive()
   {
      thrown.expect(UnexpectedInvocation.class);

      mock.prepare();
      mock.setSomething(123);
      mock.setSomethingElse("anotherValue");
      mock.setSomething(45);
      mock.save();

      new VerificationsInOrder() {{
         unverifiedInvocations();
         mock.setSomething(123);
         mock.setSomething(45);
         unverifiedInvocations();
      }};
   }

   @Test
   public void verifyConsecutiveInvocationsInTwoSequences()
   {
      exerciseCodeUnderTest();

      new VerificationsInOrder() {{
         mock.prepare();
         unverifiedInvocations();
         mock.setSomething(123);
         mock.setSomethingElse(anyString);
         unverifiedInvocations();
         mock.notifyBeforeSave();
         unverifiedInvocations();
      }};
   }

   @Test
   public void accountForUnverifiedInvocationsWithAllAlreadyVerifiedInAPreviousBlock()
   {
      mock.prepare();
      mock.setSomething(123);
      mock.setSomething(45);
      mock.setSomethingElse("anotherValue");
      mock.notifyBeforeSave();
      mock.save();

      new Verifications() {{
         mock.setSomething(anyInt);
         mock.setSomethingElse(anyString);
         mock.notifyBeforeSave(); maxTimes = 1;
      }};

      new VerificationsInOrder() {{
         mock.prepare();
         unverifiedInvocations(); // nothing was left unverified
         mock.save();
      }};
   }
}
