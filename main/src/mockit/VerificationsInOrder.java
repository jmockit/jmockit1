/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit;

import mockit.internal.expectations.*;

/**
 * Same as {@link Verifications}, but checking that invocations from code under test occurred in the same order as the
 * verified expectations.
 * <pre>
 *
 * // Exercise tested code.
 * codeUnderTest.doSomething();
 *
 * // Now verify that the expected invocations occurred in a given order.
 * new VerificationsInOrder() {{
 *    <strong>mock1</strong>.firstExpectedMethod(anyInt); minTimes = 1;
 *    <strong>mock2</strong>.secondExpectedMethod(1, "test"); maxTimes = 2;
 *    <strong>MockedClass</strong>.finalMethod(anyString);
 * }};
 * </pre>
 *
 * @see #VerificationsInOrder()
 * @see #unverifiedInvocations()
 * @see #verifiedInvocations(Verifications)
 * @see <a href="http://jmockit.org/tutorial/Mocking.html#verificationInOrder" target="tutorial">Tutorial</a>
 */
public abstract class VerificationsInOrder extends Verifications
{
   /**
    * Begins <em>in-order</em> verification on the mocked types/instances that were invoked while executing code under
    * test.
    */
   protected VerificationsInOrder() { super(true); }

   /**
    * Accounts for a sequence of invocations executed by code under test that are not explicitly verified in any
    * verification block.
    * Such a "sequence" of invocations can include only a single invocation, or even be empty.
    * <p/>
    * Invocations matching an expectation recorded with a minimum invocation count - if any - are <em>also</em>
    * included here, since their replay order could not be verified otherwise.
    * This doesn't apply to <em>strict</em> expectations, though, since in that case the replay order must be as
    * recorded.
    * <p/>
    * This method can be used to verify that one or more consecutive invocations occurred <em>before</em> others, and
    * conversely to verify that one or more consecutive invocations occurred <em>after</em> others.
    * The call to this method marks the position where the unverified invocations are expected to have occurred,
    * relative to the explicitly verified ones.
    * <p/>
    * The exact sequence of unverified invocations accounted for by a particular call to this method depends on the
    * <em>position</em> of the call relative to other verifications.
    * Each grouping of explicit verifications will correspond to a sequence of <em>consecutive</em> (and verified)
    * invocations from the code under test.
    * So, if this method is called more than once from the same verification block, each call will account for a
    * separate sequence of unverified invocations; each sequence will be verified to occur, as a whole, in the same
    * order as it appears relative to those groupings of verified invocations.
    * <p/>
    * Notice that when this method is not used, the invocations from code under test need <em>not</em> be consecutive,
    * but only have the same relative order as the verification calls.
    * <p/>
    * Finally, notice that you can combine an ordered verification block that verifies the position of some calls
    * relative to others with a later unordered block which verifies some or all of those other invocations.
    * The unordered block should not come before, however, since it would "consume" the verified invocations.
    *
    * @see #verifiedInvocations(Verifications)
    * @see <a href="http://jmockit.org/tutorial/Mocking.html#partiallyOrdered" target="tutorial">Tutorial</a>
    */
   protected final void unverifiedInvocations()
   {
      ((OrderedVerificationPhase) verificationPhase).fixPositionOfUnverifiedExpectations();
   }

   /**
    * Accounts for a sequence of invocations executed from code under test that have already been explicitly verified in
    * a previous verification block.
    *
    * @param alreadyVerified an unordered verification block describing a group of already verified invocations
    *
    * @throws IllegalArgumentException if the given verifications are ordered
    *
    * @see #unverifiedInvocations()
    * @see <a href="http://jmockit.org/tutorial/Mocking.html#partiallyOrdered" target="tutorial">Tutorial</a>
    */
   protected final void verifiedInvocations(Verifications alreadyVerified)
   {
      ((OrderedVerificationPhase) verificationPhase).checkOrderOfVerifiedInvocations(
         alreadyVerified.verificationPhase);
   }
}
