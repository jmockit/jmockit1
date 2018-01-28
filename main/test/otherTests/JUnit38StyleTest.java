package otherTests;

import mockit.*;

import junit.framework.*;

public final class JUnit38StyleTest extends TestCase
{
   @Override
   public void setUp() {
      useClassMockedInPreviousJUnit4TestClass();
   }

   public void testUseClassMockedInPreviousJUnit4TestClass() {
      useClassMockedInPreviousJUnit4TestClass();
   }

   void useClassMockedInPreviousJUnit4TestClass() {
      ClassWithObjectOverrides test = new ClassWithObjectOverrides("test");
      //noinspection UseOfObsoleteAssert
      assertEquals("test", test.toString());
   }
}
