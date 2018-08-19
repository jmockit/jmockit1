package otherTests.testng;

import static org.junit.Assert.assertEquals;
import static org.testng.Assert.*;
import org.testng.annotations.*;

import mockit.*;
import mockit.integration.*;

import otherTests.*;

public final class TestNGExpectationsTest
{
   @Tested TestedClass tested;
   @Injectable MockedClass dependency;
   @Injectable MockedClass mock2;

   @BeforeMethod
   void setUpTestMethod1() {
      new Expectations() {{
         mock2.doSomethingElse(anyInt);
         result = true;
         minTimes = 0;
      }};
   }

   @BeforeMethod
   void setUpTestMethod2() {
      new Expectations() {{ dependency.getValue(); result = "mocked"; minTimes = 0; }};
   }

   @AfterMethod
   public void tearDownTestMethod() {
      new Verifications() {{ mock2.doSomethingElse(6); }};
   }

   @Test
   public void testSomething() {
      new Expectations() {{ dependency.doSomething(anyInt); result = true; times = 2; }};

      assertTrue(dependency.doSomething(5));
      assertEquals("mocked", dependency.getValue());
      assertTrue(tested.doSomething(-5));
      assertTrue(mock2.doSomethingElse(6));

      new FullVerifications(dependency) {{ dependency.getValue(); }};
   }

   @Test
   public void testSomethingElse() {
      assertEquals("mocked", dependency.getValue());
      assertFalse(tested.doSomething(41));
      assertTrue(mock2.doSomethingElse(6));

      new VerificationsInOrder() {{
         dependency.getValue();
         dependency.doSomething(anyInt);
      }};
   }
}
