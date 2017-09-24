/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */

/**
 * Provides integration with <em>TestNG</em> test runners, for version 6.2 or newer.
 * This integration provides the following benefits to test code:
 * <ol>
 * <li>
 * Expected invocations specified through the Mocking API are automatically verified before the execution of a test is
 * completed.
 * </li>
 * <li>
 * Fake classes applied with the Faking API from inside a method annotated as a {@code @Test} or a {@code @BeforeMethod}
 * will be discarded right after the execution of the test method or the whole test, respectively.
 * </li>
 * <li>
 * Test methods accept <em>mock parameters</em>, whose values are mocked instances automatically created by JMockit and
 * passed by the test runner when the test method is executed.
 * </li>
 * </ol>
 */
package mockit.integration.testng;
