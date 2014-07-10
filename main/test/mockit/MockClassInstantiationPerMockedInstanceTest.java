/*
 * Copyright (c) 2006-2014 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit;

import java.util.*;

import org.junit.*;
import static org.junit.Assert.*;

public final class MockClassInstantiationPerMockedInstanceTest
{
   static final class ClassToMock
   {
      final int value;
      ClassToMock(int value) { this.value = value; }
      int performComputation() { return -1; }
   }

   static final class MockClass extends MockUp<ClassToMock>
   {
      final Map<ClassToMock, Integer> value = new HashMap<ClassToMock, Integer>();

      @Mock void $init(Invocation inv, int value)
      {
         ClassToMock mockedInstance = inv.getInvokedInstance();
         this.value.put(mockedInstance, value);
      }

      @Mock int performComputation(Invocation inv)
      {
         ClassToMock mockedInstance = inv.getInvokedInstance();
         return value.get(mockedInstance);
      }
   }

   @BeforeClass
   public static void setUpMocks()
   {
      new MockClass();
   }

   @Test
   public void mockInstancePerMockedInstanceInAllScopes()
   {
      ClassToMock ro1 = new ClassToMock(123);
      assertEquals(123, ro1.performComputation());
      assertEquals(123, ro1.performComputation());

      ClassToMock ro2 = new ClassToMock(-45);
      assertEquals(-45, ro2.performComputation());
   }
}