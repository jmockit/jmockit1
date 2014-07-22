/*
 * Copyright (c) 2006-2014 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit;

/**
 * Used to <em>record</em> non-strict expectations on {@linkplain Mocked mocked} types and mocked instances.
 * <p/>
 * A recorded expectation is intended to match a number of method or constructor invocations, that we expect will occur
 * during the execution of some code under test.
 * When a match is detected, the recorded {@linkplain #result result} is returned to the caller.
 * Alternatively, a recorded exception/error is thrown, or an arbitrary {@linkplain Delegate delegate} method is
 * executed.
 * Expectations are recorded simply by invoking the desired method or constructor on the mocked type/instance, during
 * the initialization of a {@code NonStrictExpectations} object.
 * Typically, this is done by instantiating an anonymous subclass containing an instance initialization body, or as we
 * call it, an <em>expectation block</em>:
 * <pre>
 * // <em>Record</em> one or more expectations on available mocked types/instances:
 * new NonStrictExpectations() {{
 *    <strong>mock1</strong>.expectedMethod(<em>anyInt</em>); <em>result</em> = 123; <em>times</em> = 1;
 *    <strong>MockedClass</strong>.allowedMethod(); <em>result</em> = new IOException();
 *    <strong>mock2</strong>.anotherAllowedMethod(1, "test"); returns("Abc", "xyz");
 * }};
 *
 * // Exercise tested code, with previously recorded expectations now available for <em>replay</em>.
 * codeUnderTest.doSomething();
 * </pre>
 * During replay, invocations matching recorded expectations can occur in any number and in any order.
 * The result the caller gets will be as recorded in the non-strict expectation block.
 * Invocations that don't match any recorded expectation, on the other hand, will simply result in a default value being
 * returned to the caller, or in nothing at all in the case of a {@code void} method.
 * <p/>
 * Multiple expectations on the same method or constructor can be recorded, provided different arguments are used.
 * For flexible matching of parameter values, we can use a variety of argument matching {@linkplain #anyString fields}
 * and {@linkplain #withAny(Object) methods}.
 * Also, multiple separate expectation blocks can be created in the same test method or test setup method.
 * <p/>
 * A lower/upper limit or an exact number of expected invocations can be specified for each recorded expectation,
 * by assigning the {@link #minTimes}, {@link #maxTimes}, or {@link #times} field, as appropriate, when recording the
 * expectation.
 * <p/>
 * Rather than creating anonymous subclasses, we can also create named subclasses to be reused in multiple tests.
 * Some examples:
 * <pre>
 * public final class MyReusableExpectations extends NonStrictExpectations {
 *    public MyReusableExpectations(...any parameters...) {
 *       // expectations recorded here
 *    }
 * }
 *
 * public class ReusableBaseExpectations extends NonStrictExpectations {
 *     protected ReusableBaseExpectations(...) {
 *        // expectations here
 *     }
 * }
 *
 * &#64;Test
 * public void someTest(@Mocked final SomeType aMock, etc.) {
 *     // Record reusable expectations by instantiating a <em>final</em> "...Expectations" class.
 *     new MyReusableExpectations(123, "test", <em>etc.</em>);
 *
 *     // Record and extend reusable expectations by instantiating a <em>non-final</em> base class.
 *     new ReusableBaseExpectations() {{
 *        // additional expectations
 *     }};
 * }
 * </pre>
 * Finally, non-strict expectations, either recorded or not, can be explicitly verified <em>after</em> exercising the
 * code under test, by using a set of complementary base classes: {@link Verifications},
 * {@link mockit.VerificationsInOrder}, etc.
 *
 * @see #NonStrictExpectations()
 * @see #NonStrictExpectations(Object...)
 * @see #NonStrictExpectations(Integer, Object...)
 * @see <a href="http://jmockit.github.io/tutorial/BehaviorBasedTesting.html#expectation">Tutorial</a>
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
    * the expectations recorded in the expectation block; this feature is known as <em>dynamic</em> partial mocking, in
    * contrast with <em>static</em> partial mocking as specified with the {@link Mocked#value} annotation attribute.
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
    * <p/>
    * <a href="http://jmockit.github.io/tutorial/BehaviorBasedTesting.html#dynamicPartial">In the Tutorial</a>
    *
    * @param classesOrObjectsToBePartiallyMocked one or more classes or objects whose classes are to be considered for
    * partial mocking
    *
    * @throws IllegalArgumentException if given a class literal for an interface, an annotation, an array, a
    * primitive/wrapper type, or a {@linkplain java.lang.reflect.Proxy#isProxyClass(Class) proxy class} created for an
    * interface, or if given a value/instance of such a type
    *
    * @see #NonStrictExpectations(Integer, Object...)
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
    * <p/>
    * <a href="http://jmockit.github.io/tutorial/BehaviorBasedTesting.html#iteratedExpectations">In the Tutorial</a>
    *
    * @param numberOfIterations the positive number of iterations for the whole set of invocations recorded inside the
    * block; when not specified, 1 (one) iteration is assumed
    * @param classesOrObjectsToBePartiallyMocked one or more classes or objects whose classes are to be considered for
    * partial mocking
    *
    * @see #NonStrictExpectations()
    */
   protected NonStrictExpectations(Integer numberOfIterations, Object... classesOrObjectsToBePartiallyMocked)
   {
      super(classesOrObjectsToBePartiallyMocked);
      getCurrentPhase().setNumberOfIterations(numberOfIterations);
   }
}
