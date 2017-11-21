/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */

/**
 * Provides the classes and annotations used when writing tests with the JMockit APIs.
 * <p/>
 * The {@linkplain mockit.Tested @Tested} annotation allows for the instantiation of classes under test, with the
 * injection and/or recursive instantiation of dependencies.
 * It can inject mocked instances when used in combination with the {@linkplain mockit.Injectable @Injectable} mocking
 * annotation, and also inject <em>non-mocked</em> (real) instances when applied as
 * <tt>@Tested(fullyInitialized = true)</tt>.
 * Non-mocked instances are either automatically created or obtained from other <tt>@Tested</tt> fields.
 * <p/>
 * The {@link mockit.Expectations} class provides an API for the <em>record-replay</em> model of recording expected
 * invocations which are later replayed and implicitly verified.
 * This API makes use of the {@linkplain mockit.Mocked @Mocked}, {@linkplain mockit.Injectable @Injectable}, and
 * {@linkplain mockit.Capturing @Capturing} <em>mocking</em> annotations.
 * The {@link mockit.Verifications} class extends the record-replay model to a <em>record-replay-verify</em> model,
 * where expectations that were not recorded can be verified explicitly <em>after</em> exercising the code under test
 * (ie, after the replay phase).
 * The {@link mockit.Invocation} class and the {@link mockit.Delegate} interface are also part of this API; they can be
 * used for recording expectations with custom results, and for the implementation of custom argument matchers.
 * <p/>
 * {@linkplain mockit.MockUp <code>MockUp&lt;T></code>} is a generic base class (where <tt>T</tt> is the type to be
 * faked) which allows the definition of fake implementations for external classes or interfaces.
 * Methods in a fake class that take the place of the "real" methods in class <tt>T</tt> are indicated with the
 * {@linkplain mockit.Mock @Mock} annotation.
 * Such methods can also (optionally) have an {@link mockit.Invocation} parameter.
 * <p/>
 * Finally, the {@link mockit.Deencapsulation} class provides a few Reflection-based utility methods, for the rare cases
 * where a <tt>private</tt> field needs to be accessed.
 * <p/>
 * For a description with examples of the <em>Mocking</em> API, see the
 * "<a href="http://jmockit.org/tutorial/Mocking.html" target="tutorial">Mocking</a>" chapter in the Tutorial.
 * For the <em>Faking</em> API, see the "<a href="http://jmockit.org/tutorial/Faking.html" target="tutorial">Faking</a>"
 * chapter.
 */
package mockit;
