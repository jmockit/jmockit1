/*
 * Copyright (c) 2006-2011 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
import org.junit.*;

public final class ClassInDefaultPackageTest
{
   @Test
   public void firstTest()
   {
      new ClassInDefaultPackage().doSomething(ClassInDefaultPackage.NestedEnum.First);
   }

   @Test
   public void secondTest()
   {
      new ClassInDefaultPackage().doSomething(ClassInDefaultPackage.NestedEnum.Second);
   }
}
