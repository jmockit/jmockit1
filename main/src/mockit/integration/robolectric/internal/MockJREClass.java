/*
 * Copyright (c) 2006-2014 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.integration.robolectric.internal;

import java.lang.reflect.*;

import mockit.*;
import mockit.internal.util.*;

public final class MockJREClass extends MockUp<Class<?>>
{
   @Mock
   public static Method getMethod(Invocation inv, String name, Class<?>[] parameterTypes) throws NoSuchMethodException
   {
      try {
         return proceed(inv);
      }
      catch (NoSuchMethodException e) {
         StackTrace stackTrace = new StackTrace(e);
         int n = stackTrace.getDepth();

         for (int i = 9; i < n; i++) {
            StackTraceElement ste = stackTrace.getElement(i);

            if ("RobolectricTestRunner.java".equals(ste.getFileName())) {
               Class<?> testClass = inv.getInvokedInstance();

               for (Method testMethod : testClass.getMethods()) {
                  if (testMethod.getName().equals(name)) {
                     return testMethod;
                  }
               }

               break;
            }
         }

         throw e;
      }
   }

   private static Method proceed(Invocation inv) throws NoSuchMethodException
   {
      return inv.proceed();
   }
}
