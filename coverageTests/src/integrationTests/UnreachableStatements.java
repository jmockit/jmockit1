package integrationTests;

public final class UnreachableStatements
{
   void nonBranchingMethodWithUnreachableLines()
   {
      int a = 1;
      assert false;
      System.gc();
   }

   void branchingMethodWithUnreachableLines(int a)
   {
      if (a > 0) {
         assert false;
         System.gc();
      }

      System.runFinalization();
   }
}