/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit;

import java.util.*;

import org.junit.*;
import static org.junit.Assert.*;

public final class FakeForGenericsTest
{
   public static final class Collaborator
   {
      public <N extends Number> N genericMethod(@SuppressWarnings("UnusedParameters") N n) { return null; }
   }

   @Test
   public void fakeGenericMethod()
   {
      new MockUp<Collaborator>() {
         @Mock <T extends Number> T genericMethod(T t) { return t; }

         // This also works (same erasure):
         // @Mock Number genericMethod(Number t) { return t; }
      };

      Integer n = new Collaborator().genericMethod(123);
      assertEquals(123, n.intValue());

      Long l = new Collaborator().genericMethod(45L);
      assertEquals(45L, l.longValue());

      Short s = new Collaborator().genericMethod((short) 6);
      assertEquals(6, s.shortValue());

      Double d = new Collaborator().genericMethod(0.5);
      assertEquals(0.5, d, 0);
   }

   @SuppressWarnings("UnusedParameters")
   public static final class GenericClass<T1, T2>
   {
      public void aMethod(T1 t) { throw new RuntimeException("t=" + t); }
      public int anotherMethod(T1 t, int i, T2 p) { return 2 * i; }
      public int anotherMethod(Integer t, int i, String p) { return -2 * i; }
   }

   @Test
   public void fakeGenericClassWithUnspecifiedTypeArguments()
   {
      new MockUp<GenericClass<?, ?>>() {
         @Mock
         void aMethod(Object o)
         {
            StringBuilder s = (StringBuilder) o;
            s.setLength(0);
            s.append("fake");
            s.toString();
         }

         @Mock
         int anotherMethod(Object o, int i, Object list)
         {
            assertTrue(o instanceof StringBuilder);
            //noinspection unchecked
            assertEquals(0, ((Collection<String>) list).size());
            return -i;
         }
      };

      StringBuilder s = new StringBuilder("test");
      GenericClass<StringBuilder, List<String>> g = new GenericClass<StringBuilder, List<String>>();

      g.aMethod(s);
      int r1 = g.anotherMethod(new StringBuilder("test"), 58, Collections.<String>emptyList());
      int r2 = g.anotherMethod(123, 65, "abc");

      assertEquals("fake", s.toString());
      assertEquals(-58, r1);
      assertEquals(-130, r2);
   }

   @Test
   public void fakeBothGenericAndNonGenericMethodsInGenericClass()
   {
      new MockUp<GenericClass<String, Boolean>>() {
         @Mock int anotherMethod(Integer t, int i, String p) { return 2; }
         @Mock int anotherMethod(String t, int i, Boolean p) { return 1; }
      };

      GenericClass<String, Boolean> o = new GenericClass<String, Boolean>();
      assertEquals(1, o.anotherMethod("generic", 1, true));
      assertEquals(2, o.anotherMethod(123, 2, "non generic"));
   }

   static class GenericBaseClass<T, U> { public U find(@SuppressWarnings("UnusedParameters") T id) { return null; } }

   @Test
   public void fakeGenericMethodWithFakeMethodHavingParameterTypesMatchingTypeArguments()
   {
      new MockUp<GenericBaseClass<String, Integer>>() {
         @Mock
         Integer find(String id) { return id.hashCode(); }
      };

      int i = new GenericBaseClass<String, Integer>().find("test");
      assertEquals("test".hashCode(), i);
   }

   @Test
   public void cannotCallGenericMethodWhenSomeFakeMethodExpectsDifferentTypes()
   {
      new MockUp<GenericBaseClass<String, Integer>>() { @Mock Integer find(String id) { return 1; } };

      try {
         new GenericBaseClass<Integer, String>().find(1);
         fail();
      }
      catch (IllegalArgumentException e) {
         assertTrue(e.getMessage().startsWith("Failure to invoke method: "));
      }
   }

   static class NonGenericSuperclass extends GenericBaseClass<Integer, String> {}
   final class NonGenericSubclass extends NonGenericSuperclass {}

   @Test
   public void fakeGenericMethodFromInstantiationOfNonGenericSubclass()
   {
      new MockUp<NonGenericSubclass>() {
         @Mock
         String find(Integer id) { return "faked" + id; }
      };

      String s = new NonGenericSubclass().find(1);
      assertEquals("faked1", s);
   }

   static class GenericSuperclass<I> extends GenericBaseClass<I, String> {}
   final class AnotherNonGenericSubclass extends GenericSuperclass<Integer> {}

   @Test
   public void fakeGenericMethodFromInstantiationOfNonGenericSubclassWhichExtendsAGenericIntermediateSuperclass()
   {
      new MockUp<AnotherNonGenericSubclass>() {
         @Mock
         String find(Integer id) { return "faked" + id; }
      };

      String s = new AnotherNonGenericSubclass().find(1);
      assertEquals("faked1", s);
   }

   @SuppressWarnings("UnusedParameters")
   public static class NonGenericClassWithGenericMethods
   {
      public static <T> T staticMethod(Class<T> cls, String s) { throw new RuntimeException(); }
      public <C> void instanceMethod(Class<C> cls, String s) { throw new RuntimeException(); }
      public final <N extends Number> void instanceMethod(Class<N> cls) { throw new RuntimeException(); }
   }

   @Test
   public void fakeGenericMethodsOfNonGenericClass()
   {
      new MockUp<NonGenericClassWithGenericMethods>() {
         @Mock <T> T staticMethod(Class<T> cls, String s) { return null; }
         @Mock <C> void instanceMethod(Class<C> cls, String s) {}
         @Mock void instanceMethod(Class<?> cls) {}
      };

      new NonGenericClassWithGenericMethods().instanceMethod(Integer.class);
      NonGenericClassWithGenericMethods.staticMethod(Collaborator.class, "test1");
      new NonGenericClassWithGenericMethods().instanceMethod(Byte.class, "test2");
   }
}
