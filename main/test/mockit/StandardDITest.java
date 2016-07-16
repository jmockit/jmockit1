/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit;

import java.util.*;
import java.util.concurrent.*;
import javax.inject.*;

import static java.util.Collections.singletonList;

import org.junit.*;
import static org.junit.Assert.*;

public final class StandardDITest
{
   public static class TestedClass
   {
      @Inject static Runnable globalAction;

      private final Collaborator collaborator;
      @Inject private Collaborator collaborator1;
      Collaborator collaborator2;
      @Inject int someValue;
      @Inject private int anotherValue;

      String nonAnnotatedField;
      Callable<String> nonAnnotatedGenericField;

      @Inject
      public TestedClass(Collaborator collaborator) { this.collaborator = collaborator; }

      @SuppressWarnings("UnusedParameters")
      public TestedClass(Collaborator collaborator, int anotherValue) { throw new RuntimeException("Must not occur"); }
   }

   static class Collaborator { boolean b = true; }

   @Tested TestedClass tested1;
   @Injectable Collaborator collaborator; // for constructor injection
   @Injectable Collaborator collaborator1; // for field injection
   @Injectable("123") int someValue;
   @Injectable final int anotherValue = 45;
   @Injectable Callable<String> callable;

   static final class TestedClassWithNoAnnotatedConstructor
   {
      @Inject int value;
      @Inject String aText;
      String anotherText;
   }

   @Tested TestedClassWithNoAnnotatedConstructor tested2;
   @Injectable final String aText = "Abc";

   public static class TestedClassWithInjectOnConstructorOnly
   {
      String name;
      @Inject public TestedClassWithInjectOnConstructorOnly() {}
   }

   @Tested TestedClassWithInjectOnConstructorOnly tested3;

   @Test
   public void invokeInjectAnnotatedConstructorOnly()
   {
      assertSame(collaborator, tested1.collaborator);
      assertSame(collaborator1, tested1.collaborator1);
      assertNull(tested1.collaborator2);
      assertEquals(123, tested1.someValue);
      assertEquals(45, tested1.anotherValue);

      assertEquals(123, tested2.value);
   }

   @Test
   public void assignInjectAnnotatedFieldsAndAlsoNonAnnotatedOnes(
      @Injectable Collaborator collaborator2, @Injectable("67") int notToBeUsed)
   {
      assertSame(collaborator, tested1.collaborator);
      assertSame(collaborator1, tested1.collaborator1);
      assertSame(collaborator2, tested1.collaborator2);
      assertEquals(123, tested1.someValue);
      assertEquals(45, tested1.anotherValue);

      assertEquals(123, tested2.value);
   }

   @Test
   public void assignAnnotatedFieldEvenIfTestedClassHasNoAnnotatedConstructor(@Injectable("123") int value)
   {
      assertEquals(123, tested2.value);
   }

   @Injectable Runnable action;

   @Test
   public void assignAnnotatedStaticFieldDuringFieldInjection()
   {
      assertSame(action, TestedClass.globalAction);
   }

   @Test
   public void considerAnnotatedAndNonAnnotatedFieldsForInjection(@Injectable("XY") String text2)
   {
      assertEquals(aText, tested2.aText);
      assertNull(tested2.anotherText);
      assertEquals(aText, tested3.name);
   }

   static final class TestedClassWithProviders
   {
      final int port;
      final Collaborator collaborator;
      @Inject Provider<String> user;
      @Inject Provider<String> password;

      @Inject
      TestedClassWithProviders(Provider<Integer> port, Collaborator collaborator)
      {
         this.port = port.get();
         this.collaborator = collaborator;
      }
   }

   @Tested TestedClassWithProviders tested4;
   @Injectable Integer portNumber = 4567;
   @Injectable String user = "John";
   @Injectable String password = "123";

   @Test
   public void supportProviderFieldsAndParameters()
   {
      assertEquals(portNumber.intValue(), tested4.port);
      assertSame(collaborator, tested4.collaborator);
      assertEquals(user, tested4.user.get());
      assertEquals(password, tested4.password.get());
   }

   static final class TestedClassWithVarargsParameterForProviders
   {
      final Collaborator collaborator1;
      final Collaborator collaborator2;
      final List<Collaborator> optionalCollaborators = new ArrayList<Collaborator>();
      @Inject Provider<String> nameProvider;

      @Inject
      TestedClassWithVarargsParameterForProviders(Provider<Collaborator>... collaborators)
      {
         int n = collaborators.length;
         assertTrue(n > 1);

         collaborator1 = collaborators[0].get();
         assertSame(collaborator1, collaborators[0].get()); // default (singleton)

         collaborator2 = collaborators[2].get();
         assertNull(collaborators[2].get()); // recorded

         if (n > 3) {
            Collaborator col = collaborators[3].get();
            optionalCollaborators.add(col);
         }
      }
   }

   @Tested TestedClassWithVarargsParameterForProviders tested5;
   @Injectable Provider<Collaborator> collaboratorProvider;
   @Injectable Collaborator col3;

   @Before
   public void configureProviderUsedByConstructorOfTestedClass()
   {
      new Expectations() {{
         Collaborator[] collaborators = {col3, null};
         collaboratorProvider.get(); result = collaborators;
      }};
   }

   @Test
   public void supportVarargsParameterWithProviders(@Injectable final Provider<String> nameProvider)
   {
      final String[] names = {"John", "Mary"};
      new Expectations() {{ nameProvider.get(); result = names; }};

      assertSame(collaborator, tested5.collaborator1);
      assertNotNull(tested5.collaborator2);
      assertNotSame(tested5.collaborator1, tested5.collaborator2);
      assertEquals(singletonList(col3), tested5.optionalCollaborators);

      assertEquals(names[0], tested5.nameProvider.get());
      assertEquals(names[1], tested5.nameProvider.get());
   }

   @Test
   public void fieldsNotAnnotatedWithKnownDIAnnotationsShouldStillBeInjected()
   {
      assertEquals("Abc", tested1.nonAnnotatedField);
      assertSame(callable, tested1.nonAnnotatedGenericField);
   }
}
