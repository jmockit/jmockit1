/*
 * Copyright (c) 2006 JMockit developers
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */

/**
 * Provides integration with <em>JUnit 4.x</em> test runners, for version 4.5 or newer.
 * This integration provides the following benefits to test code:
 * <ol>
 *    <li>
 *       Instance fields annotated with <tt>@Tested</tt>, <tt>@Injectable</tt>, <tt>@Mocked</tt>, or <tt>@Capturing</tt> are properly
 *       initialized.
 *    </li>
 *    <li>
 *       Test methods accept <em>mock parameters</em> (annotated with <tt>@Injectable</tt>, <tt>@Mocked</tt>, or <tt>@Capturing</tt>),
 *       whose values are mocked instances automatically created by JMockit and passed by the test runner when the test method is executed.
 *    </li>
 *    <li>
 *       Expected invocations specified through the Mocking API are automatically verified before the execution of a test is completed.
 *    </li>
 *    <li>
 *       Fake classes applied with the Faking API from inside a method annotated as a <tt>@Test</tt> or a <tt>@Before</tt> method are
 *       discarded right after the execution of the test method or the whole test, respectively.
 *       Similarly, fakes applied from a <tt>@BeforeClass</tt> method are discarded after all tests in a test class have executed.
 *    </li>
 * </ol>
 */
package mockit.integration.junit4;
