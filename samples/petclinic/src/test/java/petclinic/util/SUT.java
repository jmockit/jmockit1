package petclinic.util;

import java.lang.annotation.*;

import mockit.*;

/**
 * Designates a field containing an object to be tested, which gets automatically created before any @Test method as
 * needed; any fields of the object for other dependencies to be injected are also automatically created or resolved.
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Tested(fullyInitialized = true)
public @interface SUT {}
