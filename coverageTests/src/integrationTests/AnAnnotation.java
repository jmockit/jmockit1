package integrationTests;

import java.lang.annotation.*;
import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.*;

@Target({FIELD, METHOD, TYPE, TYPE_USE})
@Retention(RUNTIME)
public @interface AnAnnotation
{
   int[] integers() default {};
   String value() default "test";
}
