/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit;

import org.junit.*;
import static org.junit.Assert.*;

public final class FakedClassWithSuperClassTest
{
   static class BaseClass { protected int doSomething() { return 123; } }
   public static class Subclass extends BaseClass { BaseClass getInstance() { return this; } }

   public static final class FakeForSubclass extends MockUp<Subclass> {
      @Mock public int doSomething() { return 1; }
   }

   @Test
   public void fakeOnlyInstancesOfTheClassSpecifiedToBeFaked()
   {
      BaseClass d = new Subclass();
      assertEquals(123, d.doSomething());

      new FakeForSubclass();

      assertEquals(1, d.doSomething());
      assertEquals(123, new BaseClass().doSomething());
      assertEquals(1, new Subclass().doSomething());
      assertEquals(123, new BaseClass() {}.doSomething());
      assertEquals(1, new Subclass() {}.doSomething());
   }

   @Test
   public void fakeOnlyInstancesOfTheClassSpecifiedToBeFaked_usingFakeMethodBridge()
   {
      BaseClass d = new Subclass();
      assertEquals(123, d.doSomething());

      new MockUp<Subclass>() {
         @Mock int doSomething() { return 2; }
      };

      assertEquals(123, new BaseClass().doSomething());
      assertEquals(2, d.doSomething());
      assertEquals(2, new Subclass().doSomething());
      assertEquals(123, new BaseClass() {}.doSomething());
      assertEquals(2, new Subclass() {}.doSomething());
   }
}
