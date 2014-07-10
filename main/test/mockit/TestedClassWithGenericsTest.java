/*
 * Copyright (c) 2006-2014 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;

import org.junit.*;
import static org.junit.Assert.*;

public final class TestedClassWithGenericsTest
{
   public interface Collaborator<T> { T getValue(); }

   @SuppressWarnings("unused")
   static class SUT<T>
   {
      final Collaborator<T> collaborator;
      final Iterable<Collaborator<T>> collaborators;
      Map<T, ?> values;
      Callable<T> action1;
      Callable<?> action2;

      SUT(Collaborator<T> c) { collaborator = c; collaborators = null; }

      SUT(Iterable<Collaborator<T>> collaborators, Callable<String> action)
      {
         collaborator = null;
         this.collaborators = collaborators;
         action2 = action;
      }

      <V extends CharSequence & Serializable> SUT(Map<T, V> values, Callable<?> action)
      {
         collaborator = null;
         collaborators = null;
         this.values = values;
         action2 = action;
      }
   }

   @Tested SUT<Integer> tested;
   @Injectable Collaborator<Integer> mockCollaborator;

   @Test
   public void useSUTInstantiatedWithGenericConstructorParameterInjectedFromConcreteInjectable()
   {
      assertSame(mockCollaborator, tested.collaborator);
      assertNull(tested.action1);
   }

   @Test
   public void useSUTInstantiatedWithGenericFieldInjectedFromConcreteInjectable(
      @Injectable Callable<String> mockAction1, @Injectable Callable<Integer> mockAction2)
   {
      assertSame(mockCollaborator, tested.collaborator);
      assertSame(mockAction2, tested.action1);
      assertNull(tested.action2);
   }

   @Test
   public void useSUTInstantiatedWithGenericConstructorParametersInjectedFromConcreteInjectables(
      @Injectable Iterable<Collaborator<Integer>> mockCollaborators, @Injectable Callable<String> mockAction)
   {
      assertNull(tested.collaborator);
      assertSame(mockCollaborators, tested.collaborators);
      assertNull(tested.action1);
      assertSame(mockAction, tested.action2);
   }

   @Test
   public void useSUTInstantiatedWithGenericConstructor(
      @Injectable Callable<?> mockAction, @Injectable Map<Integer, String> mockValues)
   {
      assertNull(tested.collaborator);
      assertNull(tested.collaborators);
      assertSame(mockValues, tested.values);
      assertNull(tested.action1);
      assertSame(mockAction, tested.action2);
   }
}
