package integrationTests;

import static org.junit.Assert.*;
import org.junit.*;

public final class ClassInRegularPackageTest
{
   @Test
   public void firstTest() {
      ClassInRegularPackage.NestedEnum value = ClassInRegularPackage.NestedEnum.First;
      ClassInRegularPackage obj = new ClassInRegularPackage();
      assertTrue(obj.doSomething(value));
   }

   @Test
   public void secondTest() {
      assertFalse(new ClassInRegularPackage().doSomething(ClassInRegularPackage.NestedEnum.Second));
   }
}
