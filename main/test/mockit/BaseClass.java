/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit;

import java.util.*;

public class BaseClass
{
   protected int baseInt;
   protected String baseString;
   protected Set<Boolean> baseSet;

   @SuppressWarnings({"FieldCanBeLocal", "unused"})
   private long longField;

   void setLongField(long value) { longField = value; }
}
