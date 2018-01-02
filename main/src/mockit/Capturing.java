/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit;

import java.lang.annotation.*;
import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.*;

/**
 * Indicates a mock field or a mock parameter for which all classes extending/implementing the
 * {@linkplain Mocked mocked} type will <em>also</em> get mocked.
 * <p/>
 * <em>Future</em> instances of a capturing mocked type (ie, instances created sometime later during the test) will
 * become associated with the mock field/parameter.
 * When recording or verifying expectations on the mock field/parameter, these associated instances are regarded as
 * equivalent to the original mocked instance created for the mock field/parameter.
 *
 * @see <a href="http://jmockit.org/tutorial/Mocking.html#capturing" target="tutorial">Tutorial</a>
 */
@Retention(RUNTIME)
@Target({FIELD, PARAMETER})
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
    * <tt>maxInstances</tt> for each one of them, say <tt>n1</tt>, <tt>n2</tt>, etc.
    * In this case, the first <tt>n1</tt> new instances will be associated with the first field/parameter, the following
    * <tt>n2</tt> new instances to the second, and so on.
    *
    * @deprecated This is a rarely used feature which, when used, inevitably creates a hard to understand test which
    * most likely is mocking too much; it will be removed in a future release, with no replacement.
    */
   @Deprecated
   int maxInstances() default Integer.MAX_VALUE;
}
