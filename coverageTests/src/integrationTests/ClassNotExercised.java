package integrationTests;

public final class ClassNotExercised
{
   public boolean doSomething(int i, String s)
   {
      if (i > 0) {
         System.out.println(s);
      }

      return s.length() > 0;
   }
}
