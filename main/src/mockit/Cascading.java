/*
 * Copyright (c) 2006-2014 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit;

import java.lang.annotation.*;

/**
 * Indicates a mock field or mock parameter whose {@linkplain Mocked mocked} type/instance will produce
 * <em>cascading</em> mocked instances as non-<code>void</code> methods get called.
 * So, whenever a mocked method is invoked on a cascading mocked type/instance, a <em>cascaded</em> mocked instance will
 * be automatically returned instead of {@code null}, provided the return type of the method is of an allowed reference
 * type.
 * Further invocations can then be made on cascaded instances, with the cascading process repeating itself as needed.
 * <p/>
 * Methods returning {@code String}, primitive wrappers, or collection types are <em>not</em> considered for cascading.
 * <p/>
 * In a test having a cascading mocked type, a separate non-cascading mocked type can be declared so that expectations
 * on intermediate cascaded instances can be recorded/verified.
 *
 * @see <a href="http://jmockit.googlecode.com/svn/trunk/www/tutorial/BehaviorBasedTesting.html#cascading">Tutorial</a>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.PARAMETER})
public @interface Cascading
{
}