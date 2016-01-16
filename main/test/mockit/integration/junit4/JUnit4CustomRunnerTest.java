/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.integration.junit4;

import java.lang.annotation.*;
import java.util.*;

import org.junit.*;
import org.junit.runner.*;
import org.junit.runners.*;
import org.junit.runners.model.*;
import static org.junit.Assert.*;

import mockit.*;

@RunWith(JUnit4CustomRunnerTest.CustomRunner.class)
public final class JUnit4CustomRunnerTest
{
   public static final class CustomRunner extends BlockJUnit4ClassRunner
   {
      public CustomRunner(Class<?> testClass) throws InitializationError { super(testClass); }

      @Override
      protected void validatePublicVoidNoArgMethods(
         Class<? extends Annotation> annotation, boolean isStatic, List<Throwable> errors) {}

      @Override
      protected Object createTest() throws Exception
      {
         testCount++;
         return super.createTest();
      }
   }

   static int testCount;

   @Before
   public void setUp()
   {
      assertTrue("Unexpected test count: " + testCount, testCount == 1 || testCount == 2);
   }

   @Test
   public void withAnnotatedParameters(@Mocked Runnable runnable, @Injectable Dependency dep)
   {
      assertNotNull(runnable);
      assertNotNull(dep);
   }

   @Test
   public void withNonAnnotatedParameters(Runnable runnable, Dependency dep)
   {
      assertNull(runnable);
      assertNull(dep);
   }
}
