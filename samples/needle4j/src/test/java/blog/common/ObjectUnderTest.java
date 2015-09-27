package blog.common;

import java.lang.annotation.*;

import mockit.*;

/**
 * Used to indicate a field in a test class which holds the object under test.
 * Any dependencies this object may have are automatically resolved, either from {@link Injectable} mock instances, or
 * through the creation of real instances which are themselves initialized in the same way.
 * <p/>
 * Dependencies recursively injected into the object under test can be made accessible to tests by declaring
 * {@link Dependency} fields.
 */
@Tested(fullyInitialized = true)
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface ObjectUnderTest {}
