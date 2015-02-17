package integrationTests.otherControlStructures;

public final class TryCatchFinallyStatements
{
   void tryCatch()
   {
      try {
         System.gc();
      }
      catch (Exception e) {
         e.printStackTrace();
      }
   }

   boolean tryCatchWhichThrowsAndCatchesException()
   {
      try {
         throw new RuntimeException("testing");
      }
      catch (RuntimeException e) {
         return true;
      }
   }

   int regularTryFinally(boolean b)
   {
      try {
         if (b)
            return 1;
         return 0;
      }
      finally {
         System.gc();
      }
   }

   boolean finallyBlockWhichCannotCompleteNormally(boolean b) // very different from javac with Eclipse compiler
   {
      while (b) {
         try {
            return true;
         }
         finally {
            break;
         }
      }
      return false;
   }

   int whileTrueWithTryFinallyInsideAnother()
   {
      int i = 0;
      while (true) {
         try {
            try {
               i = 1;
            }
            finally { // the first finally clause
               i = 2;
            }
            i = 3;
            return i; // this never completes, because of the continue
         }
         finally { // the second finally clause
            if (i == 3) {
               continue; // this continue overrides the return statement
            }
         }
      }
   }

   void finallyBlockContainingIfWithBodyInSameLine()
   {
      boolean b = false;

      try {
         toString();
      }
      finally {
         if (b) toString();
      }
   }
}