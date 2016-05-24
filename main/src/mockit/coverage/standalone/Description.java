/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.coverage.standalone;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@interface Description
{
   String value();
}
