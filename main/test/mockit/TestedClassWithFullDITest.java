/*
 * Copyright (c) 2006-2014 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit;

import org.junit.*;
import static org.junit.Assert.*;

public final class TestedClassWithFullDITest
{
   public static class TestedClass
   {
      Runnable dependencyToBeMocked;
      FirstLevelDependency dependency2;
      FirstLevelDependency dependency3;
      CommonDependency commonDependency;
      String name;
      final Integer number = null;
      boolean flag = true;
      Thread.State threadState;
   }

   public static class FirstLevelDependency
   {
      String firstLevelId;
      SecondLevelDependency dependency;
      CommonDependency commonDependency;
      Runnable dependencyToBeMocked;
   }

   public static class SecondLevelDependency { CommonDependency commonDependency; }
   public static class CommonDependency {}

   @Tested(fullyInitialized = true) TestedClass tested;
   @Injectable Runnable mockedDependency;

   @Test
   public void useFullyInitializedTestedObject(@Injectable("test") String firstLevelId)
   {
      assertEquals("test", tested.name);
      assertNull(tested.number);
      assertTrue(tested.flag);
      assertNull(tested.threadState);
      assertSame(mockedDependency, tested.dependencyToBeMocked);
      assertNotNull(tested.dependency2);
      assertEquals("test", tested.dependency2.firstLevelId);
      assertSame(tested.dependency2, tested.dependency3);
      assertNotNull(tested.commonDependency);
      assertNotNull(tested.dependency2.dependency);
      assertSame(tested.dependency2.dependency, tested.dependency3.dependency);
      assertSame(tested.commonDependency, tested.dependency2.commonDependency);
      assertSame(tested.commonDependency, tested.dependency3.commonDependency);
      assertSame(tested.commonDependency, tested.dependency2.dependency.commonDependency);
      assertSame(mockedDependency, tested.dependency2.dependencyToBeMocked);
      assertSame(mockedDependency, tested.dependency3.dependencyToBeMocked);
   }
}
