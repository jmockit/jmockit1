/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit;

/**
 * An empty interface to be used with the {@link Expectations#result} field or the
 * <a href="Expectations.html#with(mockit.Delegate)">Invocations#with(Delegate)</a> method, allowing test code to define
 * varying invocation results or argument matching rules, respectively.
 * <p/>
 * When combined with the <tt>result</tt> field, a test will typically assign it with an anonymous class object
 * implementing this interface and containing a <em>delegate method</em>:
 * <pre>
 *    new Expectations() {{
 *       mock.doSomething(anyInt, anyString);
 *       result = new Delegate() {
 *          String <strong>delegate</strong>(int i, String s) {
 *             return i > 0 ? s : "";
 *          }
 *       };
 *    }};
 *
 *    tested.exerciseCodeUnderTest();
 * </pre>
 * The delegate class (either named or anonymous) must contain exactly one non-<code>private</code> instance method to
 * be executed when the mocked method or mocked constructor is invoked; it can contain any number of <tt>private</tt>
 * or <tt>static</tt> methods, though.
 * The name of the delegate method can be anything.
 * Its parameters, however, should be the same as the parameters of the corresponding mocked method/constructor, or at
 * least be compatible with them.
 * Optionally, the delegate method can have an extra parameter of type {@link Invocation}, provided it appears as the
 * first one.
 * The delegate method is also allowed to have <em>no</em> parameters (without counting the optional <tt>Invocation</tt>
 * parameter).
 * <p/>
 * When used with the <tt>result</tt> field, the result of a delegate method execution can be any return value compatible
 * with the recorded method's return type, or a thrown error/exception.
 * <p/>
 * When used with the <tt>with(Delegate)</tt> method, the delegate method must return a <tt>boolean</tt>, being
 * <tt>true</tt> for a successfully matched argument or <tt>false</tt> otherwise.
 * <p/>
 * Finally, note that a <tt>static</tt> method in the mocked type can have a delegate as well.
 * The same is true for <tt>private</tt>, <tt>final</tt>, and <tt>native</tt> methods.
 *
 * @see <a href="http://jmockit.org/tutorial/Mocking.html#delegates" target="tutorial">Tutorial</a>
 *
 * @param <T> the type of the argument to be matched, when used with the <tt>with(Delegate)</tt> method
 */
public interface Delegate<T>
{
}
