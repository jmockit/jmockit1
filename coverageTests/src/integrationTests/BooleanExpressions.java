package integrationTests;

public final class BooleanExpressions
{
   public boolean eval1(boolean x, boolean y, int z)
   {
      return x && (y || z > 0) ? true : false;
   }

   public boolean eval2(boolean x, boolean y, int z)
   {
      return x && (y || z > 0);
   }

   public boolean eval3(boolean x, boolean y, boolean z)
   {
      return x && (y || z); // LOAD 1 IFEQ L1, LOAD 2 IFNE L2, LOAD 3 IFEQ L1, [L2 1 GOTO L3], [L1 0 L3 RETURN]
   }

   public boolean eval4(boolean x, boolean y, boolean z)
   {
      return x && (!y || z);
   }

   public boolean eval5(boolean a, boolean b, boolean c)
   {
      if (a) return true;
      if (b || c) return false;

      return !c;
   }

   static boolean isSameTypeIgnoringAutoBoxing(Class<?> firstType, Class<?> secondType)
   {
      return
         firstType == secondType ||
         firstType.isPrimitive() && isWrapperOfPrimitiveType(firstType, secondType) ||
         secondType.isPrimitive() && isWrapperOfPrimitiveType(secondType, firstType);
   }

   static boolean isWrapperOfPrimitiveType(Class<?> primitiveType, Class<?> otherType)
   {
      return
         primitiveType == int.class && otherType == Integer.class ||
         primitiveType == long.class && otherType == Long.class ||
         primitiveType == double.class && otherType == Double.class ||
         primitiveType == float.class && otherType == Float.class ||
         primitiveType == boolean.class && otherType == Boolean.class;
   }

   public boolean simplyReturnsInput(boolean b)
   {
      return b;
   }

   public boolean returnsNegatedInput(boolean b)
   {
      return !b; // LOAD 1 IFNE L1, 1 GOTO L2, L1 0 L2 RETURN
   }

   public boolean returnsTrivialResultFromInputAfterIfElse(boolean b, int i)
   {
      String s;

      if (b) {
         s = "one";
      }
      else {
         s = "two";
      }

      return i != 0 ? true : false; // LOAD 2 IFEQ L1, 1 GOTO L2, L1 0 L2 RETURN
   }

   public boolean returnsResultPreviouslyComputedFromInput(boolean b, int i)
   {
      String s = b ? "a" : "b";
      boolean res;

      if (i != 0) res = true;
      else {
         res = false;
         System.out.checkError();
      }

      return res;
   }

   public boolean methodWithTooManyConditionsForPathAnalysis(int i, int j, boolean b)
   {
      if (i > 0 && j < 5 || b && i > 1 || !b && j > 5 || (i <= 3 || j >= 4) && b) {
         return i + j == 3 ? b : !b;
      }
      else if (i < 0 || j < 0) {
         return i < j;
      }

      return b;
   }

   public boolean returnsNegatedInputFromLocalVariable(boolean b)
   {
      boolean var = !b; return var; // LOAD 1 IFNE L1, [1 GOTO L2], [L1 0], L2 STORE 2, L3 LOAD 2 RETURN
   }
}
