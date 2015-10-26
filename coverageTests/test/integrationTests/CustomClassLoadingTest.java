/*
 * Copyright (c) 2006-2015 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package integrationTests;

import java.util.*;

import org.junit.*;
import org.junit.runner.*;
import static org.junit.Assert.*;

import mockit.coverage.data.*;

import org.hamcrest.*;

@RunWith(CustomRunner.class)
public final class CustomClassLoadingTest extends CoverageTest
{
   Object tested;

   @After
   public void resetFileData()
   {
      fileData = null;
   }

   @Test
   public void runCodeInClassReloadedOnCustomClassLoader()
   {
      ClassLoader thisCL = getClass().getClassLoader();
      IfElseStatements ifElse = new IfElseStatements();
      ClassLoader customCL = ifElse.methodToBeCalledFromCustomRunnerTest("TEST");

      assertNotSame(ClassLoader.getSystemClassLoader(), thisCL);
      assertSame(thisCL, customCL);

      tested = ifElse;
      assertLines(189, 193, 2);
      assertLine(189, 1, 1, 1);
      assertLine(191, 1, 1, 1);
      assertLine(195, 1, 1, 1);

      findMethodData(189);
      assertPaths(2, 1, 1);
      assertPath(4, 0);
      assertPath(5, 1);

      assertInstanceFieldUncovered("instanceField");
   }

   @Ignore("test fails, don't know whhy")
   @Test
   public void exerciseClassThatIsOnlyUsedHere()
   {
      ClassLoadedByCustomLoaderOnly obj = new ClassLoadedByCustomLoaderOnly("test");
      String value = obj.getValue();

      assertEquals("test", value);

      tested = obj;
      assertLines(9, 9, 1);
      assertLine(9, 1, 1, 1);

      findMethodData(9);
      assertPaths(1, 1, 1);
      assertPath(2, 1);
   }

   @Test
   public void exerciseClassFromNonJREJarFile()
   {
      CoreMatchers.anything();

      Map<String, FileCoverageData> fileToFileData = CoverageData.instance().getRawFileToFileData();
      assertFalse(fileToFileData.containsKey("org/hamcrest/CoreMatchers.java"));
   }
}
