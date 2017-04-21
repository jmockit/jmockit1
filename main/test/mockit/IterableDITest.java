/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit;

import java.util.*;
import javax.inject.*;

import static java.util.Arrays.asList;

import org.junit.*;
import static org.junit.Assert.*;

public final class IterableDITest
{
   static class Collaborator
   {
      final int value;
      Collaborator() { value = 0; }
      Collaborator(int value) { this.value = value; }
   }

   static final class TestedClassWithIterableInjectionPoints
   {
      final List<String> names;
      @Inject Collection<Collaborator> collaborators;
      Set<? extends Number> numbers;

      @Inject
      TestedClassWithIterableInjectionPoints(List<String> names) { this.names = names; }
   }

   @Tested TestedClassWithIterableInjectionPoints tested1;
   @Injectable final List<String> nameList = asList("One", "Two");
   @Injectable final Collection<Collaborator> colList = asList(new Collaborator(1), new Collaborator(2));

   @Test
   public void injectMultiValuedInjectablesIntoInjectionPointsOfTheSameCollectionTypes()
   {
      assertSame(nameList, tested1.names);
      assertSame(colList, tested1.collaborators);
      assertNull(tested1.numbers);
   }

   static class Dependency {}
   static class SubDependency extends Dependency {}
   static class TestedClassWithInjectedList { @Inject List<Dependency> dependencies; }

   @Tested TestedClassWithInjectedList tested2;
   @Injectable Dependency dependency;

   @Test
   public void injectMockedInstanceIntoList()
   {
      assertTrue(tested2.dependencies.contains(dependency));
   }

   @Test
   public void injectSubTypeInstanceIntoListOfBaseType(@Injectable SubDependency sub)
   {
      assertTrue(tested2.dependencies.contains(sub));
   }
}
