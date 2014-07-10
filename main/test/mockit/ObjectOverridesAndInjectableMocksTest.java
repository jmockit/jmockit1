/*
 * Copyright (c) 2006-2012 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit;

import static org.junit.Assert.*;
import org.junit.*;

@SuppressWarnings({"ObjectEqualsNull", "EqualsBetweenInconvertibleTypes", "LiteralAsArgToStringEquals", "SimplifiableJUnitAssertion"})
public final class ObjectOverridesAndInjectableMocksTest
{
   @Injectable ClassWithObjectOverrides a;
   @Injectable ClassWithObjectOverrides b;

   @Test
   public void verifyStandardBehaviorOfOverriddenEqualsMethodsInMockedClass()
   {
      assertDefaultEqualsBehavior(a, b);
      assertDefaultEqualsBehavior(b, a);
   }

   private void assertDefaultEqualsBehavior(Object a, Object b)
   {
      assertFalse(a.equals(null));
      assertFalse(a.equals("test"));
      assertTrue(a.equals(a));
      assertFalse(a.equals(b));
   }

   @Test
   public void allowAnyInvocationsOnOverriddenObjectMethodsForStrictMocks()
   {
      new Expectations() {{ a.getIntValue(); result = 58; }};

      assertFalse(a.equals(b));
      assertTrue(a.equals(a));
      assertEquals(58, a.getIntValue());
      assertFalse(b.equals(a));
      assertFalse(a.equals(b));
   }
}
