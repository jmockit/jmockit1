/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit;

import java.lang.annotation.*;

/**
 * Indicates a mock field or a mock parameter for which all classes extending/implementing the
 * {@linkplain Mocked mocked} type will <em>also</em> get mocked.
 * <p/>
 * <em>Future</em> instances of a capturing mocked type (ie, instances created sometime later during the test) will
 * become associated with the mock field/parameter.
 * When recording or verifying expectations on the mock field/parameter, these associated instances are regarded as
 * equivalent to the original mocked instance created for the mock field/parameter.
 * <p/>
 * The {@link #maxInstances} attribute allows an upper limit to the number of future instances to be specified.
 * If multiple capturing mock fields/parameters of the same type are declared, this attribute can be used so that each
 * distinct instance gets associated with a separate mock field/parameter.
 * In turn, this allows different expectations to be recorded and/or verified for each grouping of future instances,
 * with each expectation using a different mock field/parameter.
 *
 * @see <a href="http://jmockit.org/tutorial/Mocking.html#capturing" target="tutorial">Tutorial</a>
 */
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.PARAMETER})
public @interface Capturing
{
   /**
    * This attribute specifies the maximum number of new instances to be covered by the capturing mock field/parameter.
    * <p/>
    * Every eligible new instance (ie, one whose type is assignable to the capturing mocked type and which gets created
    * from the code under test) is automatically associated with the corresponding mock field or mock parameter.
    * For the purpose of matching invocations from code under test to recorded or verified expectations, such associated
    * instances are regarded as equivalent to the original mocked instance created for the mock field/parameter.
    * <p/>
    * It is valid to declare two or more mock fields/parameters of the same mocked type with a positive number of
    * {@code maxInstances} for each one of them, say {@code n1}, {@code n2}, etc.
    * In this case, the first {@code n1} new instances will be associated with the first field/parameter, the following
    * {@code n2} new instances to the second, and so on.
    */
   int maxInstances() default Integer.MAX_VALUE;
}
