/*
 * Copyright (c) 2006-2015 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit;

import java.util.*;
import javax.annotation.*;

import mockit.internal.expectations.*;
import mockit.internal.expectations.argumentMatching.*;
import mockit.internal.state.*;

/**
 * Used to <em>verify</em> a set of expectations on available {@linkplain mockit.Mocked mocked} types and/or instances,
 * against the invocations which actually occurred during the test.
 * As such, these verifications can only appear <em>after</em> having exercised the code under test.
 * <p/>
 * An expectation verification attempts to match a number of method or constructor invocations, that we expect have
 * occurred during the execution of code under test.
 * By default, <em>at least one</em> matching invocation must be found for the verification to be successful; if no
 * matching invocations are found, an assertion error is thrown.
 * <p/>
 * Expectations are verified simply by invoking the desired method or constructor on a mocked type/instance, during
 * the initialization of a {@code Verifications} object.
 * Typically, this is done by instantiating an anonymous subclass containing an instance initialization body, or as we
 * call it, a <em>verification block</em>:
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
 * The relative order between the invocations that match two or more verifications is not taken into consideration; when
 * that is desired, the {@link mockit.VerificationsInOrder} class should be used instead.
 * <p/>
 * Naturally, not all invocations that occurred during the execution of code under test need to be explicitly verified
 * in a verification block.
 * If desired, however, we can make sure that <em>all</em> such invocations are verified, by using the
 * {@link mockit.FullVerifications} class instead.
 *
 * @see #Verifications()
 * @see #Verifications(int)
 * @see #withCapture()
 * @see mockit.Expectations
 * @see <a href="http://jmockit.org/tutorial/BehaviorBasedTesting.html#verification">Tutorial</a>
 */
public abstract class Verifications extends Invocations
{
   @Nonnull final BaseVerificationPhase verificationPhase;

   /**
    * Begins a set of unordered expectation verifications, on the available mocked types and/or mocked instances.
    * Such verifications are meant to be executed <em>after</em> the call to code under test has been made.
    *
    * @see #Verifications(int)
    */
   protected Verifications() { this(false); }

   /**
    * Same as {@link #Verifications()}, but considering that such invocations occurred in a given number of iterations.
    * <p/>
    * The effect of specifying a (positive) number of iterations is equivalent to setting to that number the lower and
    * upper invocation count limits for each expectation verified.
    * If, however, the lower/upper limit is explicitly specified for an expectation, the given number of iterations
    * becomes a multiplier.
    * When not specified, at least one matching invocation will be required to have occurred; therefore, specifying
    * <em>1 (one)</em> iteration is different from not specifying the number of iterations at all.
    *
    * @param numberOfIterations the positive number of iterations for the whole set of verified expectations
    *
    * @see #times
    * @see #minTimes
    * @see #maxTimes
    * @see <a href="http://jmockit.org/tutorial/BehaviorBasedTesting.html#iterations">Tutorial</a>
    */
   protected Verifications(int numberOfIterations)
   {
      this(false);
      verificationPhase.setNumberOfIterations(numberOfIterations);
   }

   Verifications(boolean inOrder)
   {
      RecordAndReplayExecution instance = TestRun.getRecordAndReplayForVerifications();
      verificationPhase = instance.startVerifications(inOrder);
   }

   @Nonnull @Override
   final BaseVerificationPhase getCurrentPhase() { return verificationPhase; }

   /**
    * Captures the argument value passed into the associated expectation parameter, for a matching invocation that
    * occurred when the tested code was exercised.
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
    *
    * @return the captured argument value
    *
    * @see #withCapture(java.util.List)
    * @see #withCapture(Object)
    * @see <a href="http://jmockit.org/tutorial/BehaviorBasedTesting.html#withCapture">Tutorial</a>
    */
   protected final <T> T withCapture()
   {
      verificationPhase.addArgMatcher(AlwaysTrueMatcher.ANY_VALUE);
      //noinspection ConstantConditions
      return null;
   }

   /**
    * Captures new instances of type {@code T} that were created by the code under test.
    * Said instances are only those which were created through constructor invocations matching the constructor
    * verification that was passed as argument in a call to this method.
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
    *
    * @param constructorVerification a new instance of the desired mocked class, created through a regular constructor
    *                                verification
    *
    * @return a list with the (zero, one, or more) captured new instances that match the verified constructor invocation
    *
    * @see #withCapture()
    * @see #withCapture(java.util.List)
    * @see <a href="http://jmockit.org/tutorial/BehaviorBasedTesting.html#withCapture">Tutorial</a>
    */
   protected final <T> List<T> withCapture(@SuppressWarnings("unused") T constructorVerification)
   {
      return verificationPhase.getNewInstancesMatchingVerifiedConstructorInvocation();
   }
}
