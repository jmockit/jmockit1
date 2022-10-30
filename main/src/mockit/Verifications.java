/*
 * Copyright (c) 2006 JMockit developers
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit;

import java.util.*;
import javax.annotation.*;

import mockit.internal.expectations.*;
import mockit.internal.expectations.argumentMatching.*;
import mockit.internal.state.*;

/**
 * Used to <em>verify</em> a set of expectations on available {@linkplain Mocked mocked} types and/or instances, against the invocations which
 * actually occurred during the test.
 * As such, these verifications can only appear <em>after</em> having exercised the code under test.
 * <p>
 * An expectation verification attempts to match a number of method or constructor invocations, that we expect have occurred during the execution of
 * code under test.
 * By default, <em>at least one</em> matching invocation must be found for the verification to be successful; if no matching invocations are found,
 * an assertion error is thrown.
 * <p>
 * Expectations are verified simply by invoking the desired method or constructor on a mocked type/instance, during the initialization of a
 * <code>Verifications</code> object.
 * This is done by instantiating an anonymous subclass containing an instance initialization body, or as we call it, a <em>verification block</em>:
 * <pre>
 * // Exercise tested code.
 * codeUnderTest.doSomething();
 *
 * // Now verify that the expected invocations actually occurred (in any order).
 * new Verifications() {{
 *    <strong>mock1</strong>.expectedMethod(anyInt);
 *    <strong>mock2</strong>.anotherExpectedMethod(1, "test"); times = 2;
 * }};
 * </pre>
 * The relative order between the invocations that match two or more verifications is not taken into consideration; when that is desired, the
 * {@link VerificationsInOrder} class should be used instead.
 * <p>
 * Not all invocations that occurred during the execution of code under test need to be explicitly verified in a verification block.
 * If that is desired, we can make sure that <em>all</em> such invocations are verified, by using the {@link FullVerifications} class.
 *
 * @see #Verifications()
 * @see #withCapture()
 * @see Expectations
 * @see <a href="http://jmockit.github.io/tutorial/Mocking.html#verification" target="tutorial">Tutorial</a>
 */
public class Verifications extends Invocations
{
   /**
    * Begins a set of unordered expectation verifications, on the available mocked types and/or mocked instances.
    * Such verifications are meant to be executed <em>after</em> the call to code under test has been made.
    */
   protected Verifications() { this(false, (Object[]) null); }

   Verifications(boolean inOrder, @Nullable Object... mockedTypesAndInstancesToVerify) {
      RecordAndReplayExecution instance = TestRun.getRecordAndReplayForVerifications();
      currentPhase = instance.startVerifications(inOrder, mockedTypesAndInstancesToVerify);
   }

   /**
    * Captures the argument value passed into the associated expectation parameter, for a matching invocation that occurred when the tested
    * code was exercised.
    * This method should be used in a local variable assignment expression inside a verification block.
    * For example:
    * <pre>
    * codeUnderTest.doSomething();
    *
    * new Verifications() {{
    *    String <strong>name</strong>;
    *    int <strong>age</strong>;
    *    personDAOMock.create(<strong>age</strong> = <em>withCapture()</em>, <strong>name</strong> = <em>withCapture()</em>);
    *    assertFalse(<strong>name</strong>.isEmpty());
    *    assertTrue(<strong>age</strong> >= 18);
    * }};
    * </pre>
    * If there is more than one matching invocation, then only the last one to have occurred is considered.
    * Apart from capturing received argument values, this method has the same effect as the {@link #any} argument matcher.
    * <p>
    * When an argument matcher is used for a regular (ie, non-varargs) parameter in a call to a mocked method/constructor, it's <em>not</em>
    * necessary to also use matchers for the other parameters.
    * So, it's valid to mix the use of matchers with given values.
    * Arguments given as literals, local variables, or fields, will be implicitly matched as if {@link #withEqual(Object)} had been used.
    * In the special case of a varargs method, however, this flexibility is not available: if a matcher is used for any regular parameter,
    * or for any element in the varargs array, then a matcher <em>must</em> be used for every other parameter and varargs element.
    *
    * @return the captured argument value
    *
    * @see #withCapture(List)
    * @see #withCapture(Object)
    * @see <a href="http://jmockit.github.io/tutorial/Mocking.html#withCapture" target="tutorial">Tutorial</a>
    */
   @Nullable
   protected final <T> T withCapture() {
      currentPhase.addArgMatcher(AlwaysTrueMatcher.ANY_VALUE);
      return null;
   }

   /**
    * Captures new instances of type <code>T</code> that were created by the code under test.
    * Said instances are only those which were created through constructor invocations matching the constructor verification that was passed
    * as argument in a call to this method.
    * For example:
    * <pre>
    * codeUnderTest.doSomething();
    *
    * new Verifications() {{
    *    <strong>List&lt;Person> newPersons = withCapture(new Person());</strong>
    *    Person newPerson = newPersons.get(0);
    *
    *    Person personCreated;
    *    personDAOMock.create(personCreated = withCapture());
    *
    *    assertSame(newPerson, personCreated);
    * }};
    * </pre>
    * Note this is not meant be used as an argument matcher.
    *
    * @param constructorVerification a new instance of the desired mocked class, created through a regular constructor verification
    *
    * @return a list with the (zero, one, or more) captured new instances that match the verified constructor invocation
    *
    * @see #withCapture()
    * @see #withCapture(List)
    * @see <a href="http://jmockit.github.io/tutorial/Mocking.html#withCapture" target="tutorial">Tutorial</a>
    */
   @Nonnull
   protected final <T> List<T> withCapture(@Nonnull T constructorVerification) {
      return ((BaseVerificationPhase) currentPhase).getNewInstancesMatchingVerifiedConstructorInvocation();
   }
}