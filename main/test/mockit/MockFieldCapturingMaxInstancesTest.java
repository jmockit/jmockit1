/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit;

import org.junit.*;
import static org.junit.Assert.*;

public final class MockFieldCapturingMaxInstancesTest
{
   static DerivedClass preexistingInstance;

   @BeforeClass
   public static void createAndUseInstancesBeforeCapturingIsInEffect()
   {
      assertEquals(1, new ServiceImpl().doSomething());

      preexistingInstance = new DerivedClass();
      assertEquals("", preexistingInstance.getStr());
      assertEquals("test1", new DerivedClass("test1").getStr());
   }

   public interface Service { int doSomething(); }
   static final class ServiceImpl implements Service { @Override public int doSomething() { return 1; } }

   @Capturing Service mock1;

   @Test
   public void mockFieldWithUnlimitedCapturing()
   {
      assertEquals(0, new ServiceImpl().doSomething());

      new Expectations() {{ mock1.doSomething(); returns(1, 2, 3); }};

      Service service1 = new ServiceImpl();
      assertEquals(1, service1.doSomething());

      Service service2 = new Service() { @Override public int doSomething() { return -1; } };
      assertEquals(2, service2.doSomething());

      Service service3 = new ServiceImpl();
      assertEquals(3, service3.doSomething());
   }

   static class BaseClass
   {
      final String str;
      BaseClass() { str = ""; }
      BaseClass(String str) { this.str = str; }
      final String getStr() { return str; }
   }

   static class DerivedClass extends BaseClass
   {
      DerivedClass() {}
      DerivedClass(String str) { super(str); }
   }

   @Capturing(maxInstances = 1) BaseClass mock2;

   @Test
   public void mockFieldWithCapturingLimitedToOneInstance()
   {
      new Expectations() {{ mock2.getStr(); result = "mocked"; }};

      assertNull(preexistingInstance.getStr());

      BaseClass service1 = new DerivedClass("test 1");
      assertNull(service1.str);
      assertEquals("mocked", service1.getStr());

      BaseClass service2 = new BaseClass("test 2");
      assertNull(service2.str);
      assertNull(service2.getStr());

      new Verifications() {{
         preexistingInstance.getStr(); times = 1;
      }};
   }

   @Capturing(maxInstances = 1) BaseClass mock3;

   @Test
   public void secondMockFieldWithCapturingLimitedToOneInstance()
   {
      new Expectations() {{
         mock2.getStr(); result = "mocked1"; times = 1;
         mock3.getStr(); result = "mocked2"; times = 1;
      }};

      assertNull(preexistingInstance.getStr());

      BaseClass service1 = new DerivedClass("test 1");
      assertNull(service1.str);
      assertEquals("mocked1", service1.getStr());

      BaseClass service2 = new BaseClass("test 2");
      assertNull(service2.str);
      assertEquals("mocked2", service2.getStr());

      BaseClass service3 = new DerivedClass("test 3");
      assertNull(service3.str);
      assertNull(service3.getStr());
   }
}