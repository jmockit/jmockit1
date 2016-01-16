/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit;

import java.util.*;
import javax.annotation.*;

import org.junit.*;
import static org.junit.Assert.*;

public final class NaturalOrderingTest
{
   @Test
   public void verifyStandardComparableBehaviorInMockedClass(@Mocked Date a, @Mocked Date b)
   {
      assertEquals(0, a.compareTo(a));
      assertEquals(0, b.compareTo(b));

      int aXb = a.compareTo(b);
      assertTrue(aXb != 0);

      int bXa = b.compareTo(a);
      assertTrue(bXa != 0);

      assertEquals(aXb, -bXa);
   }

   @SuppressWarnings("ComparableImplementedButEqualsNotOverridden")
   static final class ComparableClass implements Comparable<String>
   {
      final String value;
      ComparableClass(String value) { this.value = value; }
      @Override public int compareTo(@Nonnull String s) { return value.compareTo(s); }
   }

   @Test
   public void mockOverrideOfCompareToMethod(@Mocked final ComparableClass a, @Mocked final ComparableClass b)
   {
      new Expectations() {{
         a.compareTo(null); result = 5;
         a.compareTo(anyString); result = 123;
      }};

      new Expectations() {{
         b.compareTo("test"); result = -50;
      }};

      assertEquals(5, a.compareTo(null));
      assertEquals(123, a.compareTo("test"));
      assertEquals(-50, b.compareTo("test"));
   }

   @Test
   public void mockOverrideOfCompareToMethodInJREClass(@Mocked final Date a, @Mocked final Date b)
   {
      new Expectations() {{
         a.compareTo(b); result = 5;
      }};

      assertEquals(5, a.compareTo(b));
      assertTrue(b.compareTo(a) != 0);

      new Verifications() {{
         a.compareTo((Date) any);
         b.compareTo(a);
      }};
   }
}
