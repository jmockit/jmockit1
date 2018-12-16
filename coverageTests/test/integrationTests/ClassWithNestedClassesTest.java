package integrationTests;

import org.junit.*;
import static org.junit.Assert.*;

public final class ClassWithNestedClassesTest extends CoverageTest
{
   final ClassWithNestedClasses tested = null;

   @Test
   public void exerciseNestedClasses() {
      ClassWithNestedClasses.doSomething();
      ClassWithNestedClasses.methodContainingAnonymousClass(1);

      assertEquals(12, fileData.lineCoverageInfo.getExecutableLineCount());
      assertEquals(64, fileData.lineCoverageInfo.getCoveragePercentage());
      assertEquals(14, fileData.lineCoverageInfo.getTotalItems());
      assertEquals( 9, fileData.lineCoverageInfo.getCoveredItems());
   }
}
