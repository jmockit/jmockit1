/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit;

import java.io.*;
import java.lang.annotation.*;
import javax.inject.*;

import org.junit.*;
import static org.junit.Assert.*;

public final class TestedClassWithQualifiedDependencyTest
{
   public static class TestedClass
   {
      @Inject private Dependency1 dep1;
      @Autowired private Dependency2 dep2;
   }

   public static class Dependency1
   {
      @Named("action1") private Runnable action;
      @Qualifier("foo") private Serializable qualifiedDep;
   }

   public static class Dependency2
   {
      @Qualifier("action2") private Runnable action;
      @Named("bar") private Serializable qualifiedDep;
   }

   @Tested Serializable foo;

   @Before
   public void createQualifiedDependency()
   {
      foo = "foo";
   }

   @Tested Dependency2 dependency2;
   @Tested(fullyInitialized = true) TestedClass tested;
   @Injectable Runnable action1;

   @Test
   public void useTestedObjectWithDifferentDependenciesEachHavingAQualifiedSubDependency(@Injectable Runnable action2)
   {
      assertSame(action2, dependency2.action);
      assertSame(dependency2, tested.dep2);
      assertSame(action1, tested.dep1.action);
      assertSame(foo, tested.dep1.qualifiedDep);
      assertNull(tested.dep2.qualifiedDep);
   }

   @Tested DependencyImpl dep1;
   @Tested(fullyInitialized = true) ClassWithQualifiedDependencyOfAbstractType tested2;

   public interface Dependency {}
   static class DependencyImpl implements Dependency {}
   static class ClassWithQualifiedDependencyOfAbstractType { @Named("dep1") Dependency dependency; }

   @Test
   public void useTestedObjectOfSubtypeForQualifiedAbstractDependencyTypeInAnotherTestedObject()
   {
      assertSame(dep1, tested2.dependency);
   }
}

@Retention(RetentionPolicy.RUNTIME)
@interface Qualifier { String value() default ""; }
