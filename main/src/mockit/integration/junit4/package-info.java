/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */

/**
 * Provides integration with <em>JUnit 4.x</em> test runners, for version 4.5 or newer.
 * Contains the {@link mockit.integration.junit4.JMockit} custom test runner (which is not an actual test runner, but
 * just a way to ensure that JMockit gets properly initialized - a better way is to simply put {@code jmockit-1.x.jar}
 * <em>before</em> {@code junit-4.x.jar} in the test runtime classpath).
 * <p/>
 * This integration provides the following benefits to test code:
 * <ol>
 *    <li>
 *       Instance fields annotated with {@code @Tested}, {@code @Injectable}, {@code @Mocked}, or {@code @Capturing}
 *       are properly initialized.
 *    </li>
 *    <li>
 *       Test methods accept <em>mock parameters</em> (annotated with {@code @Injectable}, {@code @Mocked}, or
 *       {@code @Capturing}), whose values are mocked instances automatically created by JMockit and passed by the test
 *       runner when the test method is executed.
 *    </li>
 *    <li>
 *       Expected invocations specified through the Mocking API are automatically verified before the execution of a
 *       test is completed.
 *    </li>
 *    <li>
 *       Fake classes applied with the Faking API from inside a method annotated as a {@code @Test} or a {@code @Before}
 *       method are discarded right after the execution of the test method or the whole test, respectively.
 *       Similarly, fakes applied from a {@code @BeforeClass} method are discarded after all tests in a test class have
 *       executed.
 *    </li>
 * </ol>
 */
package mockit.integration.junit4;
