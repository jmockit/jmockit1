/*
 * Copyright (c) 2006-2014 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit;

import java.lang.annotation.*;

/**
 * Indicates that the value of a mock field or mock parameter will be an isolated {@linkplain Mocked mocked} instance,
 * intended to be passed or <em>injected</em> into the code under test.
 * Such instances can be said to be proper <em>mock objects</em>, in contrast to the mocked instances of a regular
 * {@code @Mocked} type.
 * <p/>
 * For the duration of each test where the mock field/parameter is in scope, <em>only one</em> injectable instance is
 * mocked; other instances of the same mocked type are not affected.
 * For an injectable mocked <em>class</em>, <em>static methods</em> and <em>constructors</em> are <em>not</em> mocked;
 * only instance methods are.
 * <p/>
 * When used in combination with {@linkplain Tested @Tested}, the values of injectable fields and parameters will be
 * used for automatic injection into the tested object.
 * Additionally, this annotation can be applied to non-mocked fields of primitive or array types, which will also be
 * used for injection.
 *
 * @see #value
 * @see <a href="http://jmockit.org/tutorial/BehaviorBasedTesting.html#injectable">Tutorial</a>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.PARAMETER})
public @interface Injectable
{
   /**
    * Specifies a literal value when the type of the injectable mock field/parameter is {@code String}, a primitive
    * type, or an enum type.
    * For a primitive type, the value provided must be convertible to it.
    * For an enum type, the given textual value must equal the name of one of the possible enum values.
    * <p/>
    * When a value is provided for an injectable whose type is {@code String} or an enum type, said type is <em>not</em>
    * mocked; otherwise, it is.
    */
   String value() default "";
}
