package integrationTests;

@SuppressWarnings("ControlFlowStatementWithoutBraces")
public final class IfElseStatements
{
   void simpleIf(boolean b)
   {
      if (b) {
         System.gc(); System.runFinalization();
      }
   }

   void ifAndElse(boolean b)
   {
      if (b) {
         System.gc();
      }
      else {
         System.runFinalization();
      }
   }

   void singleLineIf(boolean b)
   {
      if (b) System.gc();
   }

   void singleLineIfAndElse(boolean b)
   {
      if (b) System.gc(); else System.runFinalization();
   }

   void methodWithFourDifferentPathsAndSimpleLines(boolean b, int i)
   {
      if (b) {
         System.gc();
      }
      else {
         System.runFinalization();
      }

      if (i > 0) {
         System.gc();
      }
   }

   void methodWithFourDifferentPathsAndSegmentedLines(boolean b, int i)
   {
      if (b) { System.gc(); } else { System.runFinalization(); }

      if (i > 0) { System.gc(); }
      else { System.runFinalization(); }
   }

   boolean ifElseWithComplexBooleanCondition(boolean a, boolean b)
   {
      //noinspection RedundantIfStatement
      if (a || b)
         return true;
      else
         return false;
   }

   // Must return the same value of x as it was called with. Some paths will fail that requirement.
   @SuppressWarnings({"AssignmentToMethodParameter"})
   int returnInput(int x, boolean a, boolean b, boolean c)
   {
      if (a) {
         x++;
      }

      if (b) {
         x--;
      }

      if (c) {
         //noinspection SillyAssignment
         x = x;
      }

      return x;
   }

   int nestedIf(boolean a, boolean b)
   {
      int i = 1;

      if (a) {
         if (b) {
            i = 2;
         }
      }

      return i;
   }

   int ifElseWithNestedIf(boolean a, boolean b)
   {
      int i = 1;

      if (a) {
         if (b) {
            i = 2;
         }
      }
      else {
         return 3;
      }

      return i;
   }

   int nestedIfElse(boolean a, boolean b)
   {
      int i = 1;

      if (a) {
         if (b) {
            i = 2;
         }
         else {
            i = 3;
         }
      }
      else if (b) {
         i = 4;
      }

      return i;
   }

   /**
    * Block comment with method signature: infeasiblePaths(boolean a).
    */
   void infeasiblePaths(boolean a)
   {
      if (a) {
         System.gc();
      }

      if (a) {
         System.runFinalization();
      }
   }

   int anotherSingleLineIfAndElse(boolean b)
   {
      int r; if (b) r = 1; else r = 2; return r;
   }

   int yetAnotherSingleLineIfAndElse(boolean b)
   {
      if (b) return 1; else return 2;
   }

   void ifWithBooleanAndOperator(boolean b1, boolean b2)
   {
      if (b1 && b2) {
         System.gc();
      }
   }

   void ifWithBooleanOrOperator(boolean b1, boolean b2)
   {
      if (b1 || b2) {
         System.gc();
      }
   }

   void anotherIfWithBooleanAndOperator(boolean b1, boolean b2)
   {
      if (b1 && b2) {
         System.gc();
      }
   }

   void ifSpanningMultipleLines(boolean b, int i)
   {
      if (
         b ||
         i > 0
      ) {
         System.gc();
      }
   }
}