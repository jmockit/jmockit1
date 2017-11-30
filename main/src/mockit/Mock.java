/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit;

import java.lang.annotation.*;
import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.*;

/**
 * Used inside a {@linkplain MockUp fake} class to indicate a <em>fake method</em> whose implementation will temporarily
 * replace the implementation of a matching "real" method.
 * <p/>
 * The fake method must have the same name and the same parameters as the matching real method, except for an optional
 * first parameter of type {@link Invocation}; if this extra parameter is present, the remaining ones must match the
 * parameters in the real method.
 * The fake method must also have the same return type as the matching real method.
 * <p/>
 * Method modifiers (<code>public</code>, <tt>final</tt>, <tt>static</tt>, etc.) between fake and faked methods
 * <em>don't</em> have to be the same.
 * It's perfectly fine to have a non-<code>static</code> fake method for a <tt>static</tt> faked method (or vice-versa),
 * for example.
 * Checked exceptions in the <tt>throws</tt> clause (if any) can also differ between the two matching methods.
 * <p/>
 * A fake <em>method</em> can also target a <em>constructor</em>, in which case the previous considerations still apply,
 * except for the name of the fake method which must be "<strong><code>$init</code></strong>".
 * <p/>
 * Another special fake method, "<strong><code>void $clinit()</code></strong>", will target the <tt>static</tt>
 * initializers of the faked class, if present in the fake class.
 * <p/>
 * Yet another special fake method is "<strong><code>Object $advice(Invocation)</code></strong>", which if defined will
 * match <em>every</em> method in the target class hierarchy.
 *
 * @see <a href="http://jmockit.org/tutorial/Faking.html#fakes" target="tutorial">Tutorial</a>
 */
@Retention(RUNTIME)
@Target(METHOD)
public @interface Mock
{
}
