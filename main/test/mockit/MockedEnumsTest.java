/*
 * Copyright (c) 2006-2014 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit;

import java.lang.annotation.*;
import java.util.concurrent.*;

import org.junit.*;
import static org.junit.Assert.*;

import mockit.internal.*;

public final class MockedEnumsTest
{
   enum MyEnum
   {
      First(true, 10, "First"),
      Second(false, 6, "Second");

      private final boolean flag;
      private final int num;
      private final String desc;

      MyEnum(boolean flag, int num, String desc)
      {
         this.flag = flag;
         this.num = num;
         this.desc = desc;
      }

      public double getValue(double f) { return f * num; }
      public String getDescription() { return num + desc + flag; }
   }

   @Test
   public void oneEnumBeingMockedMustNotAffectOtherEnums(@Mocked MyEnum e)
   {
      assertNotNull(RetentionPolicy.valueOf("RUNTIME"));
   }

   @Test
   public void mockEnumValues(@Mocked final MyEnum mock)
   {
      new NonStrictExpectations() {{
         MyEnum.values(); result = new MyEnum[] {mock};
         mock.getValue(anyDouble); result = 50.0;
      }};

      MyEnum[] values = MyEnum.values();
      assertEquals(1, values.length);

      double value = values[0].getValue(0.5);
      assertEquals(50.0, value, 0.0);
   }

   @Test
   public void mockInstanceMethodOnAnyEnumElement(@Mocked final MyEnum mock)
   {
      final double f = 2.5;

      new NonStrictExpectations() {{
         mock.getValue(f); result = 12.3;
      }};

      assertEquals(12.3, MyEnum.First.getValue(f), 0.0);
      assertEquals(12.3, MyEnum.Second.getValue(f), 0.0);
   }

   @Test
   public void mockSpecificEnumElementsByUsingTwoMockInstances(@Mocked MyEnum mock1, @Mocked MyEnum mock2)
   {
      new NonStrictExpectations() {{
         MyEnum.First.getValue(anyDouble); result = 12.3;
         MyEnum.Second.getValue(anyDouble); result = -5.01;
      }};

      assertEquals(12.3, MyEnum.First.getValue(2.5), 0.0);
      assertEquals(-5.01, MyEnum.Second.getValue(1), 0.0);
   }

   @Test
   public void mockSpecificEnumElementsEvenWhenUsingASingleMockInstance(@Mocked MyEnum unused)
   {
      new NonStrictExpectations() {{
         MyEnum.First.getValue(anyDouble); result = 12.3;
         MyEnum.Second.getValue(anyDouble); result = -5.01;
      }};

      assertEquals(-5.01, MyEnum.Second.getValue(1), 0.0);
      assertEquals(12.3, MyEnum.First.getValue(2.5), 0.0);

      new Verifications() {{
         MyEnum.First.getValue(2.5);
         MyEnum.Second.getValue(1);
      }};
   }

   @Test(expected = UnexpectedInvocation.class)
   public void mockSpecificEnumElementsEvenWhenUsingASingleStrictMockInstance(@Mocked("getDescription") MyEnum unused)
   {
      new Expectations() {{
         MyEnum.First.getDescription();
         MyEnum.Second.getDescription();
      }};

      MyEnum.Second.getDescription();
   }
   
   @Test
   public void mockNonAbstractMethodsInEnumWithAbstractMethod(@Mocked final TimeUnit tm) throws Exception
   {
      new Expectations() {{
         tm.convert(anyLong, TimeUnit.SECONDS); result = 1L;
         tm.sleep(anyLong);
      }};

      assertEquals(1, tm.convert(1000, TimeUnit.SECONDS));
      tm.sleep(10000);
   }

   public enum EnumWithValueSpecificMethods
   {
      One
      {
         @Override public int getValue() { return 1; }
         @Override public String getDescription() { return "one"; }
      },
      Two
      {
         @Override public int getValue() { return 2; }
         @Override public String getDescription() { return "two"; }
      };

      public abstract int getValue();
      public String getDescription() { return String.valueOf(getValue()); }
   }

   @Test
   public void mockEnumWithValueSpecificMethods(@Mocked EnumWithValueSpecificMethods mockedEnum)
   {
      assertSame(EnumWithValueSpecificMethods.One, mockedEnum);

      new NonStrictExpectations() {{
         EnumWithValueSpecificMethods.One.getValue(); result = 123;
         EnumWithValueSpecificMethods.Two.getValue(); result = -45;

         EnumWithValueSpecificMethods.One.getDescription(); result = "1";
         EnumWithValueSpecificMethods.Two.getDescription(); result = "2";
      }};

      assertEquals(123, EnumWithValueSpecificMethods.One.getValue());
      assertEquals(-45, EnumWithValueSpecificMethods.Two.getValue());
      assertEquals("1", EnumWithValueSpecificMethods.One.getDescription());
      assertEquals("2", EnumWithValueSpecificMethods.Two.getDescription());
   }
}
