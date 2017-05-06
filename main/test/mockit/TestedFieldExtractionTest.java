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

   static class TestedClassWithNamedFields
   {
      @Inject @Named("first") Dependency dep1;
      @Inject @Named("second") Dependency dep2;
   }

   @Tested(fullyInitialized = true) TestedClassWithNamedFields tested1;
   @Tested Dependency first;
   @Tested Dependency second;

   @Test
   public void extractMultipleInjectedFieldsOfSameTypeIntoSeparateTestedFields()
   {
      assertNotNull(tested1.dep1);
      assertNotNull(tested1.dep2);
      assertNotSame(tested1.dep1, tested1.dep2);
      assertSame(tested1.dep1, first);
      assertSame(tested1.dep2, second);
   }

   static class TestedClassWithInitializedFieldsOfVariousTypes
   {
      final String name = "test";
      int number = 123;
      @Inject @Named("test") final List<String> names = new ArrayList<String>();
      Map<Integer, String> numbersAndNames = new HashMap<Integer, String>();
   }

   @Tested TestedClassWithInitializedFieldsOfVariousTypes tested2;
   @Tested String name;
   @Tested List<String> test;
   @Tested Map<Integer, String> numbersAndNames;
   @Tested int number;

   @Test
   public void extractFieldsInitializedByConstructorOfTestedClass()
   {
      assertEquals(tested2.name, name);
      assertEquals(tested2.number, number);
      assertSame(tested2.names, test);
      assertSame(tested2.numbersAndNames, numbersAndNames);
   }
}
