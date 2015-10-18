/*
 * Copyright (c) 2006-2015 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit;

import java.lang.annotation.*;
import java.util.*;
import javax.enterprise.inject.*;
import javax.enterprise.util.*;
import javax.inject.*;
import static java.util.Arrays.*;

import org.junit.*;
import static org.junit.Assert.*;

@Ignore("Tests for issue #203")
public final class IterableDITest
{
   static class Collaborator {
      final int value;
      Collaborator() { value = 0; }
      Collaborator(int value) { this.value = value; }
   }

   static final class TestedClassWithIterableInjectionPoints
   {
      final List<String> names;
      @Inject Collection<Collaborator> collaborators;

      @Inject
      TestedClassWithIterableInjectionPoints(List<String> names)
      {
         this.names = names;
      }
   }

   @Tested TestedClassWithIterableInjectionPoints tested1;
   @Injectable final List<String> nameList = asList("One", "Two");
   @Injectable final Collection<Collaborator> colList = asList(new Collaborator(1), new Collaborator(2));

   @Test
   public void injectMultiValuedInjectablesIntoInjectionPointsOfTheSameCollectionTypes()
   {
      assertSame(nameList, tested1.names);
      assertSame(colList, tested1.collaborators);
   }

   static final class TestedClassWithInstanceInjectionPoints
   {
      final Set<String> names;
      @Inject Instance<Collaborator> collaborators;

      @Inject
      TestedClassWithInstanceInjectionPoints(Instance<String> names)
      {
         this.names = new HashSet<String>();

         for (String name : names) {
            this.names.add(name);
         }
      }
   }

   @Tested TestedClassWithInstanceInjectionPoints tested2;

   static final class Listed<T> implements Instance<T>
   {
      final List<T> instances;
      Listed(T... instances) { this.instances = asList(instances); }
      @Override public Instance<T> select(Annotation... annotations) { return null; }
      @Override public <U extends T> Instance<U> select(Class<U> uClass, Annotation... annotations) { return null; }
      @Override public <U extends T> Instance<U> select(TypeLiteral<U> tl, Annotation... annotations) { return null; }
      @Override public boolean isUnsatisfied() { return false; }
      @Override public boolean isAmbiguous() { return false; }
      @Override public void destroy(T instance) {}
      @Override public Iterator<T> iterator() { return instances.iterator(); }
      @Override public T get() { throw new RuntimeException("Unexpected"); }
   }

   static <T> List<T> toList(Iterable<T> instances)
   {
      List<T> list = new ArrayList<T>();

      for (T instance : instances) {
         list.add(instance);
      }

      return list;
   }

   @Injectable Collaborator col1;
   @Injectable Collaborator col2;
   @Injectable final String first = "Abc";
   @Injectable("Test") String second;

   @Test
   public void allowMultipleInjectablesOfSameTypeToBeObtainedFromInstanceInjectionPoint(@Injectable("123") String third)
   {
      assertEquals(asList("Abc", "Test", "123"), tested2.names);

      Instance<Collaborator> collaborators = tested2.collaborators;
      assertFalse(collaborators.isAmbiguous());
      assertFalse(collaborators.isUnsatisfied());

      List<Collaborator> collaboratorInstances = toList(collaborators);
      assertEquals(asList(col1, col2), collaboratorInstances);
   }
}
