/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit;

/**
 * Same as {@link Verifications}, but checking that <em>all</em> invocations from code under test are explicitly
 * verified, except for those already verified through other means.
 * As such, a verification block created with this class represents a full set of verifications for the mocked
 * types/instances used in the test.
 * <pre>
 *
 * // Exercise tested code.
 * codeUnderTest.doSomething();
 *
 * // Now verify that expected invocations occurred in any order, with no invocations left unverified.
 * new FullVerifications() {{
 *    <strong>mock1</strong>.expectedMethod(anyInt);
 *    <strong>mock2</strong>.anotherExpectedMethod(1, "test"); times = 2;
 * }};
 * </pre>
 * Any invocation from code under test that is not covered by an explicit verification, or by an invocation count
 * constraint when recorded, will cause an assertion error to be thrown.
 *
 * @see #FullVerifications()
 * @see #FullVerifications(Object...)
 * @see #unverifiedInvocations()
 * @see FullVerificationsInOrder
 * @see <a href="http://jmockit.org/tutorial/Mocking.html#fullVerification">Tutorial</a>
 */
public abstract class FullVerifications extends Verifications
{
   /**
    * Begins <em>full</em> verification on the mocked types/instances that can potentially be invoked from code under
    * test.
    *
    * @see #FullVerifications(Object...)
    */
   protected FullVerifications() { verificationPhase.setAllInvocationsMustBeVerified(); }

   /**
    * Same as {@link #FullVerifications()}, but considering that such invocations occurred in a given number of
    * iterations.
    * <p/>
    * The effect of specifying a (positive) number of iterations is equivalent to setting to that number the lower and
    * upper invocation count limits for each verified expectation.
    * If, however, the lower/upper limit is explicitly specified for an expectation, the given number of iterations
    * becomes a multiplier.
    * When not specified, at least one matching invocation will be required to have occurred; therefore, specifying
    * <em>1 (one)</em> iteration is different from not specifying the number of iterations at all.
    *
    * @param numberOfIterations the positive number of iterations for the whole set of verified expectations
    *
    * @see #FullVerifications(Object...)
    * @see #times
    * @see #minTimes
    * @see #maxTimes
    * @deprecated Will be removed in a future release; existing tests should be simplified in order to not depend on
    * iterated expectations, or eliminated altogether.
    */
   @Deprecated
   protected FullVerifications(int numberOfIterations)
   {
      super(numberOfIterations);
      verificationPhase.setAllInvocationsMustBeVerified();
   }

   /**
    * Same as {@link #FullVerifications()}, but restricting the verification to the specified mocked types and/or
    * mocked instances.
    *
    * @param mockedTypesAndInstancesToVerify one or more of the mocked types (ie, {@code Class} objects) and/or mocked
    * instances that are in scope for the test; for a given mocked <em>instance</em>, all classes up to (but not
    * including) {@code java.lang.Object} are considered
    */
   protected FullVerifications(Object... mockedTypesAndInstancesToVerify)
   {
      this();
      verificationPhase.setMockedTypesToFullyVerify(mockedTypesAndInstancesToVerify);
   }

   /**
    * Same as {@link #FullVerifications(int)}, but restricting the verification to the specified mocked types and/or
    * mocked instances.
    *
    * @param numberOfIterations the positive number of iterations for the whole set of verified expectations
    * @param mockedTypesAndInstancesToVerify one or more of the mocked types (ie, {@code Class} objects) and/or mocked
    * instances that are in scope for the test; for a given mocked <em>instance</em>, all classes up to (but not
    * including) {@code java.lang.Object} are considered
    *
    * @see #FullVerifications()
    * @see #FullVerifications(Object...)
    * @deprecated Will be removed in a future release; existing tests should be simplified in order to not depend on
    * iterated expectations, or eliminated altogether.
    */
   @Deprecated
   protected FullVerifications(Integer numberOfIterations, Object... mockedTypesAndInstancesToVerify)
   {
      this(numberOfIterations);
      verificationPhase.setMockedTypesToFullyVerify(mockedTypesAndInstancesToVerify);
   }

   /**
    * Accounts for all invocations not yet verified, removing them from any further consideration.
    * Invocations that occur after this, however, can still be verified later in another verification block.
    * <p/>
    * If one or more mocked types/instances were specified in the
    * {@linkplain #FullVerifications(Object...) constructor call} for this verification block, then only the associated
    * invocations (if any) are removed.
    */
   protected final void unverifiedInvocations() { verificationPhase.discardReplayedInvocations(); }
}
