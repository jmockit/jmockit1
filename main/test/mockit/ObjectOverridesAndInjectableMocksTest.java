package mockit;

import static org.junit.Assert.*;
import org.junit.*;

@SuppressWarnings({"ObjectEqualsNull", "SimplifiableJUnitAssertion"})
public final class ObjectOverridesAndInjectableMocksTest
{
   @Injectable ClassWithObjectOverrides a;
   @Injectable ClassWithObjectOverrides b;

   @Test
   public void verifyStandardBehaviorOfOverriddenEqualsMethodsInMockedClass() {
      assertDefaultEqualsBehavior(a, b);
      assertDefaultEqualsBehavior(b, a);
   }

   void assertDefaultEqualsBehavior(Object obj1, Object obj2) {
      assertFalse(obj1.equals(null));
      assertFalse(obj1.equals("test"));
      //noinspection EqualsWithItself
      assertTrue(obj1.equals(obj1));
      assertFalse(obj1.equals(obj2));
   }

   @Test
   public void allowAnyInvocationsOnOverriddenObjectMethodsForStrictMocks() {
      new Expectations() {{ a.getIntValue(); result = 58; }};

      assertFalse(a.equals(b));
      //noinspection EqualsWithItself
      assertTrue(a.equals(a));
      assertEquals(58, a.getIntValue());
      assertFalse(b.equals(a));
      assertFalse(a.equals(b));
   }

   static class BaseClass {
      final int value;
      BaseClass(int value) { this.value = value; }
      @Override public boolean equals(Object obj) { return value == ((BaseClass) obj).value; }
   }
   static class Subclass1 extends BaseClass { Subclass1() { super(1); } }
   static class Subclass2 extends BaseClass { Subclass2() { super(2); } }

   @Test
   public void executeEqualsOverrideOnInstancesOfDifferentSubclassThanTheOneMocked(@Injectable Subclass1 mocked) {
      Object s1 = new Subclass2();
      Object s2 = new Subclass2();

      boolean cmp = s1.equals(s2);

      assertTrue(cmp);
   }
}
