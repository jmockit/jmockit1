import org.junit.*;

public final class ClassInDefaultPackageTest
{
   @Test
   public void firstTest() {
      new ClassInDefaultPackage().doSomething(ClassInDefaultPackage.NestedEnum.First);
   }

   @Test
   public void secondTest() {
      new ClassInDefaultPackage().doSomething(ClassInDefaultPackage.NestedEnum.Second);
   }
}
