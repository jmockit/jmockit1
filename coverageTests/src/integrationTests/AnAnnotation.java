package integrationTests;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
public @interface AnAnnotation
{
   int[] integers() default {};
}
