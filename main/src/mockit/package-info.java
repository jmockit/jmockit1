/*
 * Copyright (c) 2006-2015 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */

/**
 * Provides the classes and annotations used when writing tests with the JMockit mocking APIs.
 * <p/>
 * The {@link mockit.Expectations} class provides an API for the traditional <em>record-replay</em> model of recording
 * expected invocations which are later replayed and implicitly verified.
 * This API makes use of the {@linkplain mockit.Mocked @Mocked} annotation.
 * The {@link mockit.Verifications} class extends the record-replay model to a <em>record-replay-verify</em> model,
 * where expectations that were not recorded can be verified explicitly <em>after</em> exercising the code under test
 * (ie, after the replay phase).
 * <p/>
 * The {@linkplain mockit.MockUp <code>MockUp&lt;T></code>} generic class (where {@code T} is the mocked type) allows
 * the definition of fake implementations for arbitrary classes or interfaces.
 * <p/>
 * For a description with examples of the <em>Expectations</em> API, see the
 * "<a href="http://jmockit.org/tutorial/BehaviorBasedTesting.html">Behavior-based testing</a>" chapter in the Tutorial.
 * For the <em>Mockups</em> API, see the "<a href="http://jmockit.org/tutorial/StateBasedTesting.html">Fakes and
 * state-based testing</a>" chapter.
 */
package mockit;
