/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit;

import java.lang.annotation.*;

/**
 * Indicates an instance field of a test class as being a <em>mock field</em>, or a parameter of a test method as a
 * <em>mock parameter</em>; in either case, the declared type of the field/parameter is a <em>mocked type</em>, whose
 * instances are <em>mocked instances</em>.
 * <p/>
 * Mocked types can also be introduced by other annotations: {@linkplain Injectable @Injectable},
 * {@link Capturing @Capturing}.
 * Their effect is to <em>extend</em> and/or <em>constrain</em> the mocking capabilities here specified.
 * <p/>
 * Any type can be mocked, except for primitive and array types.
 * A mocked instance of that type is automatically created and assigned to the mock field/parameter, for use when
 * {@linkplain Expectations recording} and/or {@linkplain Verifications verifying} expectations.
 * For a mock <em>field</em>, the test itself can provide the instance by declaring the field as {@code final} and
 * assigning it the desired instance (or {@code null}).
 * <p/>
 * The effect of declaring a {@code @Mocked} type, <em>by default</em>, is that all new instances of that type, as well
 * as those previously created, will also be mocked instances; this will last for the duration of each test where the
 * associated mock field/parameter is in scope.
 * Also, all non-<code>private</code> methods of the mocked type will be mocked.
 * <p/>
 * When the mocked type is a class, all super-classes up to but not including {@code java.lang.Object} are also mocked.
 * Additionally, <em>static methods</em> and <em>constructors</em> are mocked as well, just like instance methods;
 * <em>native</em> methods are also mocked, provided they are {@code public} or {@code protected}.
 * <p/>
 * When mocking an {@code enum} type, the {@code java.lang.Enum} base class is <em>not</em> mocked by default.
 * If needed, however, base types like {@code Object} and {@code Enum} can be mocked by explicitly declaring a mock
 * field or mock parameter of the specific base type.
 * <p/>
 * While a method or constructor is mocked, an invocation does not result in the execution of the original code, but in
 * a (generated) call into JMockit, which then responds with either a default or a <em>recorded</em>
 * {@linkplain Expectations#result result} (or with a {@linkplain Expectations#times constraint} violation, if the
 * invocation is deemed to be unexpected).
 * <p/>
 * Mocking will automatically <em>cascade</em> into the return types of all non-<code>void</code> methods belonging to
 * the mocked type, except for non-eligible ones (primitive wrappers, {@code String}, and collections/maps).
 * When needed, such cascaded returns can be overridden by explicitly recording a return value for the mocked method.
 * If there is a mock field/parameter with the same type (or a subtype) of some cascaded type, then the original
 * instance from that mock field/parameter will be used as the cascaded instance, rather than a new one being created;
 * this applies to all cascading levels, and even to the type of the mock field/parameter itself (ie, if a method in
 * class/interface "<code>A</code>" has return type {@code A}, then it will return itself by default).
 * Finally, when new cascaded instances are created, {@linkplain Injectable @Injectable} semantics apply.
 * <p/>
 * Static <em>class initializers</em> (including assignments to {@code static} fields) of a mocked class are not
 * affected, unless {@linkplain #stubOutClassInitialization specified otherwise}.
 *
 * @see #stubOutClassInitialization
 * @see <a href="http://jmockit.org/tutorial/Mocking.html#mocked" target="tutorial">Tutorial</a>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.PARAMETER})
public @interface Mocked
{
   /**
    * Indicates whether <em>static initialization code</em> in the mocked class should be stubbed out or not.
    * Static initialization includes the execution of assignments to static fields of the class and the execution of
    * static initialization blocks, if any.
    * (Note that {@code static final} fields initialized with <em>compile-time</em> constants are not assigned at
    * runtime, remaining unaffected whether the class is stubbed out or not.)
    * <p/>
    * By default, static initialization code in a mocked class is <em>not</em> stubbed out.
    * The JVM will only perform static initialization of a class <em>once</em>, so stubbing out the initialization code
    * can have unexpected consequences.
    * Static initialization will occur the first time the class is instantiated, has a static method called on it, or
    * has a static field whose value is defined at runtime accessed; these are the only events which prompt the JVM to
    * initialize a class.
    * If the original class initialization code was stubbed out, then it will not be there to be executed at the time of
    * static initialization, potentially leaving static fields {@code null} and later causing
    * {@code NullPointerException}'s to occur.
    */
   boolean stubOutClassInitialization() default false;
}
