/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit;

import java.io.*;
import java.util.*;
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

      <T1 extends Baz, T2 extends List<? extends Number>> Pair<T1, T2> returnTypeWithMultipleTypeVariables()
      { return null; }

      Callable<Baz> returnGenericTypeWithTypeArgument() { return null; }
      Bar bar() { return null; }
   }

   @SuppressWarnings("unused")
   public interface Pair<K, V> {}

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

      Pair<Baz, List<Integer>> x = foo.returnTypeWithMultipleTypeVariables();
      assertNotNull(x);
   }

   @Test
   public void cascadeOneLevelDuringRecord(@Mocked Callable<String> action, @Mocked Foo mockFoo)
   {
      Foo foo = new Foo();
      Callable<?> cascaded = foo.returnTypeWithWildcard();

      assertSame(action, cascaded);
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

   @SuppressWarnings("unused") static class C<T> {}
   static class D extends C<Foo> { <T extends Bar> T doSomething() { return null; } }

   @Test
   public void cascadeFromGenericMethodUsingTypeParameterOfSameNameAsTypeParameterFromBaseClass(@Mocked D mock)
   {
      Bar cascaded = mock.doSomething();

      assertNotNull(cascaded);
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
      new Expectations() {{ cascadedBaz.getDate(); result = date; }};

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

   public interface GenericInterfaceWithBoundedTypeParameter<B extends Serializable> { B get(int id); }

   @Test
   public <T extends Serializable> void cascadeFromMethodReturningATypeVariable(
      @Mocked final GenericInterfaceWithBoundedTypeParameter<T> mock)
   {
      new Expectations() {{
         mock.get(1); result = "test";
         mock.get(2); result = null;
      }};

      assertEquals("test", mock.get(1));
      assertNull(mock.get(2));
   }

   static class TypeWithUnusedTypeParameterInGenericMethod { @SuppressWarnings("unused") <U> Foo foo() {return null;} }

   @Test
   public void cascadeFromMethodHavingUnusedTypeParameter(@Mocked TypeWithUnusedTypeParameterInGenericMethod mock)
   {
      Foo foo = mock.foo();
      Bar bar = foo.bar();
      assertNotNull(bar);
   }

   @Test
   public void cascadeFromGenericMethodWhoseReturnTypeResolvesToAnotherGenericType(@Mocked B<C<?>> mock)
   {
      C<?> c = mock.getValue();

      assertNotNull(c);
   }

   public interface BaseGenericInterface<B> { B genericMethod(); }
   public interface GenericSubInterface<S> extends BaseGenericInterface<S> {}
   public interface NonGenericInterface extends GenericSubInterface<Bar> {}

   @Test
   public void cascadeFromGenericMethodDefinedTwoLevelsDeepInInheritanceHierarchy(@Mocked NonGenericInterface mock)
   {
      Bar cascadedResult = mock.genericMethod();

      assertNotNull(cascadedResult);
   }

   interface NonPublicInterfaceWithGenericMethod { <T extends Runnable> T doSomething(); }

   @Test
   public void cascadeFromGenericMethodOfNonPublicInterface(@Mocked NonPublicInterfaceWithGenericMethod mock) {
      Runnable result = mock.doSomething();

      assertNotNull(result);
   }

   public interface FactoryInterface { <T> T genericWithClass(Class<T> type); }

   @Test
   public void cascadeFromGenericMethodWithClassParameterOfMockedInterface(@Mocked FactoryInterface mock)
   {
      Foo cascaded = mock.genericWithClass(Foo.class);

      assertNotNull(cascaded);
   }

   @SuppressWarnings("unused") static class Outer<T> { class Inner {} }
   static class Client { Outer<String>.Inner doSomething() { return null; } }

   @Test
   public void cascadeFromMethodReturningInnerInstanceOfGenericClass(@Mocked final Client mock)
   {
      final Outer<?>.Inner innerInstance = new Outer().new Inner();

      new Expectations() {{
         mock.doSomething();
         result = innerInstance;
      }};

      assertSame(innerInstance, mock.doSomething());
   }

   static class SubB<T> extends B<T> {}
   static class ClassWithMethodReturningGenericClassInstance { SubB<C<?>> doSomething() { return null; } }

   @Test
   public void cascadeFromMethodReturningInstanceOfGenericSubclassThenFromGenericMethodOfGenericBaseClass(
      @Mocked ClassWithMethodReturningGenericClassInstance mock)
   {
      SubB<C<?>> cascade1 = mock.doSomething();
      C<?> cascade2 = cascade1.getValue();

      assertNotNull(cascade2);
   }

   public interface InterfaceWithGenericMethod<T> { @SuppressWarnings("unused") T genericMethod(); }
   static class BaseClass { public Bar genericMethod() { return null; } }
   static class SubClass extends BaseClass implements InterfaceWithGenericMethod<Bar> {}

   @Test
   public void cascadeFromGenericInterfaceMethodImplementedInBaseClassOfMockedSubClass(@Mocked SubClass mock)
   {
      Bar cascaded = mock.genericMethod();
      assertNotNull(cascaded);
   }
}
