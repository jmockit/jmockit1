/*
 * Copyright (c) 2006-2014 Rogério Liesenfeld
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

   public static void doStatic1() { throw new RuntimeException("Real method 1 called"); }
   public static void doStatic2() { throw new RuntimeException("Real method 2 called"); }

   public void doSomething1() { throw new RuntimeException("Real method 1 called"); }
   public void doSomething2() { throw new RuntimeException("Real method 2 called"); }

   void setLongField(long value) { longField = value; }
}
