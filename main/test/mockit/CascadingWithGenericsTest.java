/*
 * Copyright (c) 2006-2015 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit;

import java.util.*;
import java.util.Map.*;
import java.util.concurrent.*;

import org.junit.*;
import org.junit.runners.*;
import static org.junit.Assert.*;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public final class CascadingWithGenericsTest
{
   static class Foo
   {
      Callable<?> returnTypeWithWildcard() { return null; }
      <RT extends Baz> RT returnTypeWithBoundedTypeVariable() { return null; }

      @SuppressWarnings("UnusedParameters")
      <N extends Number> N genericMethodWithNonMockableBoundedTypeVariableAndClassParameter(Class<N> c) { return null; }

      @SuppressWarnings("UnusedParameters")
      <RT extends Bar> RT genericMethodWithBoundedTypeVariableAndClassParameter(Class<RT> cl) { return null; }

      <T1 extends Baz, T2 extends List<? extends Number>> Entry<T1, T2> returnTypeWithMultipleTypeVariables()
      { return null; }

      Callable<Baz> returnGenericTypeWithTypeArgument() { return null; }
   }

   static class Bar
   {
      Bar() { throw new RuntimeException(); }
      int doSomething() { return 1; }
      static String staticMethod() { return "notMocked"; }
   }

   static final class SubBar extends Bar {}

   public interface Baz { Date getDate(); }

   @Test
   public void cascadeOneLevelDuringReplay(@Mocked Foo foo)
   {
      assertNotNull(foo.returnTypeWithWildcard());
      assertNotNull(foo.returnTypeWithBoundedTypeVariable());

      Entry<Baz, List<Integer>> x = foo.returnTypeWithMultipleTypeVariables();
      assertNotNull(x);
   }

   @Test
   public void cascadeOneLevelDuringRecord(@Mocked final Callable<String> action, @Mocked final Foo mockFoo)
   {
      new Expectations() {{
         mockFoo.returnTypeWithWildcard(); result = action;
      }};

      Foo foo = new Foo();
      assertSame(action, foo.returnTypeWithWildcard());
   }

   @Test
   public void cascadeTwoLevelsDuringRecord(@Mocked final Foo mockFoo)
   {
      final Date now = new Date();

      new Expectations() {{
         mockFoo.returnTypeWithBoundedTypeVariable().getDate(); result = now;
      }};

      Foo foo = new Foo();
      assertSame(now, foo.returnTypeWithBoundedTypeVariable().getDate());
   }

   static class GenericFoo<T, U extends Bar>
   {
      T returnTypeWithUnboundedTypeVariable() { return null; }
      U returnTypeWithBoundedTypeVariable() { return null; }
   }

   @Test
   public void cascadeGenericMethods(@Mocked GenericFoo<Baz, SubBar> foo)
   {
      Baz t = foo.returnTypeWithUnboundedTypeVariable();
      assertNotNull(t);

      SubBar u = foo.returnTypeWithBoundedTypeVariable();
      assertNotNull(u);
   }

   static class A { B<?> getB() { return null; } }
   static class B<T> { T getValue() { return null; } }

   @Test
   public void cascadeOnMethodReturningAParameterizedClassWithAGenericMethod(@Injectable final A a)
   {
      new Expectations() {{
         a.getB().getValue(); result = "test";
      }};

      assertEquals("test", a.getB().getValue());
   }

   static class Factory
   {
      static <T extends Bar> T bar() { return null; }
      static <T extends Bar> T bar(@SuppressWarnings("UnusedParameters") Class<T> c) { return null; }
      WithStaticInit staticInit() { return null; }
   }
   static class WithStaticInit
   {
      static final Bar T = Factory.bar();
      static final SubBar S = Factory.bar(SubBar.class);
   }

   @Test
   public void cascadeDuringStaticInitializationOfCascadedClass(@Mocked Factory mock)
   {
      assertNotNull(mock.staticInit());
      assertNotNull(WithStaticInit.T);
      assertNotNull(WithStaticInit.S);
   }

   @Test
   public void cascadeFromGenericMethodWhereConcreteReturnTypeIsGivenByClassParameterButIsNotMockable(@Mocked Foo foo)
   {
      Integer n = foo.genericMethodWithNonMockableBoundedTypeVariableAndClassParameter(Integer.class);

      assertNotNull(n);
   }

   @Test
   public void cascadeFromGenericMethodWhereConcreteReturnTypeIsGivenByClassParameter(@Mocked Foo foo)
   {
      SubBar subBar = foo.genericMethodWithBoundedTypeVariableAndClassParameter(SubBar.class);

      assertNotNull(subBar);
   }

   @Test
   public void cascadeFromGenericMethodWhoseReturnTypeComesFromParameterOnOwnerType(
      @Mocked Foo foo, @Mocked final Baz cascadedBaz) throws Exception
   {
      final Date date = new Date();

      new Expectations() {{
         cascadedBaz.getDate(); result = date;
      }};

      Callable<Baz> callable = foo.returnGenericTypeWithTypeArgument();
      Baz baz = callable.call();

      assertSame(cascadedBaz, baz);
      assertSame(date, baz.getDate());
   }

   public interface GenericInterface<T> { <S extends T> S save(S entity); }
   public interface ConcreteInterface extends GenericInterface<Foo> {}

   @Test
   public void cascadingFromGenericMethodWhoseTypeParameterExtendsAnother(@Mocked ConcreteInterface mock)
   {
      Foo value = new Foo();

      Foo saved = mock.save(value);

      assertNotNull(saved);
      assertNotSame(value, saved);
   }
}
