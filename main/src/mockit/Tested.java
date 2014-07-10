/*
 * Copyright (c) 2006-2014 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit;

import java.lang.annotation.*;

/**
 * Indicates a class to be tested in isolation from selected dependencies, with optional automatic instantiation and/or
 * automatic injection of dependencies.
 * This annotation is only applicable to instance fields of a test class.
 * <p/>
 * If the tested field is not {@code final} and its value remains {@code null} at the time a test method is about to be
 * executed, then a suitable instance of the tested class is created and assigned to the field.
 * At this time, <em>constructor injection</em> will take place, provided all of the constructor parameters (if any) can
 * be satisfied with the values of available {@linkplain Injectable injectable} fields and/or injectable test method
 * parameters.
 * If the tested class has a constructor annotated with the standard Java EE annotation "<code>@Inject</code>", then it
 * is the one to be used;
 * otherwise, if there are multiple satisfiable constructors then the one with the most parameters <em>and</em> the
 * widest accessibility (ie, first {@code public}, then {@code protected}, then <em>package-private</em>, and finally
 * {@code private}) is chosen.
 * The matching between <em>injectable</em> fields/parameters and <em>constructor</em> parameters is done by
 * <em>type</em> when there is only one parameter of a given type; otherwise, by type <em>and name</em>.
 * <p/>
 * Whenever the tested object is created automatically, <em>field injection</em> is also performed.
 * Only non-<code>final</code> instance fields are considered, between those declared in the tested class itself or in
 * one of its super-classes; at this time constructor injection already occurred, so only fields annotated with
 * {@code @Inject} or which remain uninitialized are targeted.
 * For each such <em>target</em> field, the value of an still unused {@linkplain Injectable injectable} field or test
 * method parameter of the <em>same</em> type is assigned.
 * Multiple target fields of the same type can be injected from separate injectables, provided each target field has the
 * same name as an available injectable field/parameter of that type.
 * Finally, if there is no matching and available injectable value for a given target field, it is left unassigned.
 * <p/>
 * Whenever constructor or field injection is used, the value of each injectable mock field/parameter goes into
 * <em>at most one</em> matching constructor parameter or instance field of a tested class.
 * <p/>
 * The tested class can be {@code abstract}.
 * In this case, if the tested field is left null then a subclass implementing all abstract methods is automatically
 * generated and instantiated.
 * The abstract method implementations are automatically <em>mocked</em> so that expectations can be recorded or
 * verified on them.
 *
 * @see <a href="http://jmockit.googlecode.com/svn/trunk/www/tutorial/BehaviorBasedTesting.html#tested">Tutorial</a>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Tested
{
}
