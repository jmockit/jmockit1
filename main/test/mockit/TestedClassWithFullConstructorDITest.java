/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit;

import javax.inject.*;

import org.junit.*;
import static org.junit.Assert.*;

import org.springframework.beans.factory.annotation.Qualifier;

public final class TestedClassWithFullConstructorDITest
{
   public interface Dependency {}
   public static final class DependencyImpl implements Dependency {}
   public static class Collaborator {}

   @SuppressWarnings("unused")
   public static final class TestedClassWithSinglePublicConstructor
   {
      final Dependency dependency;
      final Collaborator collaborator1;
      Collaborator collaborator2;

      public TestedClassWithSinglePublicConstructor(Dependency dependency, Collaborator collaborator)
      {
         this.dependency = dependency;
         collaborator1 = collaborator;
      }

      TestedClassWithSinglePublicConstructor()
      {
         dependency = null;
         collaborator1 = null;
      }
   }

   @Tested DependencyImpl dep;
   @Tested(fullyInitialized = true) TestedClassWithSinglePublicConstructor tested1;

   @Test
   public void verifyInstantiationOfTestedObjectsThroughPublicConstructor()
   {
      assertTrue(tested1.dependency instanceof DependencyImpl);
      assertNotNull(tested1.collaborator1);
      assertSame(tested1.collaborator1, tested1.collaborator2);
   }

   public static final class TestedClassWithAnnotatedConstructor
   {
      int i;
      String s;
      Dependency dependency;
      Collaborator collaborator1;
      @Inject Collaborator collaborator2;

      @SuppressWarnings("unused")
      public TestedClassWithAnnotatedConstructor() {}

      @Inject
      TestedClassWithAnnotatedConstructor(int i, String s, Dependency dependency, Collaborator collaborator)
      {
         this.i = i;
         this.s = s;
         this.dependency = dependency;
         collaborator1 = collaborator;
      }
   }

   @Tested(fullyInitialized = true) TestedClassWithAnnotatedConstructor tested2;
   @Injectable final int number = 123;
   @Injectable final String text = "text";

   @Test
   public void verifyInstantiationOfTestedObjectThroughAnnotatedConstructor()
   {
      assertNotNull(tested2);
      assertEquals(123, tested2.i);
      assertEquals("text", tested2.s);
      assertTrue(tested2.dependency instanceof DependencyImpl);
      assertNotNull(tested2.collaborator1);
      assertSame(tested2.collaborator1, tested2.collaborator2);
   }

   static class TestedClassWithQualifiedConstructorParameters
   {
      final Collaborator col1;
      final Collaborator col2;

      TestedClassWithQualifiedConstructorParameters(@Named("one") Collaborator p1, @Qualifier("two") Collaborator p2)
      {
         col1 = p1;
         col2 = p2;
      }
   }

   @Tested(fullyInitialized = true) TestedClassWithQualifiedConstructorParameters tested3;

   @Test
   public void verifyInstantiationOfTestedClassWithQualifiedConstructorParameters()
   {
      assertNotNull(tested3.col1);
      assertNotSame(tested3.col1, tested3.col2);
   }

   static class TestedClassWithDependencyHavingConstructorParameter
   {
      final DependencyWithConstructorParameter dependency;

      TestedClassWithDependencyHavingConstructorParameter(DependencyWithConstructorParameter dependency)
      {
         this.dependency = dependency;
      }
   }

   static class DependencyWithConstructorParameter
   {
      final String par;
      DependencyWithConstructorParameter(String par1) { par = par1; }
   }

   @Tested(fullyInitialized = true) TestedClassWithDependencyHavingConstructorParameter tested4;

   @Test
   public void verifyRecursiveInstantiationOfDependencyWithConstructorParameter()
   {
      assertEquals("text", tested4.dependency.par);
   }
}
