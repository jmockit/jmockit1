/*
 * Copyright (c) 2006-2014 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit;

/**
 * Used to record <em>strict</em> expectations on {@linkplain mockit.Mocked mocked} types and their instances.
 * <p/>
 * During replay, invocations matching strictly recorded expectations must occur in the same number <em>and</em> the
 * same order.
 * Any invocation that doesn't match a recorded expectation will cause an {@code UnexpectedInvocation} error to be
 * thrown, failing the test.
 * <p/>
 * Strict expectations do not allow the use of {@linkplain mockit.Verifications verification blocks} for explicit
 * verification.
 * Instead, one or more strict expectation blocks are meant to fully specify <em>all</em> the invocations to mocked
 * types/instances that are expected and allowed to occur from the code under test.
 *
 * @see #StrictExpectations()
 * @see #StrictExpectations(Object...)
 * @see #StrictExpectations(Integer, Object...)
 * @see <a href="http://jmockit.github.io/tutorial/BehaviorBasedTesting.html#expectation">Tutorial</a>
 */
public abstract class StrictExpectations extends Expectations
{
   /**
    * Registers one or more strict expectations recorded on available mocked types and/or mocked instances, as written
    * inside the instance initialization body of an anonymous subclass or the called constructor of a named subclass.
    *
    * @see #StrictExpectations(Object...)
    * @see #StrictExpectations(Integer, Object...)
    */
   protected StrictExpectations() {}

   /**
    * Same as {@link #StrictExpectations()}, except that one or more classes will be partially mocked according to the
    * expectations recorded in the expectation block; this feature is known as <em>dynamic</em> partial mocking, in
    * contrast with <em>static</em> partial mocking as specified with the {@link mockit.Mocked#value} annotation
    * attribute.
    * <p/>
    * The classes to be partially mocked are those directly specified through their {@code Class} objects as well as
    * those to which any given objects belong.
    * During replay, any invocations to one of these classes or objects will execute real production code, unless a
    * matching expectation was recorded.
    * <p/>
    * For a given {@code Class} object, all constructors and methods will be considered for mocking, from the specified
    * class up to but not including {@code java.lang.Object}.
    * <p/>
    * For a given <em>object</em>, all methods will be considered for mocking, from the concrete class of the given
    * object up to but not including {@code java.lang.Object}.
    * The constructors of those classes will <em>not</em> be considered.
    * During replay, invocations to instance methods will only match expectations recorded on the given instance
    * (or instances, if more than one was given).
    *
    * @param classesOrObjectsToBePartiallyMocked one or more classes or objects whose classes are to be considered for
    * partial mocking
    *
    * @throws IllegalArgumentException if given a class literal for an interface, an annotation, an array, a
    * primitive/wrapper type, or a {@linkplain java.lang.reflect.Proxy#isProxyClass(Class) proxy class} created for an
    * interface, or if given a value/instance of such a type
    *
    * @see #StrictExpectations(Integer, Object...)
    * @see <a href="http://jmockit.github.io/tutorial/BehaviorBasedTesting.html#dynamicPartial">Tutorial</a>
    */
   protected StrictExpectations(Object... classesOrObjectsToBePartiallyMocked)
   {
      super(classesOrObjectsToBePartiallyMocked);
   }

   /**
    * Same as {@link #StrictExpectations(Object...)}, but considering that the invocations inside the block will occur
    * in a given number of iterations.
    * <p/>
    * The effect of specifying a number of iterations larger than 1 (one) is equivalent to duplicating the whole
    * sequence of invocations in the expectation block.
    *
    * @param numberOfIterations the positive number of iterations for the whole sequence of invocations recorded inside
    * the block; when not specified, 1 (one) iteration is assumed
    * @param classesOrObjectsToBePartiallyMocked one or more classes or objects whose classes are to be considered for
    * partial mocking
    *
    * @see #StrictExpectations()
    * @see <a href="http://jmockit.github.io/tutorial/BehaviorBasedTesting.html#iteratedExpectations">Tutorial</a>
    */
   protected StrictExpectations(Integer numberOfIterations, Object... classesOrObjectsToBePartiallyMocked)
   {
      super(classesOrObjectsToBePartiallyMocked);
      getCurrentPhase().setNumberOfIterations(numberOfIterations);
   }
}
