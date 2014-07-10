/*
 * Copyright (c) 2006-2013 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.integration.testng.internal;

import org.jetbrains.annotations.*;
import org.testng.*;

import mockit.*;

public final class MockTestNG extends MockUp<TestNG>
{
   public static boolean hasDependenciesInClasspath()
   {
      try {
         Class.forName(TestNG.class.getName(), true, TestNG.class.getClassLoader());
         return true;
      }
      catch (NoClassDefFoundError ignore) { return false; }
      catch (ClassNotFoundException ignore) { return false; }
   }

   @Mock
   public void init(@NotNull Invocation invocation, boolean useDefaultListeners)
   {
      invocation.proceed();

      TestNG it = invocation.getInvokedInstance();
      assert it != null;
      TestNGRunnerDecorator.registerWithTestNG(it);
   }
}
