package integrationTests.loops;

import java.util.List;

public class ForStatements
{
   void forInSeparateLines()
   {
      for (int i = 0; i < 2; i++) {
         System.gc();
      }
   }

   void forInSingleLine(int i) {
      for (; i < 2; i++) System.gc();
   }

   int forWithBreakAndContinue(int i)
   {
      for (; i < 2; i++) {
         if (i == 1) {
            i = 3;
            continue;
         }

         if (i < 0) {
            break;
         }
      }

      return i;
   }

   void nestedFor(List<Integer> list)
   {
      for (Integer i : list) {
         for (int j = 0; j < i; j++) {
            i++;
         }
      }
   }
}