/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit;

import javax.inject.*;

import org.junit.*;
import static org.junit.Assert.*;

public final class TestedFieldExtractionTest
{
   static class Dependency {}

   static class TestedClass
   {
      @Inject @Named("first") Dependency dep1;
      @Inject @Named("second") Dependency dep2;
   }

   @Tested(fullyInitialized = true) TestedClass tested;
   @Tested Dependency first;
   @Tested Dependency second;

   @Test
   public void extractMultipleInjectedFieldsOfSameTypeIntoSeparateTestedFields()
   {
      assertNotNull(tested.dep1);
      assertNotNull(tested.dep2);
      assertNotSame(tested.dep1, tested.dep2);
      assertSame(tested.dep1, first);
      assertSame(tested.dep2, second);
   }
}
