/*
 * Copyright (c) 2006-2011 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package integrationTests;

import static org.junit.Assert.*;
import org.junit.*;

public final class ClassInRegularPackageTest
{
   @Test
   public void firstTest()
   {
      ClassInRegularPackage.NestedEnum value = ClassInRegularPackage.NestedEnum.First;
      ClassInRegularPackage obj = new ClassInRegularPackage();
      assertTrue(obj.doSomething(value));
   }

   @Test
   public void secondTest()
   {
      assertFalse(new ClassInRegularPackage().doSomething(ClassInRegularPackage.NestedEnum.Second));
   }
}
