/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit;

import java.io.*;
import java.nio.*;

import javax.annotation.*;

import org.junit.*;
import org.junit.runners.*;
import static org.junit.Assert.*;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public final class FakingBaseTypesTest
{
   static class BeforeClassBaseAction { protected int perform() { return -1; } }
   static class BeforeClassAction extends BeforeClassBaseAction { @Override protected int perform() { return 12; } }

   @BeforeClass
   public static <T extends BeforeClassBaseAction> void applyFakeForAllTests()
   {
      new MockUp<T>() {
         @Mock int perform() { return 34; }
      };
   }

   @AfterClass
   public static void verifyFakeForAllTestsIsInEffect()
   {
      int i1 = new BeforeClassAction().perform();
      int i2 = new BeforeClassBaseAction().perform();

      assertEquals(34, i1);
      assertEquals(34, i2);
   }

   public interface IBeforeAction { @SuppressWarnings("unused") int perform(); }
   static class BeforeAction implements IBeforeAction { @Override public int perform() { return 123; } }

   @Before
   public <T extends IBeforeAction> void applyFakeForEachTest()
   {
      new MockUp<T>() {
         @Mock int perform() { return 56; }
      };
   }

   @After
   public void verifyFakeForEachTestIsInEffect()
   {
      int i = new BeforeAction().perform();
      assertEquals(56, i);
   }

   public interface IAction
   {
      int perform(int i);
      boolean notToBeFaked();
   }

   public static class ActionImpl1 implements IAction
   {
      @Override public int perform(int i) { return i - 1; }
      @Override public boolean notToBeFaked() { return true; }
   }

   static final class ActionImpl2 implements IAction
   {
      @Override public int perform(int i) { return i - 2; }
      @Override public boolean notToBeFaked() { return true; }
   }

   IAction actionI;

   @Test
   public <T extends IAction> void test3_fakeInterfaceImplementationClassesUsingAnonymousFake()
   {
      actionI = new ActionImpl1();

      new MockUp<T>() {
         @Mock
         int perform(int i) { return i + 1; }
      };

      assertEquals(2, actionI.perform(1));
      assertTrue(actionI.notToBeFaked());

      ActionImpl2 impl2 = new ActionImpl2();
      assertEquals(3, impl2.perform(2));
      assertTrue(impl2.notToBeFaked());
   }

   public interface TestInterface { String getData(); }

   public static final class FakeTestInterface<T extends TestInterface> extends MockUp<T>
   {
      @Mock
      public String getData(Invocation inv) { return "faked" + inv.proceed(); }
   }

   @Test
   public void fakeAllClassesImplementingAnInterfaceUsingNamedFakeWithInvocationParameter()
   {
      TestInterface impl1 = new TestInterface() { @Override public String getData() { return "1"; } };
      TestInterface impl2 = new TestInterface() { @Override public String getData() { return "2"; } };
      new FakeTestInterface();

      String faked1 = impl1.getData();
      String faked2 = impl2.getData();

      assertEquals("faked1", faked1);
      assertEquals("faked2", faked2);
   }

   public abstract static class BaseAction
   {
      protected abstract int perform(int i);
      public int toBeFakedAsWell() { return -1; }
      int notToBeFaked() { return 1; }
   }

   static final class ConcreteAction1 extends BaseAction
   {
      @Override public int perform(int i) { return i - 1; }
   }

   static class ConcreteAction2 extends BaseAction
   {
      @Override protected int perform(int i) { return i - 2; }
      @Override public int toBeFakedAsWell() { return super.toBeFakedAsWell() - 1; }
      @Override int notToBeFaked() { return super.notToBeFaked() + 1; }
   }

   static class ConcreteAction3 extends ConcreteAction2
   {
      @Override public int perform(int i) { return i - 3; }
      @Override public int toBeFakedAsWell() { return -3; }
      @Override final int notToBeFaked() { return 3; }
   }

   BaseAction actionB;

   @Test
   public <T extends BaseAction> void test4_fakeConcreteSubclassesUsingAnonymousFake()
   {
      actionB = new ConcreteAction1();

      new MockUp<T>() {
         @Mock int perform(int i) { return i + 1; }
         @Mock int toBeFakedAsWell() { return 123; }
      };

      assertEquals(2, actionB.perform(1));
      assertEquals(123, actionB.toBeFakedAsWell());
      assertEquals(1, actionB.notToBeFaked());

      ConcreteAction2 action2 = new ConcreteAction2();
      assertEquals(3, action2.perform(2));
      assertEquals(123, action2.toBeFakedAsWell());
      assertEquals(2, action2.notToBeFaked());

      ConcreteAction3 action3 = new ConcreteAction3();
      assertEquals(4, action3.perform(3));
      assertEquals(123, action3.toBeFakedAsWell());
      assertEquals(3, action3.notToBeFaked());
   }

   @After
   public void checkImplementationClassesAreNoLongerFaked()
   {
      if (actionI != null) {
         assertEquals(-1, actionI.perform(0));
      }

      assertEquals(-2, new ActionImpl2().perform(0));

      if (actionB != null) {
         assertEquals(-1, actionB.perform(0));
      }

      assertEquals(-2, new ConcreteAction2().perform(0));
      assertEquals(-3, new ConcreteAction3().perform(0));
   }

   static final class FakeInterface<T extends IAction> extends MockUp<T>
   {
      @Mock
      int perform(int i) { return i + 2; }
   }

   @Test
   public void test5_fakeInterfaceImplementationClassesUsingNamedFake()
   {
      new FakeInterface();

      actionI = new ActionImpl1();
      assertEquals(3, actionI.perform(1));
      assertEquals(4, new ActionImpl2().perform(2));
   }

   static final class FakeBaseClass<T extends BaseAction> extends MockUp<T>
   {
      @Mock
      int perform(int i) { return i + 3; }
   }

   @Test
   public void test6_fakeConcreteSubclassesUsingNamedFake()
   {
      new FakeBaseClass();

      actionB = new ConcreteAction1();
      assertEquals(4, actionB.perform(1));
      assertEquals(5, new ConcreteAction2().perform(2));
      assertEquals(6, new ConcreteAction3().perform(3));
   }

   interface GenericIAction<N extends Number> { N perform(N n); }

   @Test
   public <M extends GenericIAction<Number>> void test7_fakeImplementationsOfGenericInterface()
   {
      GenericIAction<Number> actionNumber = new GenericIAction<Number>() {
         @Override public Number perform(Number n) { return n.intValue() + 1; }
      };

      GenericIAction<Integer> actionInt = new GenericIAction<Integer>() {
         @Override public Integer perform(Integer n) { return n + 1; }
      };

      GenericIAction<Long> actionL = new GenericIAction<Long>() {
         @Override public Long perform(Long n) { return n + 2; }
      };

      new MockUp<M>() {
         @Mock Number perform(Number n) { return n.intValue() - 1; }
      };

      Number n = actionNumber.perform(1);
      assertEquals(0, n); // mocked

      int i = actionInt.perform(2);
      assertEquals(3, i); // not mocked

      long l = actionL.perform(3L);
      assertEquals(5, l); // not mocked
   }

   @Test
   public <R extends Readable> void test8_excludeJREClassesFromFakingForSafety() throws Exception
   {
      new MockUp<R>() {
         @Mock
         int read(CharBuffer cb) { return 123; }
      };

      CharBuffer buf = CharBuffer.allocate(10);
      int r1 = new Readable() { @Override public int read(@Nonnull CharBuffer cb) { return 1; } }.read(buf);
      assertEquals(123, r1);

      int r2 = new StringReader("test").read(buf);
      assertEquals(4, r2);
   }
}
