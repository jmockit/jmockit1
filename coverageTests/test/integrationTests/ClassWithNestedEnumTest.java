package integrationTests;

import org.junit.*;

public final class ClassWithNestedEnumTest
{
   @Test
   public void useNestedEnumFromNestedClass() {
      ClassWithNestedEnum.NestedClass.useEnumFromOuterClass();
   }
}