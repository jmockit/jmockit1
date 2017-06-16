/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit;

import java.util.*;
import javax.inject.*;

import org.junit.*;
import static org.junit.Assert.*;

public final class TestedFieldExtractionTest
{
   static class Dependency {}

   static class TestedClassWithMultipleFieldsOfSameType
   {
      Dependency dep1;
      Dependency dep2;
   }

   @Tested(fullyInitialized = true) TestedClassWithMultipleFieldsOfSameType tested1;
   @Tested Dependency dep;

   @Test
   public void extractMultipleFieldsOfSameTypeIntoSingleTestedField()
   {
      assertNotNull(tested1.dep1);
      assertNotNull(tested1.dep2);
      assertSame(tested1.dep1, tested1.dep2); // unqualified fields of same type get the same created instance
      assertSame(tested1.dep1, dep);
   }

   static class TestedClassWithNamedFields
   {
      @Inject @Named("first") Dependency dep1;
      @Inject @Named("second") Dependency dep2;
   }

   @Tested(fullyInitialized = true) TestedClassWithNamedFields tested2;
   @Tested Dependency first;
   @Tested Dependency second;

   @Test
   public void extractMultipleQualifiedFieldsOfSameTypeIntoSeparateTestedFields()
   {
      assertNotNull(tested2.dep1);
      assertNotNull(tested2.dep2);
      assertNotSame(tested2.dep1, tested2.dep2);
      assertSame(tested2.dep1, first);
      assertSame(tested2.dep2, second);
   }

   static class TestedClassWithInitializedFieldsOfVariousTypes
   {
      final String name = "test";
      int number = 123;
      @Inject @Named("test") final List<String> names = new ArrayList<String>();
      Map<Integer, String> numbersAndNames = new HashMap<Integer, String>();
   }

   @Tested TestedClassWithInitializedFieldsOfVariousTypes tested3;
   @Tested String name;
   @Tested List<String> test;
   @Tested Map<Integer, String> numbersAndNames;
   @Tested int number;

   @Test
   public void extractFieldsInitializedByConstructorOfTestedClass()
   {
      assertEquals(tested3.name, name);
      assertEquals(tested3.number, number);
      assertSame(tested3.names, test);
      assertSame(tested3.numbersAndNames, numbersAndNames);
   }
}
