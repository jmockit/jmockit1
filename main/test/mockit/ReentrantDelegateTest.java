/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit;

import org.junit.*;
import static org.junit.Assert.*;

public final class ReentrantDelegateTest
{
   public static class RealClass
   {
      protected static int nonRecursiveStaticMethod(int i) { return -i; }
      public int nonRecursiveMethod(int i) { return -i; }
   }

   @Test
   public void recursiveDelegateMethodWithoutInvocationParameter()
   {
      new Expectations(RealClass.class) {{
         RealClass.nonRecursiveStaticMethod(anyInt);
         result = new Delegate() {
            @Mock
            int delegate(int i)
            {
               if (i > 1) return i;
               return RealClass.nonRecursiveStaticMethod(i + 1);
            }
         };
      }};

      int result = RealClass.nonRecursiveStaticMethod(1);
      assertEquals(2, result);
   }

   @Test
   public void recursiveDelegateMethodWithInvocationParameterNotUsedForProceeding(@Injectable final RealClass rc)
   {
      new Expectations() {{
         rc.nonRecursiveMethod(anyInt);
         result = new Delegate() {
            @Mock
            int delegate(Invocation inv, int i)
            {
               if (i > 1) return i;
               RealClass it = inv.getInvokedInstance();
               return it.nonRecursiveMethod(i + 1);
            }
         };
      }};

      int result = rc.nonRecursiveMethod(1);
      assertEquals(2, result);
   }

   @Test
   public void nonRecursiveDelegateMethodWithInvocationParameterUsedForProceeding(@Injectable final RealClass rc)
   {
      new Expectations() {{
         rc.nonRecursiveMethod(anyInt);
         result = new Delegate() {
            @Mock
            int nonRecursiveMethod(Invocation inv, int i)
            {
               if (i > 1) return i;
               return inv.proceed(i + 1);
            }
         };
      }};

      int result = rc.nonRecursiveMethod(1);
      assertEquals(-2, result);
   }
}
