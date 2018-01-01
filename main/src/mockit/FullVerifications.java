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
 * @see FullVerificationsInOrder
 * @see <a href="http://jmockit.org/tutorial/Mocking.html#fullVerification" target="tutorial">Tutorial</a>
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
    * Same as {@link #FullVerifications()}, but restricting the verification to the specified mocked types and/or
    * mocked instances.
    *
    * @param mockedTypesAndInstancesToVerify one or more of the mocked types (ie, <tt>Class</tt> objects) and/or mocked
    * instances that are in scope for the test; for a given mocked <em>instance</em>, all classes up to (but not
    * including) <tt>java.lang.Object</tt> are considered
    */
   protected FullVerifications(Object... mockedTypesAndInstancesToVerify)
   {
      this();
      verificationPhase.setMockedTypesToFullyVerify(mockedTypesAndInstancesToVerify);
   }
}
