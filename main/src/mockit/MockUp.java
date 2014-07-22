/*
 * Copyright (c) 2006-2014 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit;

import java.lang.reflect.*;
import java.util.*;

import mockit.internal.mockups.*;
import mockit.internal.startup.*;
import mockit.internal.state.*;
import mockit.internal.state.MockClasses.*;
import mockit.internal.util.*;

import org.jetbrains.annotations.*;

/**
 * A base class used in the creation of a <em>mock-up</em> for a class or interface.
 * Such mock-ups can be used in <em>state-based</em> unit tests or as <em>fake</em> implementations for use in
 * integration tests.
 * <pre>
 *
 * // Define and apply one or more mock-ups:
 * new MockUp&lt;<strong>SomeClass</strong>>() {
 *    &#64;Mock int someMethod(int i) { assertTrue(i > 0); return 123; }
 *    &#64;Mock(maxInvocations = 2) void anotherMethod(int i, String s) { &#47;* validate arguments *&#47; }
 * };
 *
 * // Exercise code under test:
 * codeUnderTest.doSomething();
 * </pre>
 * One or more <em>mock methods</em> annotated {@linkplain Mock as such} must be defined in the concrete subclass.
 * Each {@code @Mock} method should have a matching method or constructor in the mocked class/interface.
 * At runtime, the execution of a mocked method/constructor will get redirected to the corresponding mock method.
 * <p/>
 * When the type to be mocked is specified indirectly through a {@linkplain TypeVariable type variable}, there are two
 * other possible outcomes:
 * <ol>
 * <li>If the type variable "<code>extends</code>" two or more interfaces, a mocked proxy class that implements all
 * interfaces is created, with the proxy instance made available through a call to {@link #getMockInstance()}.
 * Example:
 * <pre>
 *
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
 * <li>If the type variable extends a <em>single</em> type (either an interface or a class), then that type is taken
 * as a <em>base</em> type whose concrete implementation classes should <em>also</em> get mocked.
 * Example:
 * <pre>
 *
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
 * @param <T> specifies the type (class, interface, etc.) to be mocked; multiple interfaces can be mocked by defining
 * a <em>type variable</em> in the test class or test method, and using it as the type argument;
 * if a type variable is used but it extends a single type, then all implementation classes extending/implementing that
 * base type are also mocked;
 * if the type argument itself is a parameterized type, then only its raw type is considered for mocking
 *
 * @see #MockUp()
 * @see #MockUp(Class)
 * @see #MockUp(Object)
 * @see #getMockInstance()
 * @see #tearDown()
 * @see <a href="http://jmockit.github.io/tutorial/StateBasedTesting.html#setUp">Tutorial</a>
 */
public abstract class MockUp<T>
{
   static { Startup.verifyInitialization(); }

   @Nullable private final Class<?> mockedClass;
   @Nullable private Set<Class<?>> classesToRestore;
   @Nullable private T mockInstance;
   @Nullable T invokedInstance;

   /**
    * Applies the {@linkplain Mock mock methods} defined in the concrete subclass to the class or interface specified
    * through the type parameter.
    *
    * @throws IllegalArgumentException if no type to be mocked was specified;
    * or if multiple types were specified through a type variable but not all of them are interfaces;
    * or if there is a mock method for which no corresponding real method or constructor is found;
    * or if the real method matching a mock method is {@code abstract}
    *
    * @see #MockUp(Class)
    */
   protected MockUp()
   {
      validateMockingAllowed();

      Class<?> previouslyMockedClass = findPreviouslyMockedClassIfMockUpAlreadyApplied();

      if (previouslyMockedClass != null) {
         mockedClass = previouslyMockedClass;
         return;
      }

      Type typeToMock = validateTypeToMock();

      if (typeToMock instanceof Class<?>) {
         @SuppressWarnings("unchecked") Class<T> classToMock = (Class<T>) typeToMock;
         mockedClass = redefineClassOrImplementInterface(classToMock, typeToMock);
      }
      else if (typeToMock instanceof ParameterizedType) {
         ParameterizedType parameterizedType = (ParameterizedType) typeToMock;
         @SuppressWarnings("unchecked") Class<T> classToMock = (Class<T>) parameterizedType.getRawType();
         mockedClass = redefineClassOrImplementInterface(classToMock, typeToMock);
      }
      else {
         Type[] typesToMock = ((TypeVariable<?>) typeToMock).getBounds();

         if (typesToMock.length > 1) {
            mockedClass = new MockedImplementationClass<T>(this).createImplementation(typesToMock);
         }
         else {
            mockedClass = new CaptureOfMockedUpImplementations(this, typesToMock[0]).apply();
         }
      }
   }

   private static void validateMockingAllowed()
   {
      if (TestRun.isInsideNoMockingZone()) {
         throw new IllegalStateException("Invalid place to apply a mock-up");
      }
   }

   @Nullable
   private Class<?> findPreviouslyMockedClassIfMockUpAlreadyApplied()
   {
      MockUpInstances mockUpInstances = TestRun.getMockClasses().findPreviouslyAppliedMockUps(this);

      if (mockUpInstances != null) {
         if (mockUpInstances.hasMockUpsForSingleInstances()) {
            return mockUpInstances.initialMockUp.mockedClass;
         }

         tearDown();
      }

      return null;
   }

   @NotNull
   private Type validateTypeToMock()
   {
      Type typeToMock = getTypeToMock();

      if (typeToMock instanceof WildcardType || typeToMock instanceof GenericArrayType) {
         String errorMessage = "Argument " + typeToMock + " for type parameter T of an unsupported kind";
         throw new UnsupportedOperationException(errorMessage);
      }

      return typeToMock;
   }

   @NotNull
   private Type getTypeToMock()
   {
      Class<?> currentClass = getClass();

      do {
         Type superclass = currentClass.getGenericSuperclass();

         if (superclass instanceof ParameterizedType) {
            return ((ParameterizedType) superclass).getActualTypeArguments()[0];
         }

         if (superclass == MockUp.class) {
            throw new IllegalArgumentException("No type to be mocked");
         }

         currentClass = (Class<?>) superclass;
      }
      while (true);
   }

   @NotNull
   private Class<T> redefineClassOrImplementInterface(@NotNull Class<T> classToMock, @Nullable Type typeToMock)
   {
      if (classToMock.isInterface()) {
         return createInstanceOfMockedImplementationClass(classToMock, typeToMock);
      }

      classesToRestore = redefineMethods(classToMock, typeToMock);
      return classToMock;
   }

   @NotNull
   private Class<T> createInstanceOfMockedImplementationClass(@NotNull Class<T> classToMock, @Nullable Type typeToMock)
   {
      return new MockedImplementationClass<T>(this).createImplementation(classToMock, typeToMock);
   }

   @Nullable
   private Set<Class<?>> redefineMethods(@NotNull Class<T> realClass, @Nullable Type mockedType)
   {
      if (TestRun.mockFixture().isMockedClass(realClass)) {
         throw new IllegalArgumentException("Class already mocked: " + realClass.getName());
      }

      return new MockClassSetup(realClass, mockedType, this, null).redefineMethods();
   }

   /**
    * Applies the {@linkplain Mock mock methods} defined in the mock-up subclass to the given class/interface.
    * <p/>
    * In most cases, the constructor with no parameters can be used. This variation should be used only when the type
    * to be mocked is not accessible or known to the test.
    *
    * @see #MockUp()
    */
   protected MockUp(Class<?> classToMock)
   {
      if (classToMock == null) {
         throw new IllegalArgumentException("Null reference when expecting the class to mock");
      }

      validateMockingAllowed();

      Class<?> previouslyMockedClass = findPreviouslyMockedClassIfMockUpAlreadyApplied();

      if (previouslyMockedClass != null) {
         mockedClass = previouslyMockedClass;
         return;
      }

      if (classToMock.isInterface()) {
         //noinspection unchecked
         mockedClass = createInstanceOfMockedImplementationClass((Class<T>) classToMock, classToMock);
      }
      else {
         mockedClass = classToMock;
         //noinspection unchecked
         classesToRestore = redefineMethods((Class<T>) classToMock, null);
         mockInstance = null;
      }
   }

   /**
    * Applies the {@linkplain Mock mock methods} defined in the mock-up subclass to the type specified through
    * the type parameter, but only affecting the given instance.
    */
   protected MockUp(T instanceToMock)
   {
      this();

      if (instanceToMock == null) {
         throw new IllegalArgumentException("Null reference when expecting the instance to mock");
      }

      setMockInstance(instanceToMock);
   }

   private void setMockInstance(@NotNull T mockInstance)
   {
      TestRun.getMockClasses().addMock(this, mockInstance);
      this.mockInstance = mockInstance;
   }

   /**
    * TODO: rewrite according to enhanced semantics
    * Returns the mock instance created for the mocked interface(s), or {@code null} if a class was specified to be
    * mocked instead.
    * This mock instance belongs to a dynamically generated class which implements the mocked interface(s).
    * <p/>
    * For a given mock-up instance, this method always returns the same mock instance.
    * <p/>
    * All methods in the generated implementation class are empty, with non-void methods returning a default value
    * according to the return type: {@literal 0} for {@code int}, {@literal null} for a reference type, and so on.
    * <p/>
    * The {@code equals}, {@code hashCode}, and {@code toString} methods inherited from {@code java.lang.Object} are
    * overridden with an appropriate implementation in each case:
    * {@code equals} is implemented by comparing the two object references (the mock instance and the method argument)
    * for equality; {@code hashCode} is implemented to return the identity hash code for the mock instance; and
    * {@code toString} returns the standard string representation that {@code Object#toString} would have returned.
    *
    * @see <a href="http://jmockit.github.io/tutorial/StateBasedTesting.html#interfaces">Tutorial</a>
    */
   public final T getMockInstance()
   {
      if (invokedInstance == Void.class) {
         //noinspection ReturnOfNull
         return null;
      }

      if (invokedInstance != null) {
         return invokedInstance;
      }

      if (mockInstance == null && mockedClass != null) {
         Object newInstance;

         if (GeneratedClasses.isGeneratedImplementationClass(mockedClass)) {
            newInstance = GeneratedClasses.newInstance(mockedClass);
         }
         else if (Proxy.isProxyClass(mockedClass)) {
            newInstance = MockInvocationHandler.newMockedInstance(mockedClass);
         }
         else {
            newInstance = ConstructorReflection.newUninitializedInstance(mockedClass);
         }

         //noinspection unchecked
         setMockInstance((T) newInstance);
      }

      //noinspection ConstantConditions
      return mockInstance;
   }

   /**
    * Discards the mock methods originally set up by instantiating this mock-up object, restoring mocked methods to
    * their original behaviors.
    * <p/>
    * Note that JMockit will automatically restore classes mocked by a test at the end of its execution, as well as
    * classes mocked for the whole test class before the first test in the next test class is executed.
    */
   public final void tearDown()
   {
      MockUpInstances mockUpInstances = TestRun.getMockClasses().removeMock(this, mockInstance);

      if (!mockUpInstances.hasMockUpsForSingleInstances() && classesToRestore != null) {
         TestRun.mockFixture().restoreAndRemoveRedefinedClasses(classesToRestore);
         classesToRestore = null;
      }
   }
}
