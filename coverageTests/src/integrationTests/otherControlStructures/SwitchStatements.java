package integrationTests.otherControlStructures;

public final class SwitchStatements
{
   void switchStatementWithSparseCasesAndDefault(char c)
   {
      switch (c) {
         case 'A':
            System.gc();
            break;
         case 'f':
         {
            boolean b = true;
            System.gc();
            System.runFinalization();
            break;
         }
         case '\0': return;
         default:
            throw new IllegalArgumentException();
      }
   }

   void anotherSwitchStatementWithSparseCasesAndDefault(char c)
   {
      switch (c) {
         case 'B':
            System.gc();
            break;
         default:
            System.runFinalization();
      }
   }

   void switchStatementWithCompactCasesAndDefault(int i)
   {
      switch (i) {
         case 1:
            System.gc();
            break;
         case 2:
         {
            boolean b = true;
            System.gc();
            System.runFinalization();
            break;
         }
         case 4: return;
         default:
            throw new IllegalArgumentException();
      }
   }

   void anotherSwitchStatementWithCompactCasesAndDefault(int i)
   {
      switch (i) {
         case 1: System.gc(); break;
         default: System.runFinalization();
      }
   }

   void switchStatementWithSparseCasesAndNoDefault(char c)
   {
      switch (c) {
         case 'A':
            System.gc();
            break;
         case 'f':
            System.runFinalization();
            break;
      }
   }

   boolean switchStatementWithCompactCasesAndNoDefault(int i)
   {
      boolean b = true;

      switch (i) {
         case 1: System.gc(); return b;
         case 2: System.runFinalization(); return b;
         case 4: b = false;
      }

      return b;
   }

   char switchStatementWithExitInAllCases(int i)
   {
      switch (i) {
         case 1: return 'a';
         case 2: return 'b';
         default: throw new IllegalArgumentException();
      }
   }

   int switchOnString(String s, boolean b)
   {
      switch(s) {
         case "A": return 1;
         default:
            return b ? 2 : 3;
      }
   }
}