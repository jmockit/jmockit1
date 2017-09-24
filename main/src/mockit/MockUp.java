/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit;

import java.lang.reflect.*;
import java.util.*;
import javax.annotation.*;
import static java.lang.reflect.Modifier.*;

import mockit.internal.classGeneration.*;
import mockit.internal.faking.*;
import mockit.internal.reflection.*;
import mockit.internal.startup.*;
import mockit.internal.faking.FakeClasses.*;
import mockit.internal.state.*;
import static mockit.internal.util.GeneratedClasses.*;

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
 * Each {@code @Mock} method should have a matching method or constructor in the faked class/interface.
 * At runtime, the execution of a faked method/constructor will get redirected to the corresponding fake method.
 * <p/>
 * When the faked type is an interface, an implementation class is generated where all methods are empty, with non-void
 * methods returning a default value according to the return type: {@code 0} for {@code int}, {@code null} for a
 * reference type, and so on.
 * In this case, an instance of the generated implementation class should be obtained by calling
 * {@link #getMockInstance()}.
 * <p/>
 * When the type to be faked is specified indirectly through a {@linkplain TypeVariable type variable}, there are two
 * other possible outcomes:
 * <ol>
 * <li>If the type variable "<code>extends</code>" two or more interfaces, a fake proxy class that implements all
 * interfaces is created, with the proxy instance made available through a call to {@link #getMockInstance()}.
 * Example:
 * <pre>
 * &#64;Test
 * public &lt;<strong>M extends Runnable & ResultSet</strong>> void someTest() {
 *     M fakedInterfaceInstance = new MockUp&lt;<strong>M</strong>>() {
 *        &#64;Mock void run() { ...do something... }
 *        &#64;Mock boolean next() { return true; }
 *     }.getMockInstance();
 *
 *     fakedInterfaceInstance.run();
 *     assertTrue(fakedInterfaceInstance.next());
 * }
 * </pre>
 * </li>
 * <li>If the type variable extends a <em>single</em> type (either an interface or a non-<code>final</code> class), then
 * that type is taken as a <em>base</em> type whose concrete implementation classes should <em>also</em> get faked.
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
 * </li>
 * </ol>
 *
 * @param <T> specifies the type (class, interface, etc.) to be faked; multiple interfaces can be faked by defining a
 * <em>type variable</em> in the test class or test method, and using it as the type argument;
 * if a type variable is used and it extends a <em>single</em> type, then all implementation classes extending or
*  implementing that base type are also faked;
 * if the type argument itself is a parameterized type, then only its raw type is considered
 *
 * @see #MockUp()
 * @see #MockUp(Class)
 * @see #getMockInstance()
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

   @Nullable private final Class<?> fakedClass;
   @Nullable private Set<Class<?>> classesToRestore;
   @Nullable private T fakeInstance;
   @SuppressWarnings("unused") @Nullable private T invokedInstance; // set through Reflection elsewhere

   /**
    * Applies the {@linkplain Mock fake methods} defined in the concrete subclass to the class or interface specified
    * through the type parameter.
    *
    * @see #MockUp(Class)
    */
   protected MockUp()
   {
      MockUp<?> previousFake = findPreviouslyFakedClassIfFakeAlreadyApplied();

      if (previousFake != null) {
         targetType = previousFake.targetType;
         fakedClass = previousFake.fakedClass;
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
         fakedClass = redefineClassOrImplementInterface(classToFake);
      }
      else {
         Type[] typesToFake = ((TypeVariable<?>) targetType).getBounds();

         fakedClass = typesToFake.length > 1 ?
            new FakedImplementationClass<T>(this).createImplementation(typesToFake) :
            new CaptureOfFakedImplementations(this, typesToFake[0]).apply();
      }
   }

   @Nullable
   private MockUp<?> findPreviouslyFakedClassIfFakeAlreadyApplied()
   {
      FakeClasses fakeClasses = TestRun.getFakeClasses();
      FakeInstances fakeInstances = fakeClasses.findPreviouslyAppliedFakes(this);

      if (fakeInstances != null) {
         return fakeInstances.initialFake;
      }

      return null;
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

   @Nonnull
   private Class<?> redefineClassOrImplementInterface(@Nonnull Class<T> classToFake)
   {
      if (classToFake.isInterface()) {
         return createInstanceOfFakedImplementationClass(classToFake, targetType);
      }

      Class<T> realClass = classToFake;

      if (isAbstract(classToFake.getModifiers())) {
         classToFake = new ConcreteSubclass<T>(classToFake).generateClass();
      }

      classesToRestore = redefineMethods(realClass, classToFake, targetType);
      return classToFake;
   }

   @Nonnull
   private Class<T> createInstanceOfFakedImplementationClass(@Nonnull Class<T> classToFake, @Nullable Type typeToFake)
   {
      FakedImplementationClass<T> fakedImplementationClass = new FakedImplementationClass<T>(this);
      return fakedImplementationClass.createImplementation(classToFake, typeToFake);
   }

   @Nonnull
   private Set<Class<?>> redefineMethods(
      @Nonnull Class<T> realClass, @Nonnull Class<T> classToFake, @Nullable Type genericFakedType)
   {
      FakeClassSetup fakeSetup = new FakeClassSetup(realClass, classToFake, genericFakedType, this);
      return fakeSetup.redefineMethods();
   }

   /**
    * Applies the {@linkplain Mock fake methods} defined in the fake class to the given class/interface.
    * <p/>
    * In most cases, the constructor with no parameters can be used.
    * This variation should be used only when the type to be faked is not accessible or known from the test code.
    *
    * @see #MockUp()
    */
   protected MockUp(@SuppressWarnings("NullableProblems") Class<?> targetClass)
   {
      targetType = targetClass;
      MockUp<?> previousFake = findPreviouslyFakedClassIfFakeAlreadyApplied();

      if (previousFake != null) {
         fakedClass = previousFake.fakedClass;
         return;
      }

      if (targetClass.isInterface()) {
         //noinspection unchecked
         fakedClass = createInstanceOfFakedImplementationClass((Class<T>) targetClass, targetClass);
      }
      else {
         fakedClass = targetClass;
         //noinspection unchecked
         Class<T> realClass = (Class<T>) targetClass;
         classesToRestore = redefineMethods(realClass, realClass, null);
         fakeInstance = null;
      }
   }

   /**
    * Returns the mock instance exclusively associated with this fake instance.
    * If the faked type was an interface, then said instance is the one that was automatically created when the fake was
    * applied.
    * If it was a class, and no such instance is currently associated with this (stateful) fake object, then a new
    * <em>uninitialized</em> instance of the faked class is created and returned, becoming associated with the fake.
    * <p/>
    * In any case, for a given fake instance this method will always return the same fake instance.
    *
    * @see <a href="http://jmockit.org/tutorial/Faking.html#interfaces" target="tutorial">Tutorial</a>
    */
   public final T getMockInstance()
   {
      if (invokedInstance == Void.class) {
         return null;
      }

      if (invokedInstance != null) {
         return invokedInstance;
      }

      if (fakeInstance == null && fakedClass != null) {
         @SuppressWarnings("unchecked") T newInstance = (T) createFakeInstance(fakedClass);
         fakeInstance = newInstance;
      }

      //noinspection ConstantConditions
      return fakeInstance;
   }

   @Nonnull
   private Object createFakeInstance(@Nonnull Class<?> fakedClass)
   {
      String fakedClassName = fakedClass.getName();

      if (isGeneratedImplementationClass(fakedClassName)) {
         return ConstructorReflection.newInstanceUsingPublicDefaultConstructor(fakedClass);
      }

      if (Proxy.isProxyClass(fakedClass)) {
         return MockInvocationHandler.newMockedInstance(fakedClass);
      }

      return ConstructorReflection.newUninitializedInstance(fakedClass);
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
