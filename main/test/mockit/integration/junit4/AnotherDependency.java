/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.integration.junit4;

public final class AnotherDependency
{
   static boolean mockedAtSuiteLevel;
   public static boolean alwaysTrue() { return true; }
}
