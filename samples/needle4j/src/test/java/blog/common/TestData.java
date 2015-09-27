package blog.common;

import java.lang.annotation.*;

import mockit.*;

@Tested(availableDuringSetup = true, fullyInitialized = true)
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface TestData {}
