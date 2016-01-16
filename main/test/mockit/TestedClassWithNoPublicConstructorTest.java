/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit;

import org.junit.*;
import static org.junit.Assert.*;

public final class TestedClassWithNoPublicConstructorTest
{
   @SuppressWarnings("UnusedDeclaration")
   public static final class TestedClassWithPackagePrivateConstructor
   {
      private TestedClassWithPackagePrivateConstructor(int... values) { throw new RuntimeException("Must not occur"); }

      TestedClassWithPackagePrivateConstructor(int i, Collaborator collaborator)
      {
         assertEquals(123, i);
         assertNotNull(collaborator);
      }

      private TestedClassWithPackagePrivateConstructor(int i, Collaborator collaborator, String s)
      {
         throw new RuntimeException("Must not occur");
      }
   }

   @SuppressWarnings("UnusedDeclaration")
   static class TestedClassWithPrivateConstructor
   {
      private TestedClassWithPrivateConstructor() { throw new RuntimeException("Must not occur"); }
      private TestedClassWithPrivateConstructor(Collaborator collaborator) { assertNotNull(collaborator); }
   }

   static class Collaborator { static void doSomething() {} }

   @Tested TestedClassWithPackagePrivateConstructor tested1;
   @Tested TestedClassWithPrivateConstructor tested2;
   @Injectable int i = 123;
   @Injectable Collaborator collaborator;

   @Test
   public void verifyInstantiationOfTestedObjectsThroughInjectedConstructors()
   {
      assertNotNull(tested1);
      assertNotNull(tested2);
   }
}
