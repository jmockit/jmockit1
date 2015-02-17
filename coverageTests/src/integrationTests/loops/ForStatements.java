package integrationTests.loops;

import java.util.*;

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

   int forEachArrayElement(int... values)
   {
      int sum = 0;

      for (int value : values) {
         sum += value;
      }

      return sum;
   }

   String forEachCollectionElement(List<String> values)
   {
      StringBuilder result = new StringBuilder();

      for (String value : values) {
         result.append(value);
      }

      return result.toString();
   }

   void forUsingIterator(Collection<? extends Number> values)
   {
      for (Iterator<? extends Number> itr = values.iterator(); itr.hasNext(); ) {
         Number value = itr.next();

         if (value.intValue() == 0) {
            itr.remove();
         }
      }
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