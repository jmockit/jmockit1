/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit;

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
 * @see <a href="http://jmockit.org/tutorial/Mocking.html#verificationInOrder" target="tutorial">Tutorial</a>
 */
public abstract class VerificationsInOrder extends Verifications
{
   /**
    * Begins <em>in-order</em> verification on the mocked types/instances that were invoked while executing code under
    * test.
    */
   protected VerificationsInOrder() { super(true); }
}
