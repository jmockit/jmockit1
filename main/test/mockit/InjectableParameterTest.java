package mockit;

import java.math.*;
import java.util.concurrent.atomic.*;

import org.junit.*;
import static org.junit.Assert.*;

public final class InjectableParameterTest
{
   @Test
   public void injectableParametersOfPrimitiveTypes(
      @Injectable("123") int i, @Injectable("5") long l, @Injectable("-45 ") short s, @Injectable(" 127") byte b,
      @Injectable("true") boolean f1, @Injectable(" TRUE ") boolean f2, @Injectable("A") char c,
      @Injectable(" 1.23") float f, @Injectable("-1.23") double d
   ) {
      assertEquals(123, i);
      assertEquals(5L, l);
      assertEquals(-45, s);
      assertEquals(127, b);
      assertTrue(f1);
      assertTrue(f2);
      assertEquals('A', c);
      assertEquals(1.23F, f, 0.0F);
      assertEquals(-1.23, d, 0.0);
   }

   @Test
   public void injectableParametersOfWrapperTypes(
      @Injectable("123") Integer i, @Injectable("5") Long l, @Injectable("-45 ") Short s, @Injectable(" 127") Byte b,
      @Injectable("true") Boolean f1, @Injectable(" TRUE ") Boolean f2, @Injectable("A") Character c,
      @Injectable(" 1.23") Float f, @Injectable("-1.23") Double d
   ) {
      assertEquals(123, (int) i);
      assertEquals(5L, (long) l);
      assertEquals(-45, (short) s);
      assertEquals(127, (byte) b);
      assertTrue(f1);
      assertTrue(f2);
      assertEquals('A', (char) c);
      assertEquals(1.23F, f, 0.0F);
      assertEquals(-1.23, d, 0.0);
   }

   @Test
   public void injectableParametersOfOtherNumberSubtypes(
      @Injectable(" 10.234") BigDecimal d, @Injectable("123 ") BigInteger i,
      @Injectable(" 4567 ") AtomicInteger ai, @Injectable(" 23434") AtomicLong al
   ) {
      assertEquals(10.234, d.doubleValue(), 0.0);
      assertEquals(123, i.intValue());
      assertEquals(4567, ai.intValue());
      assertEquals(23434L, al.longValue());
   }

   static class TestedClass { Number n; CharSequence text; Comparable<Float> cmp; }

   @Test
   public void injectablesHavingSubtypesOfTestedFields(
      @Tested TestedClass tested, @Injectable("test") String s, @Injectable("1.2") Float cmp, @Injectable("123") Integer n
   ) {
      assertEquals(123, tested.n.intValue());
      assertEquals("test", tested.text);
      assertEquals(1.2F, tested.cmp);
   }

   static class TestedClass2 {
      final Number n;
      final CharSequence text;
      final Comparable<Float> cmp;

      TestedClass2(Number n, CharSequence text, Comparable<Float> cmp) {
         this.n = n;
         this.text = text;
         this.cmp = cmp;
      }
   }

   @Test
   public void injectablesHavingSubtypesOfTestedConstructorParameters(
      @Tested TestedClass2 tested, @Injectable("1.2") Float cmp, @Injectable("test") String s, @Injectable("123") Integer n
   ) {
      assertEquals(123, tested.n.intValue());
      assertEquals("test", tested.text);
      assertEquals(1.2F, tested.cmp);
   }
}
