/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit;

import java.lang.annotation.*;
import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.*;

/**
 * Indicates a class to be tested, with optional automatic instantiation and/or automatic injection of dependencies.
 * This annotation is applicable to instance fields of a test class, and to test method parameters; alternatively, it
 * can be used as a meta-annotation on a user-defined annotation which, in turn, needs to have
 * {@linkplain RetentionPolicy#RUNTIME runtime} {@linkplain Retention retention} and be {@linkplain Target applicable}
 * to {@linkplain ElementType#FIELD fields} and/or {@linkplain ElementType#PARAMETER parameters}.
 * <p/>
 * Every non-<tt>final</tt> tested field and every tested parameter is eligible for automatic instantiation and
 * initialization.
 * By default, automatic instantiation occurs just before a test method is executed.
 * This default can be changed by specifying the {@link #availableDuringSetup} optional attribute as <tt>true</tt> in a
 * tested field declaration (it is ignored if applied to a tested parameter).
 * Whenever automatic creation occurs, a suitable instance of the tested class is created, initialized, and assigned to
 * the tested field or passed as argument to the tested parameter when the test method gets executed.
 * Available {@linkplain Injectable injectables} and other <tt>@Tested</tt> values are used, either as argument values
 * for the chosen constructor of the tested class, or as values to set into injected fields of the newly-created tested
 * object.
 * <p/>
 * For <em>constructor injection</em>, all constructor parameters (if any) must be satisfied with available
 * tested/injectable values.
 * If the tested class has a constructor annotated with the standard CDI annotation "<code>@Inject</code>", then it is
 * the one to be used;
 * otherwise, if there are multiple satisfiable constructors then the one with the most parameters <em>and</em> the
 * widest accessibility (ie, first <tt>public</tt>, then <tt>protected</tt>, then <em>package-private</em>, and finally
 * <tt>private</tt>) is chosen.
 * The matching between injectable values and constructor parameters is done by <em>type</em> when there is only one
 * parameter of a given type; otherwise, by type <em>and name</em>.
 * <p/>
 * <em>Field injection</em> is performed on all tested objects, even when it was not instantiated automatically.
 * Only non-<code>final</code> fields are considered, between those declared in the tested class itself or in one of its
 * super-classes; at this time constructor injection already occurred, so only fields which remain uninitialized are
 * targeted.
 * For each such <em>target</em> field, the value of a still unused injectable of a matching type is assigned, if any is
 * available.
 * When a tested object has multiple target fields of a matching type, not just the type but also the <em>name</em> of
 * each field will be used when looking for available injectables.
 * Finally, if there is no matching and available injectable value for a given target field, it is left unassigned,
 * unless the target field is for a <em>required</em> dependency; note that all fields marked with a DI annotation
 * (such as <tt>@Inject</tt>, <tt>@Autowired</tt>, etc.) indicate required dependencies by default
 * (the use of "<code>@Autowired(required = false)</code>" is respected, if present).
 * <p/>
 * Tested fields/parameters whose declared type is primitive, a primitive wrapper, numeric, or an enum can use the
 * {@link #value} attribute to specify an initial value from a string.
 * <p/>
 * Custom names specified in field annotations from Java EE (<tt>@Resource(name)</tt>, <tt>@Named</tt>) or the Spring
 * framework (<tt>@Qualifier</tt>) are used when looking for a matching <tt>@Injectable</tt> or <tt>@Tested</tt> value.
 * When such a name contains a <tt>-</tt> (dash) or <tt>.</tt> (dot) character, the corresponding camel-cased name is
 * used instead.
 * <p/>
 * Whenever constructor or field injection is used, the value of each injectable goes into <em>at most one</em> matching
 * constructor parameter or instance field of a tested class.
 * <p/>
 * The tested class can be <tt>abstract</tt>.
 * In this case, if the tested field is left null then a subclass implementing all abstract methods is automatically
 * generated and instantiated.
 * The abstract method implementations are automatically <em>mocked</em> so that expectations can be recorded or
 * verified on them.
 * <p/>
 * When the {@link #fullyInitialized} attribute is <tt>true</tt>, all eligible fields in the tested object will get
 * initialized with a suitable instance, which itself is recursively initialized in the same way.
 *
 * @see <a href="http://jmockit.org/tutorial/Mocking.html#tested" target="tutorial">Tutorial</a>
 */
@Retention(RUNTIME)
@Target({FIELD, PARAMETER, ANNOTATION_TYPE, METHOD})
public @interface Tested
{
   /**
    * Specifies a literal value when the type of the tested field/parameter is <tt>String</tt>, a primitive or wrapper
    * type, a number type, or an enum type.
    * For a primitive/wrapper/number type, the value provided must be convertible to it.
    * For an enum type, the given textual value must equal the name of one of the possible enum values.
    */
   String value() default "";

   /**
    * Indicates that each non-<tt>final</tt> field of the tested object that is eligible for injection should be
    * assigned a value, which can be an available {@linkplain Injectable @Injectable} or <tt>@Tested</tt> value of a
    * type assignable to the field type, or a real (unmocked) instance of the field type.
    * <p/>
    * Non-eligible fields are those that have already being assigned from a constructor, or that have a primitive,
    * array, annotation, or JRE type (with the exception of the types described below, which are given special
    * treatment).
    * Also non-eligible are any <tt>static</tt> or <tt>volatile</tt> fields, unless annotated with an <em>injection
    * annotation</em> (one of <tt>@Inject</tt>, <tt>@Resource</tt>, <tt>@Autowired</tt>, <tt>@Value</tt>, <tt>@EJB</tt>,
    * <tt>@PersistenceContext</tt>, or <tt>@PersistenceUnit</tt>).
    * <p/>
    * For each field of a reference type that would otherwise remain <tt>null</tt>, an attempt is made to automatically
    * create and recursively initialize a suitable real instance.
    * For this attempt to succeed, the type of the field must either be a concrete class having a constructor that can
    * be satisfied by available tested/injectable values and/or by recursively created dependencies, or a known
    * interface (see below) for which a real instance can be created.
    * <p/>
    * Constructor injection is also supported.
    * In this case, the same rules used for injected fields apply to the parameters of the constructor that gets chosen
    * for automatic instantiation.
    * <p/>
    * Currently, the following standard types (some of which are Java EE interfaces) have special support:
    * <ul>
    *    <li>
    *       {@link java.util.logging.Logger}: a logger is automatically
    *       {@linkplain java.util.logging.Logger#getLogger(String) created} with the name of the tested class.
    *    </li>
    *    <li>
    *       {@link javax.sql.DataSource}: a JDBC data source is created and configured according to a matching
    *       {@link javax.annotation.sql.DataSourceDefinition @DataSourceDefinition} in the tested class.
    *    </li>
    *    <li>
    *       {@link javax.inject.Provider Provider&lt;T>}: a provider which produces an instance of type <tt>T</tt> is
    *       injected.
    *    </li>
    *    <li>
    *       JPA interfaces {@link javax.persistence.EntityManagerFactory} and {@link javax.persistence.EntityManager}:
    *       created through calls to {@link javax.persistence.Persistence#createEntityManagerFactory(String)} and
    *       {@link javax.persistence.EntityManagerFactory#createEntityManager()}, provided a suitable
    *       <tt>META-INF/persistence.xml</tt> file is available in the runtime classpath.
    *    </li>
    *    <li>
    *       Servlet interfaces {@link javax.servlet.ServletContext} and {@link javax.servlet.http.HttpSession}:
    *       objects that emulate the servlet context and HTTP session are automatically created for use in tests.
    *    </li>
    *    <li>
    *       {@link javax.enterprise.context.Conversation}: an object that emulates a web application's conversation
    *       context is created.
    *    </li>
    * </ul>
    */
   boolean fullyInitialized() default false;

   /**
    * Indicates whether the tested class gets instantiated and initialized <em>before</em> the execution of test setup
    * methods (ie, those annotated as <tt>@Before</tt> or <tt>@BeforeMethod</tt>), or <em>after</em> them.
    * <p/>
    * Typically, the early creation of tested objects is useful in a test setup method, which can use them for the
    * initialization of other objects.
    * Another potential use is to affect the initialization of other tested objects in the same test class, during their
    * creation <em>after</em> setup.
    * Finally, objects made available during setup are also available during the execution of any tear-down methods.
    */
   boolean availableDuringSetup() default false;
}
