/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit;

import java.lang.annotation.*;

import javax.annotation.*;

import org.junit.*;

import static org.junit.Assert.*;

public final class MockedAnnotationsTest
{
   public @interface MyAnnotation
   {
      String value();
      boolean flag() default true;
      String[] values() default {};
   }

   @Test
   public void specifyValuesForAnnotationAttributes(@Mocked final MyAnnotation a)
   {
      assertSame(MyAnnotation.class, a.annotationType());

      new Expectations() {{
         a.flag(); result = false;
         a.value(); result = "test";
         a.values(); returns("abc", "dEf");
      }};

      assertFalse(a.flag());
      assertEquals("test", a.value());
      assertArrayEquals(new String[] {"abc", "dEf"}, a.values());
   }

   @Test
   public void verifyUsesOfAnnotationAttributes(@Mocked final MyAnnotation a)
   {
      new Expectations() {{
         a.value(); result = "test"; times = 2;
         a.values(); returns("abc", "dEf");
      }};

      // Same rule for regular methods applies (ie, if no return value was recorded, invocations
      // will get the default for the return type).
      assertFalse(a.flag());

      assertEquals("test", a.value());
      assertArrayEquals(new String[] {"abc", "dEf"}, a.values());
      a.value();

      new FullVerifications() {{
         // Mocked methods called here always return the default value according to return type.
         a.flag();
      }};
   }

   @Resource
   public interface AnInterface {}

   @Test
   public void mockingAnAnnotatedPublicInterface(@Mocked AnInterface mock)
   {
      Annotation[] mockClassAnnotations = mock.getClass().getAnnotations();

      assertEquals(0, mockClassAnnotations.length);
   }

   static class ClassWithNullabilityAnnotations
   {
      @Nonnull String doSomething(@Nonnegative int i, @Nonnull Object obj) { return ""; }
   }

   @Test
   public void mockClassWithNullabilityAnnotations(@Injectable final ClassWithNullabilityAnnotations mock)
   {
      new Expectations() {{ mock.doSomething(anyInt, any); result = "test"; }};

      assertEquals("test", mock.doSomething(123, "test"));
   }
}