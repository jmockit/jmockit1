package otherTests;

import java.util.*;

import org.junit.*;
import static org.junit.Assert.*;

import mockit.*;

import static org.hamcrest.CoreMatchers.is;

public final class JUnit4Test
{
   @Mocked ClassWithObjectOverrides mock;

   @SuppressWarnings("EqualsBetweenInconvertibleTypes")
   @Test
   public void useMockedInstance() {
      new ClassWithObjectOverrides("test");
      assertFalse(mock.toString().isEmpty());
      mock.equals("123");
      //noinspection ObjectEqualsNull
      mock.equals(null);

      new Verifications() {{
         String s;
         mock.equals(s = withCapture());
         assertEquals("123", s);

         List<ClassWithObjectOverrides> objs = withCapture(new ClassWithObjectOverrides("test"));
         assertEquals(1, objs.size());

         mock.equals(withNull());
      }};
   }

   static class AnotherClass {
      void doSomething(int i, long l, Short s, byte b, char c, double d, float f, String str) {
         System.out.println(i + l + s + b + d + f);
         System.out.println(c + str);
      }
      String getModifiedValue(String s) { return s.toLowerCase(); }
   }

   @Test
   public void useArgumentMatchingFields(@Injectable final AnotherClass anotherMock) {
      new Expectations() {{
         anotherMock.doSomething(anyInt, anyLong, anyShort, anyByte, anyChar, anyDouble, anyFloat, anyString);
      }};

      anotherMock.doSomething(1, 2, (short) 3, (byte) 4, 'c', 1.2, 2.5F, "");
   }

   @Test
   public void useArgumentMatchingMethods(@Injectable final AnotherClass anotherMock) {
      new Expectations() {{
         anotherMock.doSomething(
            withAny(0), withEqual(2L), withInstanceOf(short.class), withNotEqual((byte) 1), withInstanceLike(' '),
            withEqual(1.2, 0), withEqual(2.5F, 0), withSameInstance("test"));

         anotherMock.getModifiedValue(withSubstring("abc")); result = "Abc";
         anotherMock.getModifiedValue(withPrefix("TX")); result = "abc";
         anotherMock.getModifiedValue(withSuffix("X")); result = "ABC";
         anotherMock.getModifiedValue(withMatch("\\d+")); result = "number";
         anotherMock.getModifiedValue(withArgThat(is("test"))); result = "test";

         anotherMock.getModifiedValue("Delegate");
         result = new Delegate() {
            @Mock
            String delegate(Invocation inv, String s) {
               assertNotNull(inv.getInvokedMember());
               assertTrue(inv.getInvocationIndex() >= 0);
               assertTrue(inv.getInvocationCount() >= 1);
               assertEquals(1, inv.getInvokedArguments().length);
               return inv.proceed();
            }
         };
      }};

      anotherMock.doSomething(1, 2, (short) 3, (byte) 4, 'c', 1.2, 2.5F, "test");

      assertEquals("Abc", anotherMock.getModifiedValue("test abc xyz"));
      assertEquals("abc", anotherMock.getModifiedValue("TX test"));
      assertEquals("ABC", anotherMock.getModifiedValue("test X"));
      assertEquals("number", anotherMock.getModifiedValue("123"));
      assertEquals("test", anotherMock.getModifiedValue("test"));
      assertEquals("delegate", anotherMock.getModifiedValue("Delegate"));

      new Verifications() {{
         List<String> values = new ArrayList<>();
         anotherMock.getModifiedValue(withCapture(values));
         assertEquals(6, values.size());
      }};
   }
}
