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
import mockit.internal.mockups.*;
import mockit.internal.startup.*;
import mockit.internal.state.MockClasses.*;
import mockit.internal.state.*;
import mockit.internal.util.*;
import static mockit.internal.util.GeneratedClasses.*;

/**
 * A base class used in the creation of a <em>mock-up</em> for an <em>external</em> type, which is usually a class from
 * some library or component used from the <em>internal</em> codebase of the system under test (SUT).
 * Such mock-ups can be used as <em>fake</em> implementations for use in unit or integration tests.
 * For example:
 * <pre>
 * public final class FakeSystem <strong>extends MockUp&lt;System></strong> {
 *    <strong>&#64;Mock</strong> public static long nanoTime() { return 123L; }
 * }
 * </pre>
 * One or more <em>mock methods</em> annotated {@linkplain Mock as such} must be defined in the concrete subclass.
 * Each {@code @Mock} method should have a matching method or constructor in the faked class/interface.
 * At runtime, the execution of a faked method/constructor will get redirected to the corresponding mock method.
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
 * <li>If the type variable "<code>extends</code>" two or more interfaces, a mock proxy class that implements all
 * interfaces is created, with the proxy instance made available through a call to {@link #getMockInstance()}.
 * Example:
 * <pre>
 * &#64;Test
 * public &lt;<strong>M extends Runnable & ResultSet</strong>> void someTest() {
 *     M mock = new MockUp&lt;<strong>M</strong>>() {
 *        &#64;Mock void run() { ...do something... }
 *        &#64;Mock boolean next() { return true; }
 *     }.getMockInstance();
 *
 *     mock.run();
 *     assertTrue(mock.next());
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
 * @see #MockUp(Object)
 * @see #getMockInstance()
 * @see #onTearDown()
 * @see #targetType
 * @see <a href="http://jmockit.org/tutorial/Faking.html#setUp">Tutorial</a>
 */
public abstract class MockUp<T>
{
   static { Startup.verifyInitialization(); }

   /**
    * Holds the class or generic type targeted by this mock-up instance.
    */
   protected final Type targetType;

   @Nullable private final Class<?> mockedClass;
   @Nullable private Set<Class<?>> classesToRestore;
   @Nullable private T mockInstance;
   @Nullable private T invokedInstance;

   /**
    * Applies the {@linkplain Mock mock methods} defined in the concrete subclass to the class or interface specified
    * through the type parameter.
    *
    * @see #MockUp(Class)
    * @see #MockUp(Object)
    */
   protected MockUp()
   {
      MockUp<?> previousMockUp = findPreviouslyFakedClassIfMockUpAlreadyApplied();

      if (previousMockUp != null) {
         targetType = previousMockUp.targetType;
         mockedClass = previousMockUp.mockedClass;
         return;
      }

      targetType = getTypeToFake();
      Class<T> classToMock = null;

      if (targetType instanceof Class<?>) {
         //noinspection unchecked
         classToMock = (Class<T>) targetType;
      }
      else if (targetType instanceof ParameterizedType) {
         ParameterizedType parameterizedType = (ParameterizedType) targetType;
         //noinspection unchecked
         classToMock = (Class<T>) parameterizedType.getRawType();
      }

      if (classToMock != null) {
         mockedClass = redefineClassOrImplementInterface(classToMock);
      }
      else {
         Type[] typesToMock = ((TypeVariable<?>) targetType).getBounds();

         mockedClass = typesToMock.length > 1 ?
            new MockedImplementationClass<T>(this).createImplementation(typesToMock) :
            new CaptureOfMockedUpImplementations(this, typesToMock[0]).apply();
      }
   }

   @Nullable
   private MockUp<?> findPreviouslyFakedClassIfMockUpAlreadyApplied()
   {
      MockClasses mockClasses = TestRun.getMockClasses();
      MockUpInstances mockUpInstances = mockClasses.findPreviouslyAppliedMockUps(this);

      if (mockUpInstances != null && mockUpInstances.hasMockUpsForSingleInstances()) {
         return mockUpInstances.initialMockUp;
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
   private Class<?> redefineClassOrImplementInterface(@Nonnull Class<T> classToMock)
   {
      if (classToMock.isInterface()) {
         return createInstanceOfMockedImplementationClass(classToMock, targetType);
      }

      Class<T> realClass = classToMock;

      if (isAbstract(classToMock.getModifiers())) {
         classToMock = new ConcreteSubclass<T>(classToMock).generateClass();
      }

      classesToRestore = redefineMethods(realClass, classToMock, targetType);
      return classToMock;
   }

   @Nonnull
   private Class<T> createInstanceOfMockedImplementationClass(@Nonnull Class<T> classToMock, @Nullable Type typeToMock)
   {
      return new MockedImplementationClass<T>(this).createImplementation(classToMock, typeToMock);
   }

   @Nullable
   private Set<Class<?>> redefineMethods(
      @Nonnull Class<T> realClass, @Nonnull Class<T> classToMock, @Nullable Type genericMockedType)
   {
      return new MockClassSetup(realClass, classToMock, genericMockedType, this).redefineMethods();
   }

   /**
    * Applies the {@linkplain Mock mock methods} defined in the mock-up subclass to the given class/interface.
    * <p/>
    * In most cases, the constructor with no parameters can be used.
    * This variation should be used only when the type to be faked is not accessible or known from the test code.
    *
    * @see #MockUp()
    * @see #MockUp(Object)
    */
   protected MockUp(@SuppressWarnings("NullableProblems") Class<?> targetClass)
   {
      targetType = targetClass;
      MockUp<?> previousMockUp = findPreviouslyFakedClassIfMockUpAlreadyApplied();

      if (previousMockUp != null) {
         mockedClass = previousMockUp.mockedClass;
         return;
      }

      if (targetClass.isInterface()) {
         //noinspection unchecked
         mockedClass = createInstanceOfMockedImplementationClass((Class<T>) targetClass, targetClass);
      }
      else {
         mockedClass = targetClass;
         //noinspection unchecked
         Class<T> realClass = (Class<T>) targetClass;
         classesToRestore = redefineMethods(realClass, realClass, null);
         mockInstance = null;
      }
   }

   /**
    * Applies the {@linkplain Mock mock methods} defined in the mock-up subclass to the type specified through the type
    * parameter, but only affecting the given instance.
    * <p/>
    * In most cases, the constructor with no parameters should be adequate.
    * This variation can be used when mock data or behavior is desired only for a particular instance, with other
    * instances remaining unaffected; or when multiple mock-up objects carrying different states are desired, with one
    * mock-up instance per real instance.
    * <p/>
    * If {@link #getMockInstance()} later gets called on this mock-up instance, it will return the instance that was
    * given here.
    *
    * @param targetInstance a real instance of the type to be faked, meant to be the only one of that type that should
    * be affected by this mock-up instance
    *
    * @see #MockUp()
    * @see #MockUp(Class)
    */
   protected MockUp(T targetInstance)
   {
      MockUp<?> previousMockUp = findPreviouslyFakedClassIfMockUpAlreadyApplied();

      if (previousMockUp != null) {
         targetType = previousMockUp.targetType;
         mockedClass = previousMockUp.mockedClass;
         setMockInstance(targetInstance);
         return;
      }

      @SuppressWarnings("unchecked") Class<T> classToMock = (Class<T>) targetInstance.getClass();
      targetType = classToMock;
      mockedClass = classToMock;
      classesToRestore = redefineMethods(classToMock, classToMock, classToMock);

      setMockInstance(targetInstance);
   }

   private void setMockInstance(@Nonnull T mockInstance)
   {
      TestRun.getMockClasses().addMock(this, mockInstance);
      this.mockInstance = mockInstance;
   }

   /**
    * Returns the mock instance exclusively associated with this mock-up instance.
    * If the mocked type was an interface, then said instance is the one that was automatically created when the mock-up
    * was applied.
    * If it was a class, and no such instance is currently associated with this (stateful) mock-up object, then a new
    * <em>uninitialized</em> instance of the faked class is created and returned, becoming associated with the mock-up.
    * If a regular <em>initialized</em> instance was desired, then the {@link #MockUp(Object)} constructor should have
    * been used instead.
    * <p/>
    * In any case, for a given mock-up instance this method will always return the same mock instance.
    *
    * @see <a href="http://jmockit.org/tutorial/Faking.html#interfaces">Tutorial</a>
    */
   public final T getMockInstance()
   {
      if (invokedInstance == Void.class) {
         return null;
      }

      if (invokedInstance != null) {
         return invokedInstance;
      }

      if (mockInstance == null && mockedClass != null) {
         @SuppressWarnings("unchecked") T newInstance = (T) createMockInstance(mockedClass);
         setMockInstance(newInstance);
      }

      //noinspection ConstantConditions
      return mockInstance;
   }

   @Nonnull
   private Object createMockInstance(@Nonnull Class<?> mockedClass)
   {
      String mockedClassName = mockedClass.getName();

      if (isGeneratedImplementationClass(mockedClassName)) {
         return newInstance(mockedClass);
      }

      if (Proxy.isProxyClass(mockedClass)) {
         return MockInvocationHandler.newMockedInstance(mockedClass);
      }

      return ConstructorReflection.newUninitializedInstance(mockedClass);
   }

   /**
    * An empty method that can be overridden in a mock-up subclass that wants to be notified whenever the mock-up is
    * automatically torn down.
    * Tear down happens when the mock-up goes out of scope: at the end of the test when applied inside a test, at the
    * end of the test class when applied before the test class, or at the end of the test run when applied through the
    * "<code>mockups</code>" system property.
    * <p/>
    * By default, this method does nothing.
    */
   protected void onTearDown() {}
}
