/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit;

import java.io.*;
import javax.annotation.*;
import javax.inject.*;
import javax.sql.*;

import org.junit.*;
import static org.junit.Assert.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

public final class TestedClassWithQualifiedDependencyTest
{
   public static class TestedClass
   {
      @Inject private Dependency1 dep1;
      @Autowired private Dependency2 dep2;
      @Resource(name = "main-db") private DataSource ds;
      @Named("some.textual.value") private String text;
      @Qualifier("a.BC-12") private Long numericValue;
      @Qualifier("a.BC-12b") private Long numericValue2;
   }

   public static class Dependency1
   {
      @Inject @Named("action1") private Runnable action;
      @Autowired @Qualifier("foo") private Serializable qualifiedDep;
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
   @Tested final Long aBC12 = 123L;
   @Tested final Long aBC12b = 45L;
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

   @Injectable DataSource mainDb;

   @Test
   public void verifyDependenciesHavingCompositeNames(@Injectable("text value") String someTextualValue)
   {
      assertSame(mainDb, tested.ds);
      assertEquals(someTextualValue, tested.text);
      assertEquals(aBC12, tested.numericValue);
      assertEquals(aBC12b, tested.numericValue2);
   }
}
