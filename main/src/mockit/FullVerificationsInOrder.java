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
 * @see <a href="http://jmockit.org/tutorial/Mocking.html#fullVerificationInOrder" target="tutorial">Tutorial</a>
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
    * Same as {@link #FullVerificationsInOrder()}, but restricting the verification to the specified mocked types and/or
    * mocked instances.
    *
    * @param mockedTypesAndInstancesToVerify one or more of the mocked types (ie, <tt>Class</tt> objects) and/or mocked
    * instances that are in scope for the test; for a given mocked <em>instance</em>, all classes up to (but not
    * including) <tt>java.lang.Object</tt> are considered
    *
    * @see #FullVerificationsInOrder()
    */
   protected FullVerificationsInOrder(Object... mockedTypesAndInstancesToVerify)
   {
      this();
      verificationPhase.setMockedTypesToFullyVerify(mockedTypesAndInstancesToVerify);
   }
}
