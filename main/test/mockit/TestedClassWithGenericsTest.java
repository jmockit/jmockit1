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
   static class SUTWithUnboundedTypeParameter<T>
   {
      T value;
      final Collaborator<T> collaborator;
      final Iterable<Collaborator<T>> collaborators;
      Map<T, ?> values;
      Callable<T> action1;
      Callable<?> action2;

      SUTWithUnboundedTypeParameter(Collaborator<T> c) { collaborator = c; collaborators = null; }

      SUTWithUnboundedTypeParameter(Iterable<Collaborator<T>> collaborators, Callable<String> action)
      {
         collaborator = null;
         this.collaborators = collaborators;
         action2 = action;
      }

      <V extends CharSequence & Serializable> SUTWithUnboundedTypeParameter(Map<T, V> values, Callable<?> action)
      {
         collaborator = null;
         collaborators = null;
         this.values = values;
         action2 = action;
      }
   }

   @Tested SUTWithUnboundedTypeParameter<Integer> tested1;
   @Injectable final Integer numberToInject = 123;
   @Injectable Collaborator<Integer> mockCollaborator;

   @Test
   public void useSUTWithGenericFieldInjectedFromConcreteInjectable()
   {
      assertSame(numberToInject, tested1.value);
   }

   @Test
   public void useSUTInstantiatedWithGenericConstructorParameterInjectedFromConcreteInjectable()
   {
      assertSame(mockCollaborator, tested1.collaborator);
      assertNull(tested1.action1);
   }

   @Test
   public void useSUTInstantiatedWithGenericFieldInjectedFromConcreteInjectable(
      @Injectable Callable<String> mockAction1, @Injectable Callable<Integer> action1)
   {
      assertSame(mockCollaborator, tested1.collaborator);
      assertSame(action1, tested1.action1);
      assertNull(tested1.action2);
   }

   @Test
   public void useSUTInstantiatedWithGenericConstructorParametersInjectedFromConcreteInjectables(
      @Injectable Iterable<Collaborator<Integer>> mockCollaborators, @Injectable Callable<String> mockAction)
   {
      assertNull(tested1.collaborator);
      assertSame(mockCollaborators, tested1.collaborators);
      assertNull(tested1.action1);
      assertSame(mockAction, tested1.action2);
   }

   @Test
   public void useSUTInstantiatedWithGenericConstructor(
      @Injectable Callable<?> mockAction, @Injectable Map<Integer, String> mockValues)
   {
      assertNull(tested1.collaborator);
      assertNull(tested1.collaborators);
      assertSame(mockValues, tested1.values);
      assertNull(tested1.action1);
      assertSame(mockAction, tested1.action2);
   }

   static class GenericClass<T> { T value; }
   static class SUTWithBoundedTypeParameter<N extends Number, C extends CharSequence>
   {
      C textValue;
      N numberValue;
      GenericClass<N> collaborator;
      Callable<C> action;
   }

   @Tested SUTWithBoundedTypeParameter<Integer, String> tested2;
   @Tested SUTWithBoundedTypeParameter<Number, CharSequence> tested3;
   @Tested SUTWithBoundedTypeParameter<?, ?> tested4;
   @Tested SUTWithBoundedTypeParameter<Long, StringBuilder> tested5;
   @Injectable GenericClass<? extends Number> collaborator;

   @Test
   public void useSUTDeclaredWithTypeBound(@Injectable("test") String name, @Injectable Callable<String> textAction)
   {
      assertSame(numberToInject, tested2.numberValue);
      assertSame(name, tested2.textValue);
      assertSame(collaborator, tested2.collaborator);
      assertSame(textAction, tested2.action);

      assertSame(numberToInject, tested3.numberValue);
      assertSame(name, tested3.textValue);
      assertSame(collaborator, tested3.collaborator);
      assertSame(textAction, tested3.action);

      assertSame(numberToInject, tested4.numberValue);
      assertSame(name, tested4.textValue);
      assertSame(collaborator, tested4.collaborator);
      assertSame(textAction, tested4.action);

      assertNull(tested5.numberValue);
      assertNull(tested5.textValue);
      assertSame(collaborator, tested5.collaborator);
      assertNull(tested5.action);
   }

   @Test
   public void useSUTDeclaredWithTypeBoundHavingNonMatchingInjectableWithWildcard(
      @Injectable Callable<? extends Number> action)
   {
      assertNull(tested2.action);
      assertNull(tested3.action);
      assertNull(tested4.action);
      assertNull(tested5.action);
   }
}
