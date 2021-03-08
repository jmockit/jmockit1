package mockit;

import org.junit.*;
import static org.junit.Assert.*;

public final class FakedClassWithSuperClassTest
{
   public static class BaseClass {
      protected int doSomething() { return 123; }
      protected int doSomethingElse() { return 1; }
   }

   public static class Subclass extends BaseClass {
      BaseClass getInstance() { return this; }
      @Override protected int doSomethingElse() { return super.doSomethingElse() + 1; }
   }

   public static final class FakeForSubclass extends MockUp<Subclass> {
      @Mock public int doSomething() { return 1; }
      @Mock public int doSomethingElse(Invocation inv) { return inv.proceed(); }
   }

   @Test
   public void fakeOnlyInstancesOfTheClassSpecifiedToBeFaked() {
      BaseClass d = new Subclass();
      assertEquals(123, d.doSomething());
      assertEquals(2, d.doSomethingElse());

      new FakeForSubclass();

      assertEquals(1, d.doSomething());
      assertEquals(123, new BaseClass().doSomething());
      assertEquals(1, new Subclass().doSomething());
      assertEquals(123, new BaseClass() {}.doSomething());
      assertEquals(1, new Subclass() {}.doSomething());
      assertEquals(1, new BaseClass().doSomethingElse());
      assertEquals(2, new Subclass().doSomethingElse());
   }

   @Test
   public void fakeOnlyInstancesOfTheClassSpecifiedToBeFaked_usingFakeMethodBridge() {
      BaseClass d = new Subclass();
      assertEquals(123, d.doSomething());
      assertEquals(2, d.doSomethingElse());

      new MockUp<Subclass>() {
         @Mock int doSomething() { return 2; }
         @Mock public int doSomethingElse(Invocation inv) { return inv.proceed(); }
      };

      assertEquals(123, new BaseClass().doSomething());
      assertEquals(2, d.doSomething());
      assertEquals(2, new Subclass().doSomething());
      assertEquals(123, new BaseClass() {}.doSomething());
      assertEquals(2, new Subclass() {}.doSomething());
      assertEquals(1, new BaseClass().doSomethingElse());
      assertEquals(2, new Subclass().doSomethingElse());
   }
}
