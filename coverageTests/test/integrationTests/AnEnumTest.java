package integrationTests;

import org.junit.*;
import static org.junit.Assert.*;

public final class AnEnumTest extends CoverageTest
{
   AnEnum tested;

   @Test
   public void useAnEnum() {
      tested = AnEnum.OneValue;

      assertEquals(100, fileData.lineCoverageInfo.getCoveragePercentage());
      assertEquals(100, fileData.pathCoverageInfo.getCoveragePercentage());
   }
}