/*
 * Copyright (c) 2006-2015 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit;

import java.lang.annotation.*;

import org.junit.*;
import org.junit.runners.*;
import static org.junit.Assert.*;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public final class TestedClassWithFullDITest
{
   public static class TestedClass
   {
      Runnable dependencyToBeMocked;
      FirstLevelDependency dependency2;
      FirstLevelDependency dependency3;
      CommonDependency commonDependency;
      String name;
      StringBuilder description;
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

   @Tested(fullyInitialized = true)
   @Retention(RetentionPolicy.RUNTIME)
   @Target(ElementType.FIELD)
   public @interface IntegrationTested {}

   @IntegrationTested TestedClass tested;
   @Injectable Runnable mockedDependency;

   @Test
   public void useFullyInitializedTestedObjectWithNoInjectableForFirstLevelDependency()
   {
      assertEquals("", tested.name);
      assertSame(tested.commonDependency, tested.dependency2.dependency.commonDependency);
   }

   @Test
   public void useFullyInitializedTestedObjectWithValueForFirstLevelDependency(@Injectable("test") String id)
   {
      assertEquals("test", tested.name);
      assertEquals(0, tested.description.length());
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
