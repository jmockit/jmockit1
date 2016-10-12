/**
 * Provides integration with <em>Spock</em> test runners.
 * Contains the {@link mockit.integration.spock.JMockitExtension} global extension.
 * <p/>
 * This integration provides the following benefits to test code:
 * <ol>
 * <li>
 * Expected invocations specified through the Expectations or Mockups API are automatically verified before the
 * execution of a test is completed.
 * </li>
 * <li>
 * Mock-up classes applied with the Mockups API from inside a feature method or a setup method are discarded right
 * after the execution of the test method or the whole test iteration, respectively.
 * </li>
 * <li>
 * Feature methods accept <em>mock parameters</em>, whose values are mocked instances automatically created by
 * JMockit and passed by the test runner when the feature method is executed.
 * </li>
 * </ol>
 */
package mockit.integration.spock;
