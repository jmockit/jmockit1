package petclinic.util;

import java.lang.annotation.*;

import mockit.*;

/**
 * Designates a field containing a test utility object, which will be initialized before any @Test or @Before
 * method is executed.
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Tested(availableDuringSetup = true, fullyInitialized = true)
public @interface TestUtil {}
