package blog.common;

import java.lang.annotation.*;

import mockit.*;

/**
 * Used to indicate a field in a test class which will hold a dependency injected into the
 * {@linkplain ObjectUnderTest object under test}.
 */
@Tested(fullyInitialized = true)
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Dependency {}
