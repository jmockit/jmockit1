/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit;

/**
 * A combination of {@link FullVerifications} and {@link VerificationsInOrder}.
 * <pre>
 *
 * // Exercise tested code.
 * codeUnderTest.doSomething();
 *
 * // Now verify that expected invocations occurred in the same order, with no invocations left unverified.
 * new FullVerificationsInOrder() {{
 *    <strong>mock1</strong>.firstExpectedMethod(anyInt); times = 1;
 *    <strong>mock2</strong>.secondExpectedMethod(1, anyString);
 *    <strong>MockedClass</strong>.finalMethod(anyBoolean, null);
 * }};
 * </pre>
 *
 * @see #FullVerificationsInOrder()
 * @see #FullVerificationsInOrder(Object...)
 * @see <a href="http://jmockit.org/tutorial/Mocking.html#fullVerificationInOrder">Tutorial</a>
 */
public abstract class FullVerificationsInOrder extends Verifications
{
   /**
    * Begins <em>in-order</em> verification for <em>all</em> invocations on the mocked types/instances that can
    * potentially be invoked from code under test.
    *
    * @see #FullVerificationsInOrder(Object...)
    */
   protected FullVerificationsInOrder()
   {
      super(true);
      verificationPhase.setAllInvocationsMustBeVerified();
   }

   /**
    * Same as {@link #FullVerificationsInOrder()}, but considering that such invocations occurred in a given number of
    * iterations.
    * <p/>
    * The effect of specifying a number of iterations larger than 1 (one) is equivalent to duplicating (like in "copy &
    * paste") the whole sequence of verified expectations.
    *
    * @param numberOfIterations the positive number of iterations for the whole set of verified expectations
    *
    * @see #FullVerificationsInOrder(Object...)
    * @deprecated Will be removed in a future release; existing tests should be simplified in order to not depend on
    * iterated expectations, or eliminated altogether.
    */
   @Deprecated
   protected FullVerificationsInOrder(int numberOfIterations)
   {
      super(true);
      verificationPhase.setAllInvocationsMustBeVerified();
      verificationPhase.setNumberOfIterations(numberOfIterations);
   }

   /**
    * Same as {@link #FullVerificationsInOrder()}, but restricting the verification to the specified mocked types and/or
    * mocked instances.
    *
    * @param mockedTypesAndInstancesToVerify one or more of the mocked types (ie, {@code Class} objects) and/or mocked
    * instances that are in scope for the test; for a given mocked <em>instance</em>, all classes up to (but not
    * including) {@code java.lang.Object} are considered
    *
    * @see #FullVerificationsInOrder()
    */
   protected FullVerificationsInOrder(Object... mockedTypesAndInstancesToVerify)
   {
      this();
      verificationPhase.setMockedTypesToFullyVerify(mockedTypesAndInstancesToVerify);
   }

   /**
    * Same as {@link #FullVerificationsInOrder(int)}, but restricting the verification to the specified mocked types
    * and/or mocked instances.
    *
    * @param mockedTypesAndInstancesToVerify one or more of the mocked types (ie, {@code Class} objects) and/or mocked
    * instances that are in scope for the test; for a given mocked <em>instance</em>, all classes up to (but not
    * including) {@code java.lang.Object} are considered
    *
    * @see #FullVerificationsInOrder()
    * @see #FullVerificationsInOrder(Object...)
    * @deprecated Will be removed in a future release; existing tests should be simplified in order to not depend on
    * iterated expectations, or eliminated altogether.
    */
   @Deprecated
   protected FullVerificationsInOrder(Integer numberOfIterations, Object... mockedTypesAndInstancesToVerify)
   {
      this(numberOfIterations);
      verificationPhase.setMockedTypesToFullyVerify(mockedTypesAndInstancesToVerify);
   }
}
