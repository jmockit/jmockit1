package integrationTests.otherControlStructures;

import org.junit.*;

import integrationTests.*;

public final class TryCatchFinallyStatementsTest extends CoverageTest
{
   TryCatchFinallyStatements tested;

   @Test
   public void tryCatch() {
      tested.tryCatch();
   }

   @Test
   public void tryCatchWhichThrowsAndCatchesException() {
      tested.tryCatchWhichThrowsAndCatchesException();
   }
}
