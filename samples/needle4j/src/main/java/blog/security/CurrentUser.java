package blog.security;

import java.lang.annotation.*;
import javax.inject.*;

@Qualifier
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface CurrentUser {}
