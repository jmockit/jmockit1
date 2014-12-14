/*
 * Copyright (c) 2006-2014 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.integration.testng.internal;

import org.jetbrains.annotations.*;
import org.testng.*;

import mockit.*;
import mockit.internal.util.*;

public final class MockTestNG extends MockUp<TestNG>
{
   public static boolean hasDependenciesInClasspath()
   {
      return ClassLoad.searchTypeInClasspath("org.testng.TestNG", true) != null;
   }

   @Mock
   public static void init(@NotNull Invocation invocation, boolean useDefaultListeners)
   {
      invocation.proceed();

      TestNG it = invocation.getInvokedInstance();
      assert it != null;
      TestNGRunnerDecorator.registerWithTestNG(it);
   }
}
