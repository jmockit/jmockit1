/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit;

import java.lang.reflect.*;
import javax.annotation.*;
import static java.lang.reflect.Modifier.*;

import mockit.internal.classGeneration.*;
import mockit.internal.faking.*;
import mockit.internal.startup.*;
import mockit.internal.state.*;

/**
 * A base class used in the creation of a <em>fake</em> for an <em>external</em> type, which is usually a class from
 * some library or component used from the <em>internal</em> codebase of the system under test (SUT).
 * Such fake classes can be used as <em>fake implementations</em> for use in unit or integration tests.
 * For example:
 * <pre>
 * public final class FakeSystem <strong>extends MockUp&lt;System></strong> {
 *    <strong>&#64;Mock</strong> public static long nanoTime() { return 123L; }
 * }
 * </pre>
 * One or more <em>fake methods</em> annotated {@linkplain Mock as such} must be defined in the concrete subclass.
 * Each <tt>@Mock</tt> method should have a matching method or constructor in the faked class.
 * At runtime, the execution of a faked method/constructor will get redirected to the corresponding fake method.
 * <p/>
 * When the type to be faked is specified indirectly through a {@linkplain TypeVariable type variable}, then that type
 * is taken as a <em>base</em> type whose concrete implementation classes should <em>also</em> get faked.
 * Example:
 * <pre>
 * &#64;Test
 * public &lt;<strong>BC extends SomeBaseClass</strong>> void someTest() {
 *     new MockUp&lt;<strong>BC</strong>>() {
 *        &#64;Mock int someMethod(int i) { return i + 1; }
 *     };
 *
 *     int i = new AConcreteSubclass().someMethod(1);
 *     assertEquals(2, i);
 * }
 * </pre>
 *
 * @param <T> specifies the type to be faked; if a type variable is used, then all implementation classes extending or
 *           implementing that base type are also faked; if the type argument itself is a parameterized type, then only
 *           its raw type is considered
 *
 * @see #MockUp()
 * @see #MockUp(Class)
 * @see #onTearDown()
 * @see #targetType
 * @see <a href="http://jmockit.org/tutorial/Faking.html#setUp" target="tutorial">Tutorial</a>
 */
public abstract class MockUp<T>
{
   static { Startup.verifyInitialization(); }

   /**
    * Holds the class or generic type targeted by this fake instance.
    */
   protected final Type targetType;

   /**
    * Applies the {@linkplain Mock fake methods} defined in the concrete subclass to the class specified through the
    * type parameter.
    */
   protected MockUp()
   {
      MockUp<?> previousFake = findPreviouslyFakedClassIfFakeAlreadyApplied();

      if (previousFake != null) {
         targetType = null;
         return;
      }

      targetType = getTypeToFake();
      Class<T> classToFake = null;

      if (targetType instanceof Class<?>) {
         //noinspection unchecked
         classToFake = (Class<T>) targetType;
      }
      else if (targetType instanceof ParameterizedType) {
         ParameterizedType parameterizedType = (ParameterizedType) targetType;
         //noinspection unchecked
         classToFake = (Class<T>) parameterizedType.getRawType();
      }

      if (classToFake != null) {
         redefineClass(classToFake);
      }
      else {
         Type[] typesToFake = ((TypeVariable<?>) targetType).getBounds();

         if (typesToFake.length == 1) {
            new CaptureOfFakedImplementations(this, typesToFake[0]).apply();
         }
         else {
            throw new UnsupportedOperationException("Unable to capture more than one base type at once");
         }
      }
   }

   @Nullable
   private MockUp<?> findPreviouslyFakedClassIfFakeAlreadyApplied()
   {
      FakeClasses fakeClasses = TestRun.getFakeClasses();
      MockUp<?> previousFake = fakeClasses.findPreviouslyAppliedFake(this);
      return previousFake;
   }

   @Nonnull
   private Type getTypeToFake()
   {
      Class<?> currentClass = getClass();

      do {
         Type superclass = currentClass.getGenericSuperclass();

         if (superclass instanceof ParameterizedType) {
            return ((ParameterizedType) superclass).getActualTypeArguments()[0];
         }

         if (superclass == MockUp.class) {
            throw new IllegalArgumentException("No target type");
         }

         currentClass = (Class<?>) superclass;
      }
      while (true);
   }

   private void redefineClass(@Nonnull Class<T> classToFake)
   {
      if (!classToFake.isInterface()) {
         Class<T> realClass = classToFake;

         if (isAbstract(classToFake.getModifiers())) {
            classToFake = new ConcreteSubclass<T>(classToFake).generateClass();
         }

         redefineMethods(realClass, classToFake, targetType);
      }
   }

   private void redefineMethods(
      @Nonnull Class<T> realClass, @Nonnull Class<T> classToFake, @Nullable Type genericFakedType)
   {
      FakeClassSetup fakeSetup = new FakeClassSetup(realClass, classToFake, genericFakedType, this);
      fakeSetup.redefineMethods();
   }

   /**
    * Applies the {@linkplain Mock fake methods} defined in the fake class to the given class.
    * <p/>
    * In most cases, the {@linkplain #MockUp() constructor with no parameters} can be used.
    * This variation is useful when the type to be faked is not known at compile time. For example, it can be used with
    * an {@linkplain Mock $advice} method and the <tt>fakes</tt> system property in order to have an aspect-like fake
    * implementation applicable to any class; it can then be applied at the beginning of the test run with the desired
    * target class being specified in the test run configuration.
    */
   protected MockUp(@SuppressWarnings("NullableProblems") Class<?> targetClass)
   {
      targetType = targetClass;
      MockUp<?> previousFake = findPreviouslyFakedClassIfFakeAlreadyApplied();

      if (previousFake != null) {
         return;
      }

      if (!targetClass.isInterface()) {
         //noinspection unchecked
         Class<T> realClass = (Class<T>) targetClass;
         redefineMethods(realClass, realClass, null);
      }
   }

   /**
    * An empty method that can be overridden in a fake class that wants to be notified whenever the fake is
    * automatically torn down.
    * Tear down happens when the fake goes out of scope: at the end of the test when applied inside a test, at the end
    * of the test class when applied before the test class, or at the end of the test run when applied through the
    * "<code>fakes</code>" system property.
    * <p/>
    * By default, this method does nothing.
    */
   protected void onTearDown() {}
}
