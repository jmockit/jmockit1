package petclinic.util;

import java.lang.annotation.*;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.*;

import mockit.*;

/**
 * Designates a field containing a test utility object, which will be initialized before any @Test or @Before
 * method is executed.
 */
@Target(FIELD)
@Retention(RUNTIME)
@Tested(availableDuringSetup = true, fullyInitialized = true)
public @interface TestUtil {}
