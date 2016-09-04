/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit;

import java.util.*;
import javax.enterprise.inject.*;
import javax.inject.*;
import static java.util.Arrays.*;

import org.junit.*;
import static org.junit.Assert.*;

public final class InstanceDITest
{
   static class Collaborator {}

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

   @Tested TestedClassWithInstanceInjectionPoints tested;
   @Injectable Collaborator col1;
   @Injectable Collaborator col2;
   @Injectable final List<String> names = asList("Abc", "Test", "123");

   @Test
   public void allowMultipleInjectablesOfSameTypeToBeObtainedFromInstanceInjectionPoint()
   {
      assertEquals(new HashSet<String>(asList("Abc", "Test", "123")), tested.names);

      Instance<Collaborator> collaborators = tested.collaborators;
      assertFalse(collaborators.isAmbiguous());
      assertFalse(collaborators.isUnsatisfied());

      List<Collaborator> collaboratorInstances = toList(collaborators);
      assertEquals(asList(col1, col2), collaboratorInstances);
   }

   static <T> List<T> toList(Iterable<T> instances)
   {
      List<T> list = new ArrayList<T>();

      for (T instance : instances) {
         list.add(instance);
      }

      return list;
   }
}
