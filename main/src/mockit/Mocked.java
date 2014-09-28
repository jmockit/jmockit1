/*
 * Copyright (c) 2006-2014 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit;

import java.lang.annotation.*;

/**
 * Indicates an instance field of a test class as being a <em>mock field</em>, or a parameter of a test method as a
 * <em>mock parameter</em>; in either case, the declared type of the field/parameter is a <em>mocked type</em>, whose
 * instances are <em>mocked instances</em>.
 * <p/>
 * Any type can be mocked, except for primitive and array types.
 * A mocked instance of that type is automatically created and assigned to the mock field/parameter, for use when
 * {@linkplain mockit.Expectations recording} or {@linkplain mockit.Verifications verifying} expectations.
 * <p/>
 * The effect of declaring a {@code @Mocked} type, by default, is that all new instances of that type, as well as those
 * previously created, will also be mocked instances; this will last for the duration of each test where the associated
 * mock field/parameter is in scope.
 * Also, all methods of the mocked type will be mocked, unless a subset is {@linkplain #value explicitly specified}.
 * <p/>
 * When the mocked type is a class, all super-classes up to but not including {@code java.lang.Object} are also mocked.
 * Additionally, <em>static methods</em> and <em>constructors</em> are mocked as well, just like instance methods.
 * <p/>
 * When mocking an {@code enum} type, the {@code java.lang.Enum} base class is <em>not</em> mocked by default.
 * If needed, however, base types like {@code Object} and {@code Enum} can be mocked by explicitly declaring a mock
 * field or mock parameter of the specific base type.
 * <p/>
 * While a method or constructor is mocked, an invocation does not result in the execution of the original code, but in
 * a (generated) call into JMockit, which then responds with either a default or a <em>recorded</em> result (or with a
 * constraint violation, if the invocation is deemed to be unexpected - which depends on a few factors discussed
 * elsewhere).
 * <p/>
 * Static <em>class initializers</em> (including assignments to {@code static} fields) of a mocked class are not
 * affected, unless {@linkplain #stubOutClassInitialization specified otherwise}.
 * <p/>
 * Normally, a new mocked instance gets created and assigned to a declared mock field/parameter automatically.
 * An exception to that occurs in the case of a mocked {@code enum} type, where the first enumeration element is used.
 * Alternatively, and only in the case of a mock <em>field</em>, the test itself can provide the instance by declaring
 * the field as {@code final} and explicitly assigning it with the desired instance (which will still be a mocked
 * instance); if no instance is necessary (perhaps because only static methods or constructors will be called), then
 * this final field can receive the {@code null} reference.
 * Mock parameters, on the other hand, will always receive a new mocked instance whenever the test method is executed by
 * the test runner.
 * <p/>
 * By default, mocking will cascade into the return types of all non-void methods belonging to the mocked type.
 * If needed, this {@link #cascading} process can be explicitly disabled on a per-mocked type basis.
 *
 * @see #value
 * @see #stubOutClassInitialization
 * @see #cascading
 * @see <a href="http://jmockit.github.io/tutorial/BehaviorBasedTesting.html#declaration">Tutorial</a>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.PARAMETER})
public @interface Mocked
{
   /**
    * One or more <em>mock filters</em>.
    * Given a target class for mocking, only those methods and constructors which match at least one filter will be
    * mocked.
    * <p/>
    * Each mock filter must follow the syntax <strong>{@code [nameRegex][(paramTypeName...)]}</strong>, where
    * {@code nameRegex} is a {@linkplain java.util.regex.Pattern regular expression} for matching method names, and
    * {@code paramTypeName} is the name of a primitive or reference parameter type (actually, any suffix of the type
    * name is enough, like "String" instead of the full class name "java.lang.String").
    * If {@code nameRegex} is omitted the filter matches only constructors.
    * If {@code (paramTypeName...)} is omitted the filter matches methods with any parameters.
    * <p/>
    * If no filters are specified, then all methods and constructors declared in the target class are mocked.
    * <p/>
    * A filter containing just the empty string matches <em>no</em> methods or constructors of the target class;
    * this can be used to obtain a mocked instance where no executable code is actually mocked.
    *
    * @see #stubOutClassInitialization
    * @see <a href="http://jmockit.github.io/tutorial/BehaviorBasedTesting.html#staticPartial">Tutorial</a>
    */
   String[] value() default {};

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

   /**
    * Indicates whether the mocked type is <em>cascading</em> or not, ie, whether its non-void methods should return
    * mocked instances or {@code null}; {@code true} by default.
    * <p/>
    * A non-<code>void</code> mocked method which returns a reference type will produce, if and when called, a
    * <em>cascaded</em> mocked instance according to its return type.
    * Methods returning {@code String}, primitive wrappers, or collection types, however, are <em>not</em> cascaded,
    * producing non-mocked default values instead (in either case, a <em>recorded</em> return value for the method will
    * always produce the recorded value).
    * This cascaded mocking process means that mocked methods will never return {@code null}, unless {@code null} is
    * explicitly recorded as the result.
    * <p/>
    * If a mock field/parameter is explicitly declared with the same type (or, alternatively, a subtype) of some
    * cascaded type, then the instance originally assigned to the mock field/parameter will be used as the cascaded
    * instance.
    * This allows expectations on intermediate cascaded instances to be recorded/verified when needed, without having
    * to specify the full method call chain from the "root" instance to the "leaf" instance of interest.
    */
   boolean cascading() default true;
}
