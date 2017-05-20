/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit;

import java.lang.annotation.*;

/**
 * Used inside a {@linkplain MockUp mock-up} class to indicate a <em>mock (fake) method</em> whose implementation will
 * temporarily replace the implementation of a matching "real" method.
 * <p/>
 * The mock method must have the same name and the same parameters as the matching real method, except for an optional
 * first parameter of type {@link Invocation}; if this extra parameter is present, the remaining ones must match the
 * parameters in the real method.
 * The mock method must also have the same return type as the matching real method.
 * <p/>
 * Method modifiers (<code>public</code>, {@code final}, {@code static}, etc.) between mock and mocked
 * methods <em>don't</em> have to be the same.
 * It's perfectly fine to have a non-<code>static</code> mock method for a {@code static} mocked method (or vice-versa),
 * for example.
 * Checked exceptions in the {@code throws} clause (if any) can also differ between the two matching methods.
 * <p/>
 * A mock <em>method</em> can also target a <em>constructor</em>, in which case the previous considerations still apply,
 * except for the name of the mock method which must be "<strong><code>$init</code></strong>".
 * <p/>
 * Another special mock method, "<strong><code>void $clinit()</code></strong>", will target the {@code static}
 * initializers of the faked class, if present in the mock-up class.
 * <p/>
 * Yet another special mock method is "<strong><code>Object $advice(Invocation)</code></strong>", which if defined will
 * match <em>every</em> method in the target class hierarchy.
 *
 * @see <a href="http://jmockit.org/tutorial/Faking.html#mocks" target="tutorial">Tutorial</a>
 */
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Mock
{
}
