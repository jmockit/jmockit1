/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package integrationTests;

import org.junit.*;

public final class ClassWithNestedEnumTest
{
   @Test
   public void useNestedEnumFromNestedClass()
   {
      ClassWithNestedEnum.NestedClass.useEnumFromOuterClass();
   }
}