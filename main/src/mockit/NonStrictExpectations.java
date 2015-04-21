/*
 * Copyright (c) 2006-2015 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit;

/**
 * Used to record <em>non-strict</em> expectations on {@linkplain Mocked mocked} types and their instances.
 * The difference from {@linkplain Expectations regular expectations} is that a non-strict recorded expectation
 * is allowed to have <em>no</em> matching invocations (unless otherwise specified).
 * This property makes them particularly useful for test setup methods such as a JUnit {@code @Before} method, or a
 * {@code @BeforeMethod} in a TestNG test class, since such expectations may be used by some tests only, but not all in
 * the test class.
 *
 * @see #NonStrictExpectations()
 * @see #NonStrictExpectations(Object...)
 * @see #NonStrictExpectations(Integer, Object...)
 * @see <a href="http://jmockit.org/tutorial/BehaviorBasedTesting.html#expectation">Tutorial</a>
 */
public abstract class NonStrictExpectations extends Expectations
{
   /**
    * Registers one or more non-strict expectations recorded on available mocked types and/or mocked instances, as
    * written inside the instance initialization body of an anonymous subclass or the called constructor of a named
    * subclass.
    *
    * @see #NonStrictExpectations(Object...)
    * @see #NonStrictExpectations(Integer, Object...)
    */
   protected NonStrictExpectations() {}

   /**
    * Same as {@link #NonStrictExpectations()}, except that one or more classes will be partially mocked according to
    * the expectations recorded in the expectation block.
    * <p/>
    * The classes to be partially mocked are those directly specified through their {@code Class} objects as well as
    * those to which any given objects belong.
    * During replay, any invocations to one of these classes or objects will execute real production code, unless a
    * matching expectation was recorded.
    * <p/>
    * For a given {@code Class} object, all constructors and methods can be mocked, from the specified class up to but
    * not including {@code java.lang.Object}.
    * For a given <em>object</em>, only methods can be mocked, not constructors; also, during replay, invocations to
    * instance methods will only match expectations recorded on the given instance (or instances, if more than one was
    * given).
    *
    * @param classesOrObjectsToBePartiallyMocked one or more classes or objects whose classes are to be partially mocked
    *
    * @throws IllegalArgumentException if given a class literal for an interface, an annotation, an array, a
    * primitive/wrapper type, or a {@linkplain java.lang.reflect.Proxy#isProxyClass(Class) proxy class} created for an
    * interface, or if given a value/instance of such a type
    *
    * @see #NonStrictExpectations(Integer, Object...)
    * @see <a href="http://jmockit.org/tutorial/BehaviorBasedTesting.html#partial">Tutorial</a>
    */
   protected NonStrictExpectations(Object... classesOrObjectsToBePartiallyMocked)
   {
      super(classesOrObjectsToBePartiallyMocked);
   }

   /**
    * Same as {@link #NonStrictExpectations(Object...)}, but considering that the invocations inside the block will
    * occur in a given number of iterations.
    * <p/>
    * The effect of specifying a number of iterations larger than 1 (one) is equivalent to multiplying by that number
    * the lower and upper invocation count limits for each invocation inside the expectation block.
    * Note that by default the invocation count range for a non-strict expectation is [0, ∞), that is, a lower limit of
    * 0 (zero) and no upper limit, so the number of iterations will only be meaningful if a positive and finite limit is
    * explicitly specified for the expectation.
    *
    * @param numberOfIterations the positive number of iterations for the whole set of invocations recorded inside the
    * block; when not specified, 1 (one) iteration is assumed
    * @param classesOrObjectsToBePartiallyMocked one or more classes or objects whose classes are to be partially mocked
    *
    * @see #NonStrictExpectations()
    * @see <a href="http://jmockit.org/tutorial/BehaviorBasedTesting.html#iteratedExpectations">Tutorial</a>
    */
   protected NonStrictExpectations(Integer numberOfIterations, Object... classesOrObjectsToBePartiallyMocked)
   {
      super(classesOrObjectsToBePartiallyMocked);
      getCurrentPhase().setNumberOfIterations(numberOfIterations);
   }
}
