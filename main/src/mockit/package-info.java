/*
 * Copyright (c) 2006-2014 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */

/**
 * Provides the classes and annotations used when writing tests with the JMockit mocking APIs.
 * <p/>
 * The {@link mockit.Expectations} class provides an API for the traditional <em>record-replay</em> model of recording
 * expected invocations which are later replayed and implicitly verified.
 * This API makes use of the {@link mockit.Mocked} annotation.
 * The {@link mockit.Verifications} class, when combined with the {@link mockit.NonStrictExpectations} class, extends
 * the record-replay model to a <em>record-replay-verify</em> model, where the record phase can be left empty, with
 * expected invocations verified explicitly <em>after</em> exercising the code under test (ie, after the replay phase).
 * <p/>
 * The {@linkplain mockit.MockUp <code>MockUp&lt;T></code>} generic class (where {@code T} is the mocked type) allows
 * the definition of fake implementations for arbitrary classes.
 * <p/>
 * For a description with examples of the <em>Expectations & Verifications</em> API, see the
 * "<a href="http://jmockit.github.io/tutorial/BehaviorBasedTesting.html">Writing behavior-based tests</a>" chapter in
 * the Tutorial.
 * For the <em>Mockups</em> API, see the "<a href="http://jmockit.github.io/tutorial/StateBasedTesting.html">Writing
 * state-based tests and fakes</a>" chapter.
 */
package mockit;
