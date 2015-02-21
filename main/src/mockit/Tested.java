/*
 * Copyright (c) 2006-2015 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit;

import java.lang.annotation.*;

/**
 * Indicates a class to be tested, with optional automatic instantiation and/or automatic injection of dependencies.
 * This annotation is applicable to instance fields of a test class; alternatively, it can be used as a meta-annotation
 * on a user-defined annotation which, in turn, needs to have runtime retention and be applicable to fields.
 * <p/>
 * If the tested field is not {@code final}, then it is eligible for automatic instantiation and initialization.
 * By default, automatic creation occurs just before a test method is executed, provided the tested field remains
 * {@code null} at such time; this default can be changed by specifying the {@link #availableDuringSetup} optional
 * attribute as {@code true}.
 * Whenever automatic creation occurs, a suitable instance of the tested class is created, initialized, and assigned to
 * the field.
 * Available {@linkplain Injectable injectables} are used, either as argument values for the chosen constructor of the
 * tested class, or as values to set into injected fields of the newly-created tested object.
 * <p/>
 * For <em>constructor injection</em>, all constructor parameters (if any) must be satisfied with the values of
 * available injectables.
 * If the tested class has a constructor annotated with the standard CDI annotation "<code>@Inject</code>", then it is
 * the one to be used;
 * otherwise, if there are multiple satisfiable constructors then the one with the most parameters <em>and</em> the
 * widest accessibility (ie, first {@code public}, then {@code protected}, then <em>package-private</em>, and finally
 * {@code private}) is chosen.
 * The matching between injectable values and constructor parameters is done by <em>type</em> when there is only one
 * parameter of a given type; otherwise, by type <em>and name</em>.
 * <p/>
 * Whenever the tested object is created automatically, <em>field injection</em> is also performed.
 * Only non-<code>final</code> fields are considered, between those declared in the tested class itself or in one of its
 * super-classes; at this time constructor injection already occurred, so only fields which remain uninitialized are
 * targeted.
 * For each such <em>target</em> field, the value of a still unused injectable of the <em>same</em> type is assigned, if
 * any is available.
 * Multiple target fields of the same type can be injected from separate injectables, provided each target field has the
 * same name as an available injectable of that type.
 * Finally, if there is no matching and available injectable value for a given target field, it is left unassigned,
 * unless the target field is for a <em>required</em> dependency; note that all fields marked with a DI annotation
 * (such as {@code @Inject}, {@code Autowired}, etc.) indicate required dependencies by default
 * (the use of "<code>@Autowired(required = false)</code>" is respected, if present).
 * <p/>
 * Whenever constructor or field injection is used, the value of each injectable goes into <em>at most one</em> matching
 * constructor parameter or instance field of a tested class.
 * <p/>
 * The tested class can be {@code abstract}.
 * In this case, if the tested field is left null then a subclass implementing all abstract methods is automatically
 * generated and instantiated.
 * The abstract method implementations are automatically <em>mocked</em> so that expectations can be recorded or
 * verified on them.
 * <p/>
 * This annotation is not only intended for <em>unit</em> tests, but also for <em>integration</em> tests.
 * In the second case, the {@link #fullyInitialized} attribute will normally be specified as {@code true}, so that all
 * eligible fields in the tested object get initialized with a suitable instance, which itself is recursively
 * initialized in the same way.
 *
 * @see <a href="http://jmockit.org/tutorial/BehaviorBasedTesting.html#tested">Tutorial</a>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.ANNOTATION_TYPE})
public @interface Tested
{
   /**
    * Indicates that each and every field of the tested object should be assigned a value, either an
    * {@linkplain Injectable @Injectable} or a real (unmocked) instance of the field type.
    * <p/>
    * For each field of a reference type that would otherwise remain {@code null}, an attempt is made to automatically
    * create and recursively initialize a suitable real instance.
    * For this attempt to succeed, the type of the field must either be a concrete class having a public no-args
    * constructor, or a known interface for which a real instance can be created.
    * Currently, the {@code javax.persistence.EntityManagerFactory} and {@code javax.persistence.EntityManager}
    * interfaces are supported.
    */
   boolean fullyInitialized() default false;

   /**
    * Indicates whether the tested class gets instantiated and initialized <em>before</em> the execution of test setup
    * methods (ie, those annotated as {@code @Before} or {@code @BeforeMethod}), or <em>after</em> them.
    * <p/>
    * Typically, the early creation of tested objects is useful in a test setup method, which can use them for the
    * initialization of other objects.
    * Another potential use is to affect the initialization of other tested objects in the same test class, during their
    * creation <em>after</em> setup.
    * <p/>
    * In order to be made available during test setup, a tested object won't be injected from {@code @Injectable}
    * test method parameters, only from injectable fields.
    */
   boolean availableDuringSetup() default false;
}
