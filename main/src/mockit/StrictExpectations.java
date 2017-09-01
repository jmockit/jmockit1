/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit;

/**
 * Used to record <em>strict</em> expectations on {@linkplain Mocked mocked} types and their instances.
 * <p/>
 * During replay, invocations matching strictly recorded expectations must occur in the same number <em>and</em> the
 * same order.
 * Any invocation that doesn't match a recorded expectation will cause an {@code UnexpectedInvocation} error to be
 * thrown, failing the test.
 * <p/>
 * Strict expectations do not allow the use of {@linkplain Verifications verification blocks} for explicit verification.
 * Instead, one or more strict expectation blocks are meant to fully specify <em>all</em> the invocations to mocked
 * types/instances that are expected and allowed to occur from the code under test.
 *
 * @see #StrictExpectations()
 * @see #StrictExpectations(Object...)
 * @deprecated Use {@link Expectations} instead; to detect unrecorded invocations (if desired), use
 * {@link FullVerifications}.
 */
@Deprecated
public abstract class StrictExpectations extends Expectations
{
   /**
    * Registers one or more strict expectations recorded on available mocked types and/or mocked instances, as written
    * inside the instance initialization body of an anonymous subclass or the called constructor of a named subclass.
    *
    * @see #StrictExpectations(Object...)
    */
   protected StrictExpectations() {}

   /**
    * Same as {@link #StrictExpectations()}, except that one or more classes will be partially mocked according to the
    * expectations recorded in the expectation block.
    * <p/>
    * The classes to be partially mocked are those directly specified through their {@code Class} objects as well as
    * those to which any given objects belong.
    * During replay, any invocations to one of these classes or objects will execute real production code, unless a
    * matching expectation was recorded.
    * This mechanism, however, does not apply to {@code native} methods, which are not supported for partial mocking.
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
    * @see <a href="http://jmockit.org/tutorial/Mocking.html#partial" target="tutorial">Tutorial</a>
    */
   protected StrictExpectations(Object... classesOrObjectsToBePartiallyMocked)
   {
      super(classesOrObjectsToBePartiallyMocked);
   }
}
